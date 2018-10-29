[![Build Status](https://img.shields.io/travis/gsharma/vector-clock/master.svg)](https://travis-ci.org/gsharma/vector-clock)
[![Test Coverage](https://img.shields.io/codecov/c/github/gsharma/vector-clock/master.svg)](https://codecov.io/github/gsharma/vector-clock?branch=master)

# Vector Clocks

## Theoretical Foundation
Vector clocks find uses in distributed systems where wall clocks cannot reliably be and more importantly, should not be trusted to be synchronized across many processes. Vector clocks are maintained as vectors of logical timestamps on every node/process in the system. 

Given N "nodes" in a "system" denoted by (1..N), for every globally-known, versioned entity in the system, every node maintains a vector of N scalars modeled by the logical timestamps (Lamport clocks) corresponding to its knowledge/view of the state of the system as evidenced by its interaction with the other N-1 nodes in the system via a sequence of "events" as observed by it. These events could be local to the node or remote events generated via its interaction with the other nodes in the system. For the sake of modeling this system, remote events could be generalized to send and receive type events. Events result in monotonically increasing logical timestamps.

In order to increment a vector clock:
1. For a local event on or a send event from a node i, it only ticks its ith element locally.
2. Each event from node i->j carries with it the sender's vector clock V<sub>i</sub> to node j.
3. On receipt of an event on node j, V<sub>j</sub> ticks its local jth element and merges the V<sub>i</sub> logical timestamps provided they are not concurrent.

Two events may be causally-related if they obey the happens-before or happens-after properties. They may conversely be independent or concurrent. One of the powerful and interesting paradigms this affords systems is by way of establishing causality and conflict-detection without resorting to use of wall clocks. As compared to logical/Lamport clocks which also obey causality, Vector clocks allow detection of concurrent events and conflicting updates at the cost of using some additional space.


## API
1. Create a new Vector Clock
```java
final VectorClock nodeOneClock = new VectorClock();
```

2. Initialize a Node with this clock to init its Logical Timestamp
```java
final VectorClock nodeOneClock = new VectorClock();
```

3. Generate a local event on this vector clock
```java
nodeOneClock.recordEvent(new Event(EventType.LOCAL, nodeOne, Optional.empty()));
```

4. Clone the vector clock to create a deep copy
```java
VectorClock clonedClock = nodeOneClock.clone();
```

5. Generate a snapshot of the vector clock
```java
Map<Node, LogicalTstamp> clockSnapshot = nodeOneClock.snapshot();
```

6. Compare 2 Vector Clocks to determine relative event ordering. The compared clocks could indicate causality of recorded events or non-causality (events recorded were concurrent or not comparable.
```java
// EventOrdering could be one of HAPPENS_BEFORE, HAPPENS_AFTER, CONCURRENT, IDENTICAL, NOT_COMPARABLE
EventOrdering ordering = VectorClock.compareClocks(nodeOneClock, nodeTwoClock);
```

7. Remove a node from the vector clock
```java
nodeOneClock.removeNode(node);
```


### A note on Logical Timestamps
Note that logical timestamps can be generated from their long timestamp values but once created, they are immutable. Along the same lines, calling tick() on a logical timestamp does not modify the existing timestamp but generates a new immutable version.


## Usage Example
Let's walk through step-by-step with a test case to see the vector clocks in action. The asserts along the way will help with code clarity and set right expectations.
1. Let's model a system of 3 inter-communicating nodes
```java
final Node nodeOne = new Node("a");
final Node nodeTwo = new Node("b");
final Node nodeThree = new Node("c");
```

2. Setup vector clocks for every node
```java
final VectorClock nodeOneClock = new VectorClock();
nodeOneClock.initNode(nodeOne);
nodeOneClock.initNode(nodeTwo);
nodeOneClock.initNode(nodeThree);
```

3. Now check that every tstamp in this clock initialized to zero
```java
for (final LogicalTstamp logicalTstamp : nodeOneClock.snapshot().values()) {
  assertEquals(0L, logicalTstamp.currentValue());
}
```

4. Similarly, setup 2 vector clocks for the remaining 2 nodes
```java
final VectorClock nodeTwoClock = new VectorClock();
nodeTwoClock.initNode(nodeOne);
nodeTwoClock.initNode(nodeTwo);
nodeTwoClock.initNode(nodeThree);

final VectorClock nodeThreeClock = new VectorClock();
nodeThreeClock.initNode(nodeOne);
nodeThreeClock.initNode(nodeTwo);
nodeThreeClock.initNode(nodeThree);
```

5. Let's run a sequence of 6 events & figure sanity and ordering afforded to us by the vector clocks. We will prove that the sequence of numbering below does not correlate with wall or system clock timestamps and will test the actual ordering observed in this system via validating our expectations from the EventOrdering
```java
// event-1:: 0,0,0 -> 1,0,0 :: nodeOne local event
nodeOneClock.recordEvent(new Event(EventType.LOCAL, nodeOne, Optional.empty()));
assertEquals(1L, nodeOneClock.snapshot().get(nodeOne).currentValue());
assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

// event-2:: 0,0,0 -> 0,1,0 :: nodeTwo local event
nodeTwoClock.recordEvent(new Event(EventType.LOCAL, nodeTwo, Optional.empty()));
assertEquals(0L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
assertEquals(1L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
assertEquals(0L, nodeTwoClock.snapshot().get(nodeThree).currentValue());

// event-3:: 0,0,0 -> 0,0,1 :: nodeThree->nodeTwo send event
nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));
assertEquals(0L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
assertEquals(1L, nodeThreeClock.snapshot().get(nodeThree).currentValue());

// event-4:: nodeTwo<-nodeThree receive event
// sender clock :: 0,0,1
// receiver clock :: 0,1,0
// boom, conflict detected!
VectorClock senderClock = nodeThreeClock.clone();
VectorClockTransition transition =
    nodeTwoClock.recordEvent(new Event(EventType.RECEIVE, nodeTwo, Optional.of(senderClock)));
assertTrue(transition.isConcurrentEventConflictDetected());
// assert that nodeTwoClock rejected the event and is still at 0,1,0
assertEquals(0L, nodeTwoClock.snapshot().get(nodeOne).currentValue());
assertEquals(1L, nodeTwoClock.snapshot().get(nodeTwo).currentValue());
assertEquals(0L, nodeTwoClock.snapshot().get(nodeThree).currentValue());
```

6. Note that we observed Concurrent events - this will stall further processing on nodeTwo until conflicts are resolved. nodeOne and nodeThree can continue business as usual.
```java
// event-5:: 1,0,0 -> 2,0,0 :: nodeOne->nodeThree send event
nodeOneClock.recordEvent(new Event(EventType.SEND, nodeOne, Optional.empty()));
assertEquals(2L, nodeOneClock.snapshot().get(nodeOne).currentValue());
assertEquals(0L, nodeOneClock.snapshot().get(nodeTwo).currentValue());
assertEquals(0L, nodeOneClock.snapshot().get(nodeThree).currentValue());

// event-6:: 0,0,1 -> 0,0,2 :: nodeThree->nodeOne send event
nodeThreeClock.recordEvent(new Event(EventType.SEND, nodeThree, Optional.empty()));
assertEquals(0L, nodeThreeClock.snapshot().get(nodeOne).currentValue());
assertEquals(0L, nodeThreeClock.snapshot().get(nodeTwo).currentValue());
assertEquals(2L, nodeThreeClock.snapshot().get(nodeThree).currentValue());
```

7. Compare clocks to validate that we have not yet resolved any conflicts.
```java
// 2,0,3 (nodeThree) vs 3,0,0 (nodeOne)
assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeThreeClock, nodeOneClock));

// 2,0,3 (nodeThree) vs 0,1,0 (nodeTwo)
assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeThreeClock, nodeTwoClock));

// 3,0,0 (nodeOne) vs 0,1,0 (nodeTwo)
assertEquals(EventOrdering.CONCURRENT, VectorClock.compareClocks(nodeOneClock, nodeTwoClock));
```


## Papers & Additional Reading
[Time, Clocks and the Ordering of Events in a Distributed System](http://research.microsoft.com/en-us/um/people/lamport/pubs/time-clocks.pdf)

