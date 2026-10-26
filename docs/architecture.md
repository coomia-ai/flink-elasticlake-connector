# Architecture

English | [简体中文](architecture.zh-CN.md)

**Design status:** the following describes the upstream candidate direction.
Runtime approval, implementation, and real-service validation remain pending.
See [the bootstrap record](development/bootstrap.md) for the exact design baseline.

## System Boundary

The connector translates application mutations into requests to the ElasticLake
Java SDK. ElasticLake owns validation, WAL, Paimon persistence, indexes, RowID
execution, and query visibility. Paimon remains the database's sole source of truth;
connector spool is recoverable delivery input, not queryable business storage.

Flink APIs are supplied by the cluster. No database `core-api`, server, Paimon,
Lucene, or logging implementation is embedded in connector production dependencies.
The optional S3 provider will consume the approved logical recovery SPI rather
than coupling the Sink to a concrete storage implementation.

## Proposed Write Path

1. Writers validate complete mutations and persist bounded, immutable spool chunks.
   They do not write to the database.
2. A checkpoint assembler verifies every writer's summary and source coverage,
   then seals exact request bytes, identities, and a complete manifest.
3. A commit callback authorized by a completed checkpoint records durable send
   intent before issuing requests through the SDK.
4. Commit processing resolves original requests, finalizes each table, and persists
   delivery reports before reporting success.

The candidate has parallel writers and a single logical commit lane. Standard
Sink2 may create multiple physical Committer subtasks. Real Flink 1.20.3 topology,
summary routing, checkpoint aborts, and state redistribution must be proved before
granting parallel or rescaling profiles; an assembler parallelism of one is not proof.

## Delivery and Recovery

A completed checkpoint authorizes submission, not a multi-table database transaction.
Parts and tables can become visible progressively. Durable acknowledgment, NRT
visibility, landed data, and finalized delivery are distinct observations. No current
exactly-once, latency, throughput, or automatic failover guarantee is made.

Recovery must preserve sealed payload bytes, request keys, source coverage,
checkpoint identity, and original first-attempt information. Unknown outcomes require
reconciliation of the same request. Regenerating an identity to bypass uncertainty,
dropping aborted-checkpoint work, or treating unreadable inventory as empty is invalid.

First startup, failure before the first completed checkpoint, and checkpoint restore
are separate controlled paths in the candidate design. They require trusted permits,
complete recovery inventory, and evidence that the previous run and its I/O stopped.
No serializer, state format, or trusted controller is implemented in this scaffold.

## Failure and Resource Rules

Failures must include locating context and stop unsafe work. Unsupported profiles,
missing state, corrupt payloads, incomplete source coverage, dependency mismatches,
and exhausted budgets must fail explicitly. Spool, queues, requests, retries, and
callback time require measured bounds. A completed future alone does not prove
that underlying I/O has stopped.

These properties require property/model tests and real Flink/database evidence.
Building empty modules cannot establish them.
