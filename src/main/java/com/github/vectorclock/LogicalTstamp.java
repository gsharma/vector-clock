package com.github.vectorclock;

/**
 * An immutable logical timestamp representation.
 * 
 * Note that logical timestamps can be generated from their long timestamp values but once created,
 * they are immutable. Along the same lines, calling {@link #tick()} does not modify the existing
 * logical timestamp but generates a new immutable version.
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

  // due to the fact that ticking generates another immutable tstamp by simply reading the
  // timestamp, there's no need to lock here
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
