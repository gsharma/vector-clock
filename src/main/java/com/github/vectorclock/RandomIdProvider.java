package com.github.vectorclock;

import java.util.UUID;

/**
 * A random id provider relying on random Type 4 UUIDs.
 * 
 * @author gaurav
 */
public class RandomIdProvider implements IdProvider {

  @Override
  public String id() {
    return UUID.randomUUID().toString();
  }

}
