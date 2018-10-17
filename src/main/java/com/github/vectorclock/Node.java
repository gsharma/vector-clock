package com.github.vectorclock;

/**
 * Basic immutable node skeleton.
 * 
 * @author gaurav
 */
public class Node {
  private final String id;

  public Node(final String id) {
    this.id = id;
  }

  public Node(final IdProvider idProvider) {
    id = idProvider.id();
  }

  // return the unique node id
  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof Node)) {
      return false;
    }
    Node other = (Node) obj;
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Node[id:").append(id).append("]");
    return builder.toString();
  }

}
