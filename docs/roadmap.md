# Roadmap

English | [简体中文](roadmap.zh-CN.md)

This roadmap describes acceptance stages, not delivery dates or released features.

| Stage | Scope | Acceptance |
| --- | --- | --- |
| Project bootstrap | Standalone build, module layout, bilingual docs, licensing, CI configuration | Scaffold builds; no runtime claim |
| Reviewed foundation | Exact SDK artifacts, isolated logical API variant, architecture rules, Java 17 consumers | Approved contracts, version/SHA checks, nonempty rule scans and independent builds |
| Protocol and model | Source coverage, identity, immutable spool, serializers, recovery model | Independent byte fixtures, property/model tests, corrupt/unsupported input rejection |
| Flink Sink | Writers, assembler, commit lane, checkpoint and restore integration | Actual Flink 1.20.3 topology, abort, bounded drain, rescaling and classloader evidence |
| Optional S3 recovery | Immutable objects, conditional control updates, inventory and retention | Real S3 fault/CAS/pagination tests and controlled-run evidence |
| End-to-end release | Real SDK/server writes, reconciliation, installable artifacts and reports | Recovery/data oracle, resource limits, compatibility and license review |

The current initialization covers the bootstrap scope. It does not complete the
upstream I-06 foundation acceptance, formal API review, or product runtime gates.
Parallel commit/rescaling, unattended failover, SQL/Table integration, Source
connectors, other Flink versions, and performance guarantees require separate
design decisions and evidence. In particular, checkpoint completion does not
provide cross-table atomic visibility.
