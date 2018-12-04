package com.github.vectorclock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.github.vectorclock.Event.EventType;

/**
 * Maintain sanity and correctness of VectorClock implementation.
 * 
 * @author gaurav
 */
public class VectorClockTest {
  private static final Logger logger = LogManager.getLogger(VectorClockTest.class.getSimpleName());

  @Test
  public void testTstampCausality() {
    LogicalTstamp tstampInit = new LogicalTstamp();
    LogicalTstamp tstampNext = tstampInit.tick();
    LogicalTstamp tstampNextNext = tstampNext.tick();
    logger.info(String.format("tstampInit:%s, tstampNext:%s, tstampNextNext:%s", tstampInit,
        tstampNext, tstampNextNext));

    assertTrue(tstampInit.before(tstampNext));
    assertTrue(tstampNext.before(tstampNextNext));
    assertTrue(tstampInit.before(tstampNextNext));

    assertTrue(tstampNext.after(tstampInit));
    assertTrue(tstampNextNext.after(tstampNext));
    assertTrue(tstampNextNext.after(tstampInit));

    LogicalTstamp tstampInitClone = tstampInit.clone();
    assertEquals(tstampInit, tstampInitClone);

    LogicalTstamp curatedFromTstampInit = LogicalTstamp.curate(tstampInit.currentValue());
    assertEquals(tstampInit, curatedFromTstampInit);
  }

  @Test
  public void testVectorClockOrdering() {
    // Let's model a system of 3 inter-communicating nodes
    final Node nodeOne = new Node("1");
    final Node nodeTwo = new Node("2");
    final Node nodeThree = new Node("3");

    // Setup vector clocks for every node
    final IVectorClock nodeOneClock = new VectorClock();
    nodeOneClock.initNode(nodeOne);
    nodeOneClock.initNode(nodeTwo);
    nodeOneClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeOneClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final IVectorClock nodeTwoClock = new VectorClock();
    nodeTwoClock.initNode(nodeOne);
    nodeTwoClock.initNode(nodeTwo);
    nodeTwoClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeTwoClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final IVectorClock nodeThreeClock = new VectorClock();
    nodeThreeClock.initNode(nodeOne);
    nodeThreeClock.initNode(nodeTwo);
    nodeThreeClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeThreeClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    assertEquals(EventOrdering.IDENTICAL, VectorClock.compareClocks(nodeOneClock, nodeTwoClock));
    assertEquals(EventOrdering.IDENTICAL, VectorClock.compareClocks(nodeOneClock, nodeThreeClock));
    assertEquals(EventOrdering.IDENTICAL, VectorClock.compareClocks(nodeTwoClock, nodeThreeClock));

    // 1a. local event on nodeOne: 0,0,0 -> 1,0,0
    nodeOneClock.recordEvent(new Event(EventType.LOCAL, nodeOne, Optional.empty()));

    // 1b. validate ordering
    assertEquals(EventOrdering.HAPPENS_AFTER,
        VectorClock.compareClocks(nodeOneClock, nodeTwoClock));
    assertEquals(EventOrdering.HAPPENS_AFTER,
        VectorClock.compareClocks(nodeOneClock, nodeThreeClock));
    assertEquals(EventOrdering.HAPPENS_BEFORE,
        VectorClock.compareClocks(nodeTwoClock, nodeOneClock));
    assertEquals(EventOrdering.HAPPENS_BEFORE,
        VectorClock.compareClocks(nodeThreeClock, nodeOneClock));
    assertEquals(EventOrdering.IDENTICAL, VectorClock.compareClocks(nodeTwoClock, nodeThreeClock));

    // 2a. concurrent events:: nodeTwo: 0,0,0->0,1,0, nodeThree: 0,0,0->0,0,1
    nodeTwoClock.recordEvent(new Event(EventType.SEND, nodeTwo, Optional.empty()));
    nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));

    // 2b. validate ordering - at this stage, all clocks show concurrent events
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeOneClock, nodeTwoClock));
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeOneClock, nodeThreeClock));
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeTwoClock, nodeThreeClock));
  }

  @Test
  public void testVectorClocks() {
    // Let's model a system of 3 inter-communicating nodes
    final Node nodeOne = new Node("a");
    final Node nodeTwo = new Node("b");
    final Node nodeThree = new Node("c");

    // Setup vector clocks for every node
    final IVectorClock nodeOneClock = new VectorClock();
    nodeOneClock.initNode(nodeOne);
    nodeOneClock.initNode(nodeTwo);
    nodeOneClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeOneClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final IVectorClock nodeTwoClock = new VectorClock();
    nodeTwoClock.initNode(nodeOne);
    nodeTwoClock.initNode(nodeTwo);
    nodeTwoClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeTwoClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final IVectorClock nodeThreeClock = new VectorClock();
    nodeThreeClock.initNode(nodeOne);
    nodeThreeClock.initNode(nodeTwo);
    nodeThreeClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeThreeClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    // Let's run a sequence of 6 events & figure sanity and ordering afforded to us by the vector
    // clocks. We will prove that the sequence of numbering below does not correlate with wall or
    // system clock timestamps and will test the actual ordering observed in this system via
    // validating our expectations from the EventOrderResolution

    // 1. 0,0,0 -> 1,0,0 :: nodeOne local event
    nodeOneClock.recordEvent(new Event(EventType.LOCAL, nodeOne, Optional.empty()));
    assertEquals(1L, nodeOneClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

    // 2. 0,0,0 -> 0,1,0 :: nodeTwo local event
    nodeTwoClock.recordEvent(new Event(EventType.LOCAL, nodeTwo, Optional.empty()));
    assertEquals(0L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
    assertEquals(1L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeTwoClock.snapshot().get(nodeThree).currentValue());

    // 3. 0,0,0 -> 0,0,1 :: nodeThree->nodeTwo send event
    nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(1L, nodeThreeClock.snapshot().get(nodeThree).currentValue());

    // 4. nodeTwo<-nodeThree receive event
    // sender clock :: 0,0,1
    // receiver clock :: 0,1,0
    // boom, conflict detected!
    VectorClock senderClock = nodeThreeClock.deepCopy();
    VectorClockTransition transition =
        nodeTwoClock.recordEvent(new Event(EventType.RECEIVE, nodeTwo, Optional.of(senderClock)));
    assertTrue(transition.isConcurrentEventConflictDetected());
    logger.info(transition);
    assertEquals(0L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
    assertEquals(1L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeTwoClock.snapshot().get(nodeThree).currentValue());

    // Observed Concurrent events will stall further processing on nodeTwo until conflicts are
    // resolved while nodeOne and nodeThree and continue business as usual

    // 5. 1,0,0 -> 2,0,0 :: nodeOne->nodeThree send event
    nodeOneClock.recordEvent(new Event(EventType.SEND, nodeOne, Optional.empty()));
    assertEquals(2L, nodeOneClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

    // 6. 0,0,1 -> 0,0,2 :: nodeThree->nodeOne send event
    nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(2L, nodeThreeClock.snapshot().get(nodeThree).currentValue());

    // 2,0,3 (nodeThree) vs 3,0,0 (nodeOne)
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeThreeClock, nodeOneClock));

    // 2,0,3 (nodeThree) vs 0,1,0 (nodeTwo)
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeThreeClock, nodeTwoClock));

    // 3,0,0 (nodeOne) vs 0,1,0 (nodeTwo)
    assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeOneClock, nodeTwoClock));
  }

  @Test
  public void testTstampTickSafety() throws Exception {
    LogicalTstamp init = new LogicalTstamp();
    assertEquals(0L, init.currentValue());

    // test that concurrent tstamp ticking doesn't ever screw with correctness - every thread should
    // simply gets its local copy of tstamp to muck with and should never trample on anyone else's
    // tstamp
    int workerCount = 100;
    Thread[] workers = new Thread[workerCount];
    for (int iter = 0; iter < workerCount; iter++) {
      workers[iter] = new Thread() {
        public void run() {
          final LogicalTstamp next = init.tick();
          assertNotNull(next);
          assertEquals(1L, next.currentValue());
        }
      };
    }
    for (Thread worker : workers) {
      worker.start();
    }
    for (Thread worker : workers) {
      worker.join();
    }
  }

}
