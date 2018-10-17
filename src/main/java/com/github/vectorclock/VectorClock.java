package com.github.vectorclock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Models a vector clock as a dynamic array of node:logicalTimestamp for the node.
 * 
 * @author gaurav
 */
public final class VectorClock {
  private static final Logger logger = LogManager.getLogger(VectorClock.class.getSimpleName());

  // map from nodeId:logicalTstamp for that node
  // the reason to not do this as a simple 2-d static array of ints is to allow for dynamism which
  // is inherent in a system as nodes are allowed to come and go at will
  private final ConcurrentMap<Node, LogicalTstamp> tstampVector = new ConcurrentHashMap<>();

  public void initNode(final Node node) {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    tstampVector.putIfAbsent(node, new LogicalTstamp());
  }

  public void initNodeTstampTuple(final Node node, final LogicalTstamp tstamp) {
    if (node == null || tstamp == null) {
      throw new IllegalArgumentException("node and logical timestamp cannot be null");
    }
    tstampVector.putIfAbsent(node, tstamp);
  }

  public boolean removeNode(final Node node) {
    return tstampVector.remove(node) != null ? true : false;
  }

  public Map<Node, LogicalTstamp> snapshot() {
    final Map<Node, LogicalTstamp> snapshot = new HashMap<>();
    for (final Node node : tstampVector.keySet()) {
      snapshot.put(node, tstampVector.get(node).clone());
    }
    final Map<Node, LogicalTstamp> immutableSnapshot = Collections.unmodifiableMap(snapshot);
    logger.info(immutableSnapshot);
    return immutableSnapshot;
  }

  @Override
  public VectorClock clone() {
    final Map<Node, LogicalTstamp> snapshot = snapshot();
    final VectorClock cloned = new VectorClock();
    for (final Map.Entry<Node, LogicalTstamp> entry : snapshot.entrySet()) {
      cloned.initNodeTstampTuple(entry.getKey(), entry.getValue());
    }
    return cloned;
  }

  /**
   * Record a significant event that materially changes the state and/or data of a Node. This
   * results in a change to the VectorClock.
   */
  public void recordEvent(final Event event) {
    final Node node = event.getImpactedNode();
    final LogicalTstamp current = tstampVector.get(node);
    switch (event.getEventType()) {
      case LOCAL:
      case SEND:
        final LogicalTstamp next = current.tick();
        tstampVector.put(node, next);
        break;
      case RECEIVE:
        // first tick current tstamp
        tstampVector.put(node, current.tick());
        // this is expected to be typically a clone of the original clock
        final VectorClock receivedClock = event.getSenderClock();
        // now merge in received vector clock
        mergeClock(receivedClock);
        break;
    }
  }

  private void mergeClock(final VectorClock clock) {
    for (final Map.Entry<Node, LogicalTstamp> entry : clock.tstampVector.entrySet()) {
      final LogicalTstamp thisTstamp = tstampVector.get(entry.getKey());
      if (thisTstamp != null) {
        final LogicalTstamp receivedTstamp = entry.getValue();
        if (thisTstamp.before(receivedTstamp)) {
          tstampVector.put(entry.getKey(), LogicalTstamp.curate(receivedTstamp.currentValue()));
        }
      }
    }
  }

  public static EventOrderResolution determineOrdering(final VectorClock clockOne,
      final VectorClock clockTwo) {
    return null;
  }

}
