# JustinDB

<img src="https://github.com/justin-db/JustinDB/blob/master/logo.png" align="right" width="280" />

[![Build Status](https://travis-ci.org/justin-db/JustinDB.svg?branch=master)](https://travis-ci.org/justin-db/JustinDB)
[![codecov](https://codecov.io/gh/justin-db/JustinDB/branch/master/graph/badge.svg)](https://codecov.io/gh/justin-db/JustinDB)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/f5f10352c6e74aa99d0f996cf0a77124)](https://www.codacy.com/app/mateusz-maciaszekhpc/JustinDB)
[![License](http://img.shields.io/:license-Apache%202-red.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt)
![Project Status](https://img.shields.io/badge/status-beta-yellow.svg)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-brightgreen.svg)](https://gitter.im/justin-db/Lobby)

**Distributed Key-Value Storage built on top of Scala/Akka**

JustinDB KV is an eventually consistent key-value database that favours write availability.
It’s a faithful implementation of Amazon’s Dynamo, with advanced features such as vector clocks for conflict resolution.
JustinDB is also fault-tolerant. Servers can go up or down at any moment with no single point of failure.

## Summary of techniques

| Problem | Technique  | Advantage  |
|---------|------------|------------|
|Partitioning                      |Consistent Hashing                                    |Incremental Scalability|
|Membership and failure detection  |Gossip-based membership protocol and failure detection|Preserves symmetry and avoids having a centralized registry for storing membership and node liveness information|
|High Availability for writes      |Vector clocks with reconciliation during reads        |Version size is decoupled from update rites|
|Recovering from permanent failures|Anti-entropy using Merkle trees                       |Synchronizes divergent replicas in the background|
|Conflicts resolution              |CRDTs                                                 |Automatic conflict resolution during reads

### Why akka
Its a toolkit and runtime for building highly concurrent applications which comes
with ideas that have been around from some time - actor model.
Besides that it has many welcome features around clustering:

1. load balancing
2. location transparency
3. self maintenance
4. fault tolerance

### Authentication, authorization, validation

In case it's not obvious, Justin performs no authentication, authorization, or any validation of input data. Clients must implement those things themselves.

## System Requirements
JustinDB works with Java 8 and newer.