# Flink ElasticLake Connector

English | [简体中文](README.zh-CN.md)

[![CI](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml)

An independent Apache Flink connector project for writing streaming data to
[ElasticLake](https://gitee.com/spancer/elasticlake), a multimodal lakehouse database.

**Status: logical API and pure models, pre-alpha.** The Flink-independent v3 API
has real constructor and immutability tests and an isolated publication/consumer
path. See the [API delivery record](docs/development/csn-i/I-07-CONNECTOR.md).
Generation, epoch identity, pending-manager and budget models have their own
[validation record](docs/development/csn-i/I-16.md). Independent epoch/key goldens
are now consumed by the pure models, including original opaque-identity checks;
full fixture and registered-profile integration remains pending.
The production Sink, SDK HTTP integration, serializers and S3 recovery provider
are pending. No connector artifact has been released; ingestion jobs are not yet supported.

Current source publication and artifact/CI prerequisites are recorded in the
[source delivery note](docs/development/csn-i/SOURCE-DELIVERY-20261030.md). The
existing hosted workflow lacks sealed inputs and is not yet a passing build gate.

## Design Direction

The planned integration sends data through the ElasticLake Java SDK and remote
HTTP API. It does not embed the database, write directly to Paimon, or build indexes.

```text
Flink source -> application transformations -> ElasticLake Sink
                                                |
                                     recoverable spool + checkpoint
                                                |
                                      ElasticLake Java SDK
                                                |
                                       ElasticLake HTTP API
```

The proposed Sink seals recoverable requests before committing them after a
completed checkpoint. Recovery preserves request identity and original payloads.
These are design goals awaiting implementation and validation, not current guarantees.
One checkpoint does not imply atomic visibility across parts or tables.

## Build

Install JDK 17 and set `JAVA_HOME`; architecture tests also require JDK 21. Production bytecode targets Java 17.
The wrapper downloads Gradle 8.12; the first build requires network access to
Gradle distributions, the Gradle Plugin Portal, and Maven Central.

```bash
git clone https://github.com/coomia-ai/flink-elasticlake-connector.git
cd flink-elasticlake-connector
```

Linux / macOS:

```bash
bash ./gradlew :flink-connector:compileConnectorApiJava \
  -PsdkRepository=<sealed-sdk-repository> \
  -PsdkManifest=<sdk-candidate.json> -PsdkManifestSha256=<trusted-sha>
```

Windows PowerShell:

```powershell
.\gradlew.bat :flink-connector:compileConnectorApiJava `
  -PsdkRepository=<sealed-sdk-repository> `
  -PsdkManifest=<sdk-candidate.json> -PsdkManifestSha256=<trusted-sha>
```

SDK paths and trusted SHA are mandatory once API sources are present. Compilation
does not require SDK source or HTTP code. Tests and API candidate sealing also use
the explicit shared-rules repository described in the API delivery record. A full
Connector build/install is pending; the existing scaffold CI is not evidence for
the new API path. Foundation probes remain separate non-product fixtures.

## Modules

| Directory | Intended artifact | Responsibility | Status |
| --- | --- | --- | --- |
| `flink-connector` | `io.elasticlake:elasticlake-flink-connector-1.20` | Logical API; planned Sink, assembly, commit and recovery integration | API implementation; runtime pending |
| `connector-recovery-s3` | `io.elasticlake:elasticlake-connector-recovery-s3` | Optional persistent recovery store | Scaffold |

The coordinates above are reserved design names, not published dependencies.
Flink is a `compileOnly` dependency supplied by the Flink runtime. The optional S3
module currently has no provider dependency or implementation.

## Compatibility Targets

| Component | Baseline | Meaning |
| --- | --- | --- |
| Java | 17 | Production bytecode target; Java 17 execution must be validated for release |
| Apache Flink | 1.20.3 | SPEC target; runtime, topology, and rescaling tests are pending |
| Gradle | 8.12 | Independent wrapper and version catalog |
| ElasticLake SDK / server | SDK API build integration | Explicit sealed repository and trusted manifest SHA required; HTTP/server behavior pending |

These targets do not claim support for other Flink lines, SQL/Table factories,
Source connectors, or production delivery semantics.

## Documentation

- [Documentation index](docs/README.md)
- [Architecture and delivery semantics](docs/architecture.md)
- [Development and build conventions](docs/development.md)
- [Roadmap](docs/roadmap.md)
- [Release checklist](docs/releasing.md)
- [Contributing](CONTRIBUTING.md), [community conduct](CODE_OF_CONDUCT.md), and [security](SECURITY.md)
- [Initialization scope and design provenance](docs/development/bootstrap.md)
- [SDK artifact and Connector build foundation](docs/development/csn-i/I-06.md)

Source is hosted on [GitHub](https://github.com/coomia-ai/flink-elasticlake-connector).
Submit bug reports and proposals to the [issue tracker](https://github.com/coomia-ai/flink-elasticlake-connector/issues).
Use private reporting for security concerns. Artifact publishing and verified
maintainer contact details remain to be configured before the first release.

## License

Copyright 2026 The Flink ElasticLake Connector Authors.
Licensed under the [Apache License, Version 2.0](LICENSE). See [NOTICE](NOTICE)
for attribution. This independent project is not an Apache Software Foundation project.
