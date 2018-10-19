package com.github.vectorclock;

/**
 * This immutable object reflects a transition in VectorClock from its previous to next
 * LogicalTstamp resulting from a node Event handled by this node.
 * 
 * @author gaurav
 */
public final class VectorClockTransition {
  private final Event nodeEvent;
  private final VectorClock receiverVectorClock;
  private final boolean concurrentEventConflictDetected;

  public VectorClockTransition(final Event nodeEvent, final VectorClock receiverVectorClock,
      final boolean concurrentEventConflictDetected) {
    this.nodeEvent = nodeEvent;
    this.receiverVectorClock = receiverVectorClock;
    this.concurrentEventConflictDetected = concurrentEventConflictDetected;
  }

  public Event getNodeEvent() {
    return nodeEvent;
  }

  public VectorClock getReceiverVectorClock() {
    return receiverVectorClock;
  }

  public boolean isConcurrentEventConflictDetected() {
    return concurrentEventConflictDetected;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VectorClockTransition [").append(nodeEvent).append(", receiver")
        .append(receiverVectorClock).append(", concurrentEventConflictDetected:")
        .append(concurrentEventConflictDetected).append("]");
    return builder.toString();
  }

}
