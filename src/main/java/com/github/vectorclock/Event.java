package com.github.vectorclock;

import java.util.Optional;

/**
 * Model a simple event and the associated node that needs to record it.
 * 
 * Note that this class does not explicitly model specializations of events:<br/>
 * a) node-local events<br/>
 * b) event sent from node1->node2<br/>
 * c) event received by node1<-node2<br/>
 * 
 * All the above events that have ramifications on any state change are implicitly modeled at this
 * top-level object. This does not preclude creation of child events like NodeSendMessage,
 * NodeReceiveMessage, NodeStateChange, etc.
 * 
 * @author gaurav
 */
public class Event {
  private final Node impactedNode;
  private final EventType type;
  // only applicable for events of type RECEIVE
  private final VectorClock senderClock;

  public Event(final EventType type, final Node impactedNode,
      final Optional<VectorClock> senderClock) {
    this.type = type;
    this.impactedNode = impactedNode;
    if (type == EventType.RECEIVE) {
      if (!senderClock.isPresent()) {
        throw new IllegalArgumentException(
            "RECEIVE events should be accompanied with their sender's vector clock");
      }
      this.senderClock = senderClock.get();
    } else {
      this.senderClock = null;
    }
  }

  public EventType getEventType() {
    return type;
  }

  public Node getImpactedNode() {
    return impactedNode;
  }

  public VectorClock getSenderClock() {
    return senderClock;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Event[type:").append(type).append(", ").append(impactedNode).append("]");
    return builder.toString();
  }

  public enum EventType {
    LOCAL, SEND, RECEIVE;
  }

}
