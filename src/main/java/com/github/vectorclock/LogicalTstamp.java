package com.github.vectorclock;

/**
 * Logical timestamp representation.
 * 
 * @author gaurav
 */
public final class LogicalTstamp implements Comparable<LogicalTstamp> {
  private final long timestamp;

  public LogicalTstamp() {
    this(0L);
  }

  // typically used while merging VectorClocks
  static LogicalTstamp curate(final long timestamp) {
    if (timestamp < 0) {
      throw new IllegalArgumentException("Only positive timestamp values are allowed");
    }
    return new LogicalTstamp(timestamp);
  }

  private LogicalTstamp(final long timestamp) {
    if (timestamp < 0) {
      throw new IllegalArgumentException("Only positive timestamp values are allowed");
    }
    this.timestamp = timestamp;
  }

  LogicalTstamp tick() {
    long nextTstamp = timestamp;
    if (nextTstamp == Long.MAX_VALUE) {
      nextTstamp = 0L;
    } else {
      ++nextTstamp;
    }
    return new LogicalTstamp(nextTstamp);
  }

  boolean before(final LogicalTstamp other) {
    return this.compareTo(other) < 0;
  }

  boolean after(final LogicalTstamp other) {
    return this.compareTo(other) > 0;
  }

  long currentValue() {
    return timestamp;
  }

  @Override
  public LogicalTstamp clone() {
    return new LogicalTstamp(timestamp);
  }

  @Override
  public int compareTo(final LogicalTstamp other) {
    return Long.compare(this.timestamp, other.timestamp);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Tstamp[val:").append(timestamp).append("]");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
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
    if (!(obj instanceof LogicalTstamp)) {
      return false;
    }
    LogicalTstamp other = (LogicalTstamp) obj;
    if (timestamp != other.timestamp) {
      return false;
    }
    return true;
  }

}
