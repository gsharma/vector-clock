package com.github.vectorclock;

/**
 * This immutable object reflects a transition in VectorClock from its previous to next
 * LogicalTstamp resulting from a node Event handled by this node.
 * 
 * @author gaurav
 */
public final class VectorClockTransition {
  private final Event nodeEvent;
  private final LogicalTstamp previousTstamp;
  private final LogicalTstamp currentTstamp;

  public VectorClockTransition(final Event nodeEvent, final LogicalTstamp previousTstamp,
      final LogicalTstamp currentTstamp) {
    this.nodeEvent = nodeEvent;
    this.previousTstamp = previousTstamp;
    this.currentTstamp = currentTstamp;
  }

  public Event getNodeEvent() {
    return nodeEvent;
  }

  public LogicalTstamp getPreviousTstamp() {
    return previousTstamp;
  }

  public LogicalTstamp getCurrentTstamp() {
    return currentTstamp;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("VectorClockTransition[").append(nodeEvent).append(", previous ")
        .append(previousTstamp).append(", current").append(currentTstamp).append("]");
    return builder.toString();
  }

}
