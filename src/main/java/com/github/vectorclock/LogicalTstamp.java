package com.github.vectorclock;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Logical timestamp representation.
 * 
 * @author gaurav
 */
public final class LogicalTstamp implements Comparable<LogicalTstamp> {
  private final AtomicLong timestamp = new AtomicLong();

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
    this.timestamp.set(timestamp);
  }

  LogicalTstamp tick() {
    if (timestamp.get() == Long.MAX_VALUE) {
      timestamp.set(0L);
    } else {
      timestamp.incrementAndGet();
    }
    return new LogicalTstamp(timestamp.get());
  }

  boolean before(final LogicalTstamp other) {
    return this.compareTo(other) < 0;
  }

  boolean after(final LogicalTstamp other) {
    return this.compareTo(other) > 0;
  }

  long currentValue() {
    return timestamp.get();
  }

  @Override
  public LogicalTstamp clone() {
    return new LogicalTstamp(timestamp.get());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long longTstamp = timestamp.get();
    result = prime * result + (int) (longTstamp ^ (longTstamp >>> 32));
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
    if (timestamp == null) {
      if (other.timestamp != null) {
        return false;
      }
    } else if (timestamp.get() != other.timestamp.get()) {
      return false;
    }
    return true;
  }

  @Override
  public int compareTo(final LogicalTstamp other) {
    return Long.compare(this.timestamp.get(), other.timestamp.get());
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LogicalTstamp[value:").append(timestamp).append("]");
    return builder.toString();
  }

}
