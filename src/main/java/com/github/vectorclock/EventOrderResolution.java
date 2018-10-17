package com.github.vectorclock;

/**
 * Models the ordering of Events as represented by a pair of VectorClock snapshots.
 * 
 * @author gaurav
 */
public enum EventOrderResolution {
  CONCURRENT, HAPPENS_BEFORE, HAPPENS_AFTER, IDENTICAL;
}
