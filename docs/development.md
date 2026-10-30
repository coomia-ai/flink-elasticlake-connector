# Development

English | [简体中文](development.zh-CN.md)

## Environment and Commands

Use JDK 17 or 21, `JAVA_HOME`, and the checked-in Gradle 8.12 wrapper. Java compilation
uses `--release 17`. A JDK 21 build does not replace actual Java 17 execution evidence.
API work consumes sealed SDK artifacts, not an SDK checkout. Database, Docker and
S3 credentials are unnecessary for API tests; explicit SDK/rules paths and trusted
hashes are described in [I-07-CONNECTOR](development/csn-i/I-07-CONNECTOR.md).

| Task | Linux / macOS command |
| --- | --- |
| List modules | `bash ./gradlew projects` |
| Build and check | `bash ./gradlew build` |
| Format | `bash ./gradlew spotlessApply` |
| Check formatting | `bash ./gradlew spotlessCheck` |
| Run future tests | `bash ./gradlew test` |
| Inspect dependencies | `bash ./gradlew :flink-connector:dependencies` |
| Regenerate dependency locks | `bash ./gradlew :flink-connector:dependencies :connector-recovery-s3:dependencies --write-locks` |

On Windows replace `bash ./gradlew` with `.\gradlew.bat`. API tests cover constructor
constraints and immutable values. Explicit foundation probes have separate linkage tests;
they do not certify delivery or recovery behavior. JUnit 5 and jqwik are configured.

## Repository Layout

```text
flink-connector/          Sink integration scaffold
connector-recovery-s3/    Optional provider scaffold
gradle/                  Wrapper and dependency/plugin version catalog
docs/                    Bilingual guides and initialization record
.github/                 CI, dependency updates, issue and PR templates
```

Source packages start with `io.elasticlake.connector`. Production Java source
directories contain package documentation; non-product fixtures live separately
under `build-support/probes`. JARs include LICENSE/NOTICE and use reproducible entry
ordering and timestamps. They are local scaffold outputs, not release candidates.
Javadoc packaging will be enabled when approved public types exist; package-only
namespaces currently have no public API to document.

## Dependencies

Use `gradle/libs.versions.toml` for versions. Flink stays `compileOnly`; never bundle
the cluster runtime into the connector. Locks use strict mode. Review lock diffs
after dependency updates. Use exact versions; do not add dynamic or changing dependencies.
Maven Central and the Gradle Plugin Portal are the configured public repositories;
there is no implicit `mavenLocal`, composite build, or sibling-source dependency.

For a restricted network, explicitly select trusted mirrors if needed:

```bash
bash ./gradlew -PpluginRepository=https://maven.aliyun.com/repository/gradle-plugin -PmavenRepository=https://maven.aliyun.com/repository/public build
```

Both properties are optional and affect build dependencies only. Defaults remain
the official public repositories. SDK and shared rules use separate exclusive repositories.

Opt-in foundation mode requires explicit `sdkRepository`, `sdkManifest` and
`sdkManifestSha256` properties. A full foundation build also requires the corresponding
`rulesRepository`, `rulesManifest` and `rulesManifestSha256`. Candidate verification
checks exact versions, metadata, inventories and artifact bytes without fallback.
The SDK API can compile independently without HTTP or architecture-rule artifacts.
Do not copy SDK DTOs or substitute sibling sources.

`nonProductProbe=true` selects reviewed build fixtures and separate strict lockfiles.
Java 17 compiles and runs ordinary tests; a separate Java 21 `architectureTest`
executes six original shared rule groups against nonempty Java 17 outputs. Actual
dependencies are captured before compilation and revalidated before sealing;
original XML, class files and task graphs are archived with hashes. Local publication
requires a fresh candidate ID and remains non-product. Rule code stays out of
ordinary tests and production artifacts. The existing CI workflow has no sealed-input provisioning and will reject missing
SDK inputs once API sources are present; see the [source delivery note](development/csn-i/SOURCE-DELIVERY-20261030.md).
local foundation evidence is recorded separately in the
[bilingual I-06 record](development/csn-i/I-06.md), including replay commands and open gates.

## Code and Review

Use Google Java Format through Spotless. Keep comments for constraints that code
cannot express; cite SPEC sections where helpful. Test behavioral changes with
JUnit 5; core structures need jqwik tests against an independent naive oracle.
Fakes cannot certify real checkpoint routing, SDK integrity, S3 semantics, or service
recovery. Read [AGENTS.md](../AGENTS.md) and [CONTRIBUTING.md](../CONTRIBUTING.md).

CI runs on Linux/Windows with JDK 17/21. See the
[GitHub Actions runs](https://github.com/coomia-ai/flink-elasticlake-connector/actions)
for actual hosted results after pushing. Branch protection and private security
reporting are separate hosting settings.
