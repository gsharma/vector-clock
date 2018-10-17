package com.github.vectorclock;

/**
 * Basic immutable node skeleton.
 * 
 * @author gaurav
 */
public class Node {
  private final String key = keyProvider().key();

  // return the unique node id
  public String getKey() {
    return key;
  }

  public KeyProvider keyProvider() {
    return new RandomKeyProvider();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((key == null) ? 0 : key.hashCode());
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
    if (key == null) {
      if (other.key != null) {
        return false;
      }
    } else if (!key.equals(other.key)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Node[key:").append(key).append("]");
    return builder.toString();
  }

}
