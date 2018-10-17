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
  public void testVectorClocks() {
    // Let's model a system of 3 inter-communicating nodes
    final Node nodeOne = new Node("a");
    final Node nodeTwo = new Node("b");
    final Node nodeThree = new Node("c");

    // Setup vector clocks for every node
    final VectorClock nodeOneClock = new VectorClock();
    nodeOneClock.initNode(nodeOne);
    nodeOneClock.initNode(nodeTwo);
    nodeOneClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeOneClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final VectorClock nodeTwoClock = new VectorClock();
    nodeTwoClock.initNode(nodeOne);
    nodeTwoClock.initNode(nodeTwo);
    nodeTwoClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeTwoClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    final VectorClock nodeThreeClock = new VectorClock();
    nodeThreeClock.initNode(nodeOne);
    nodeThreeClock.initNode(nodeTwo);
    nodeThreeClock.initNode(nodeThree);

    // check that every tstamp in this clock initialized to zero
    for (final LogicalTstamp logicalTstamp : nodeThreeClock.snapshot().values()) {
      assertEquals(0L, logicalTstamp.currentValue());
    }

    // Let's run a sequence of 16 events & figure sanity and ordering afforded to us by the vector
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

    // 4. 0,1,0 -> 0,2,1 :: nodeTwo<-nodeThree receive event
    // this will result in merge, push down the vector clock of sender
    VectorClock senderClock = nodeThreeClock.clone();
    nodeTwoClock.recordEvent(new Event(EventType.RECEIVE, nodeTwo, Optional.of(senderClock)));
    assertEquals(0L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
    assertEquals(2L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(1L, nodeTwoClock.snapshot().get(nodeThree).currentValue());

    // 5. 1,0,0 -> 2,0,0 :: nodeOne->nodeThree send event
    nodeOneClock.recordEvent(new Event(EventType.SEND, nodeOne, Optional.empty()));
    assertEquals(2L, nodeOneClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

    // #6 and #7 are CONCURRENT events
    // 6. 0,0,1 -> 0,0,2 :: nodeThree->nodeOne send event
    nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(2L, nodeThreeClock.snapshot().get(nodeThree).currentValue());

    // #6 & #7 are CONCURRENT events
    // 7. 2,0,0 -> 2,0,3 :: nodeThree<-nodeOne receive event
    senderClock = nodeOneClock.clone();
    nodeThreeClock.recordEvent(new Event(EventType.RECEIVE, nodeThree, Optional.of(senderClock)));
    assertEquals(2L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(3L, nodeThreeClock.snapshot().get(nodeThree).currentValue());

    // 8. 2,0,0 -> 3,0,0 :: nodeOne->nodeTwo send event
    nodeOneClock.recordEvent(new Event(EventType.SEND, nodeOne, Optional.empty()));
    assertEquals(3L, nodeOneClock.snapshot().get(nodeOne).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

    // 9. 0,2,1 -> 3,3,1 :: nodeTwo<-nodeOne receive event
    senderClock = nodeOneClock.clone();
    nodeTwoClock.recordEvent(new Event(EventType.RECEIVE, nodeTwo, Optional.of(senderClock)));
    assertEquals(3L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
    assertEquals(3L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
    assertEquals(1L, nodeTwoClock.snapshot().get(nodeThree).currentValue());

    // TODO: Due to #6 and #7 being CONCURRENT events, expect future events to be first resolved
    /*
     * // 10. 3,0,0 -> 4,0,2 :: nodeOne<-nodeThree receive event senderClock =
     * nodeThreeClock.clone(); nodeOneClock.recordEvent(new Event(EventType.RECEIVE, nodeOne,
     * Optional.of(senderClock))); assertEquals(4L,
     * nodeOneClock.snapshot().get(nodeOne).currentValue()); assertEquals(0L,
     * nodeOneClock.snapshot().get(nodeTwo).currentValue()); assertEquals(2L,
     * nodeOneClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 11. 3,3,1 -> 3,4,1 :: nodeTwo->nodeOne send event nodeTwoClock.recordEvent(new
     * Event(EventType.SEND, nodeTwo, Optional.empty())); assertEquals(3L,
     * nodeTwoClock.snapshot().get(nodeOne).currentValue()); assertEquals(4L,
     * nodeTwoClock.snapshot().get(nodeTwo).currentValue()); assertEquals(1L,
     * nodeTwoClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 12. 4,0,2 -> 5,4,2 :: nodeOne<-nodeTwo receive event senderClock = nodeTwoClock.clone();
     * nodeOneClock.recordEvent(new Event(EventType.RECEIVE, nodeOne, Optional.of(senderClock)));
     * assertEquals(5L, nodeOneClock.snapshot().get(nodeOne).currentValue()); assertEquals(4L,
     * nodeOneClock.snapshot().get(nodeTwo).currentValue()); assertEquals(2L,
     * nodeOneClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 13. 5,4,2 -> 6,4,2 :: nodeOne->nodeThree send event nodeOneClock.recordEvent(new
     * Event(EventType.SEND, nodeOne, Optional.empty())); assertEquals(6L,
     * nodeOneClock.snapshot().get(nodeOne).currentValue()); assertEquals(4L,
     * nodeOneClock.snapshot().get(nodeTwo).currentValue()); assertEquals(2L,
     * nodeOneClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 14. 2,0,3 -> 6,4,4 :: nodeThree<-nodeOne receive event senderClock = nodeOneClock.clone();
     * nodeThreeClock.recordEvent(new Event(EventType.RECEIVE, nodeThree,
     * Optional.of(senderClock))); assertEquals(6L,
     * nodeThreeClock.snapshot().get(nodeOne).currentValue()); assertEquals(4L,
     * nodeThreeClock.snapshot().get(nodeTwo).currentValue()); assertEquals(4L,
     * nodeThreeClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 15. 6,4,4 -> 6,4,5 :: nodeThree->nodeTwo send event nodeThreeClock.recordEvent(new
     * Event(EventType.SEND, nodeThree, Optional.empty())); assertEquals(6L,
     * nodeThreeClock.snapshot().get(nodeOne).currentValue()); assertEquals(4L,
     * nodeThreeClock.snapshot().get(nodeTwo).currentValue()); assertEquals(5L,
     * nodeThreeClock.snapshot().get(nodeThree).currentValue());
     * 
     * // 16. 3,4,1 -> 6,5,5 :: nodeTwo<-nodeThree receive event senderClock =
     * nodeThreeClock.clone(); nodeTwoClock.recordEvent(new Event(EventType.RECEIVE, nodeTwo,
     * Optional.of(senderClock))); assertEquals(6L,
     * nodeTwoClock.snapshot().get(nodeOne).currentValue()); assertEquals(5L,
     * nodeTwoClock.snapshot().get(nodeTwo).currentValue()); assertEquals(5L,
     * nodeTwoClock.snapshot().get(nodeThree).currentValue());
     */

    // TODO: validate EventOrderResolution

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
