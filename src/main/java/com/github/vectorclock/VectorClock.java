package com.github.vectorclock;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Models a vector clock as a dynamic array of node:logicalTimestamp for the node.
 * 
 * The class itself is not completely thread-safe but the most important {@link #recordEvent(Event)}
 * implementation uses pessimistic locking to ensure correctness.
 * 
 * @author gaurav
 */
public final class VectorClock implements IVectorClock {
  private static final Logger logger = LogManager.getLogger(VectorClock.class.getSimpleName());

  private final ReentrantReadWriteLock superLock = new ReentrantReadWriteLock(true);
  private final WriteLock writeLock = superLock.writeLock();

  // map from nodeId:logicalTstamp for that node
  // the reason to not do this as a simple 2-d static array of ints is to allow for dynamism which
  // is inherent in a system as nodes are allowed to come and go at will
  private final ConcurrentMap<Node, LogicalTstamp> tstampVector = new ConcurrentHashMap<>();

  /*
   * (non-Javadoc)
   * 
   * @see com.github.vectorclock.IVectorClock#initNode(com.github.vectorclock.Node)
   */
  @Override
  public void initNode(final Node node) {
    if (node == null) {
      throw new IllegalArgumentException("node cannot be null");
    }
    tstampVector.putIfAbsent(node, new LogicalTstamp());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.vectorclock.IVectorClock#removeNode(com.github.vectorclock.Node)
   */
  @Override
  public boolean removeNode(final Node node) {
    return tstampVector.remove(node) != null ? true : false;
  }

  // Not a perfect snapshot and there isn't a need for one either
  /*
   * (non-Javadoc)
   * 
   * @see com.github.vectorclock.IVectorClock#snapshot()
   */
  @Override
  public Map<Node, LogicalTstamp> snapshot() {
    final Map<Node, LogicalTstamp> snapshot = new TreeMap<>(new Comparator<Node>() {
      public int compare(Node nodeOne, Node nodeTwo) {
        return nodeOne.getId().compareTo(nodeTwo.getId());
      }
    });
    for (final Node node : tstampVector.keySet()) {
      snapshot.put(node, tstampVector.get(node).clone());
    }
    logger.info(snapshot);
    return snapshot;
  }

  @Override
  public VectorClock deepCopy() {
    final Map<Node, LogicalTstamp> snapshot = snapshot();
    final VectorClock cloned = new VectorClock();
    for (final Map.Entry<Node, LogicalTstamp> entry : snapshot.entrySet()) {
      cloned.initNodeTstampTuple(entry.getKey(), entry.getValue());
    }
    return cloned;
  }

  private void initNodeTstampTuple(final Node node, final LogicalTstamp tstamp) {
    if (node == null || tstamp == null) {
      throw new IllegalArgumentException("node and logical timestamp cannot be null");
    }
    tstampVector.putIfAbsent(node, tstamp);
  }

  /**
   * Compare two vector clocks and return:<br/>
   * 
   * 1. IDENTICAL if the count and values all match<br/>
   * 2. HAPPENS_BEFORE if all tstamps of clockOne happen before those of clockTwo<br/>
   * 3. HAPPENS_AFTER if all tstamps of clockOne happen after those of clockTwo<br/>
   * 4. CONCURRENT if some tstamps of clockOne and clockTwo are reverse ordered<br/>
   * 5. NOT_COMPARABLE otherwise
   */
  public static EventOrdering compareClocks(final IVectorClock clockOne,
      final IVectorClock clockTwo) {
    EventOrdering ordering = null;
    if (clockOne == null || clockTwo == null) {
      throw new IllegalArgumentException("Cannot compare null vector clocks");
    }

    final Map<Node, LogicalTstamp> clockOneSnapshot = clockOne.snapshot();
    final Map<Node, LogicalTstamp> clockTwoSnapshot = clockTwo.snapshot();

    Set<Node> clockOneNodes = clockOneSnapshot.keySet();
    Set<Node> clockTwoNodes = clockTwoSnapshot.keySet();

    // sizes differ, not comparable
    if (clockOneNodes.size() != clockTwoNodes.size()) {
      return EventOrdering.NOT_COMPARABLE;
    }

    // sizes are same but some nodes differ
    clockOneNodes.retainAll(clockTwoNodes);
    if (clockOneNodes.size() != clockTwoNodes.size()) {
      return EventOrdering.NOT_COMPARABLE;
    }

    // got here, so nodes and sizes are identical - let's iterate and compare each tstamp
    boolean oneAfterTwo = false;
    boolean twoAfterOne = false;
    boolean concurrent = false;
    for (Iterator<LogicalTstamp> iterOne = clockOneSnapshot.values().iterator(), iterTwo =
        clockTwoSnapshot.values().iterator(); iterOne.hasNext() && iterTwo.hasNext();) {
      final LogicalTstamp tstampOne = iterOne.next();
      final LogicalTstamp tstampTwo = iterTwo.next();
      if (tstampOne.after(tstampTwo)) {
        oneAfterTwo = true;
      }
      if (tstampTwo.after(tstampOne)) {
        twoAfterOne = true;
      }
      if (oneAfterTwo && twoAfterOne) {
        ordering = EventOrdering.CONCURRENT;
        concurrent = true;
        break;
      }
    }

    if (oneAfterTwo && !twoAfterOne) {
      ordering = EventOrdering.HAPPENS_AFTER;
    } else if (!oneAfterTwo && twoAfterOne) {
      ordering = EventOrdering.HAPPENS_BEFORE;
    } else if (!oneAfterTwo && !twoAfterOne && !concurrent) {
      ordering = EventOrdering.IDENTICAL;
    } // else ordering is CONCURRENT

    logger.info(String.format("%s and %s are %s", clockOne, clockTwo, ordering));
    return ordering;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.github.vectorclock.IVectorClock#recordEvent(com.github.vectorclock.Event)
   */
  @Override
  public VectorClockTransition recordEvent(final Event event) {
    VectorClockTransition transition = null;
    if (writeLock.tryLock()) {
      try {
        final Node node = event.getImpactedNode();
        final LogicalTstamp current = tstampVector.get(node);
        switch (event.getEventType()) {
          case LOCAL:
          case SEND:
            final LogicalTstamp next = current.tick();
            tstampVector.put(node, next);
            transition = new VectorClockTransition(event, null, false);
            break;
          case RECEIVE:
            // this is expected to be typically a clone of the original clock
            final VectorClock receivedClock = event.getSenderClock();

            // clone the current clock
            // VectorClock currentClock = clone();

            // now check if the event ordering indicates concurrent events
            final EventOrdering eventOrdering = VectorClock.compareClocks(this, receivedClock);

            if (eventOrdering == EventOrdering.CONCURRENT) {
              // do not accept events that result in conflicting version updates
              transition = new VectorClockTransition(event, this, true);
            } else {
              // first tick current tstamp
              final LogicalTstamp nextTstamp = current.tick();
              tstampVector.put(node, nextTstamp);

              // now merge in received vector clock
              mergeClock(receivedClock);

              transition = new VectorClockTransition(event, this, false);
            }
            break;
        }
      } finally {
        writeLock.unlock();
      }
    }
    return transition;
  }

  // Merge the passed clock into this clock
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

  /*
   * (non-Javadoc)
   * 
   * @see com.github.vectorclock.IVectorClock#toString()
   */
  @Override
  public String toString() {
    return "VectorClock:[" + snapshot().toString() + "]";
  }

}
