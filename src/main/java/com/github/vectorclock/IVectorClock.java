package com.github.vectorclock;

import java.util.Map;

/**
 * Models a vector clock as a dynamic array of node:logicalTimestamp for the node.
 * 
 * @author gaurav
 */
public interface IVectorClock {

  void initNode(Node node);

  boolean removeNode(Node node);

  // Not a perfect snapshot and there isn't a need for one either
  Map<Node, LogicalTstamp> snapshot();

  /**
   * Record a significant event that materially changes the state and/or data of a Node. This
   * results in a change to the VectorClock.
   * 
   * We want to serialize recording of this event on the Node and use pessimistic locking for
   * simplicity and correctness. Since correctness is non-negotiable, instead of reducing the
   * critical section and other foo-bar, a more worthwhile goal is to speed up this thread's
   * execution.
   */
  VectorClockTransition recordEvent(Event event);

  VectorClock deepCopy();

}
