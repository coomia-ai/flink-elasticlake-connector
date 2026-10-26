# Development

English | [简体中文](development.zh-CN.md)

## Environment and Commands

Use JDK 17 or 21, `JAVA_HOME`, and the checked-in Gradle 8.12 wrapper. Java compilation
uses `--release 17`. A JDK 21 build does not replace actual Java 17 execution evidence.
No database, Docker, SDK checkout, or S3 credentials are needed to build the scaffold.

| Task | Linux / macOS command |
| --- | --- |
| List modules | `bash ./gradlew projects` |
| Build and check | `bash ./gradlew build` |
| Format | `bash ./gradlew spotlessApply` |
| Check formatting | `bash ./gradlew spotlessCheck` |
| Run future tests | `bash ./gradlew test` |
| Inspect dependencies | `bash ./gradlew :flink-connector:dependencies` |
| Regenerate dependency locks | `bash ./gradlew :flink-connector:dependencies :connector-recovery-s3:dependencies --write-locks` |

On Windows replace `bash ./gradlew` with `.\gradlew.bat`. The `test` tasks currently
have no test sources. JUnit 5 and jqwik are configured for future behavior tests.
Do not add placeholder passing tests to disguise this status.

## Repository Layout

```text
flink-connector/          Sink integration scaffold
connector-recovery-s3/    Optional provider scaffold
gradle/                  Wrapper and dependency/plugin version catalog
docs/                    Bilingual guides and initialization record
.github/                 CI, dependency updates, issue and PR templates
```

Source packages start with `io.elasticlake.connector`. The only current Java sources
are package documentation. JARs include LICENSE/NOTICE and use reproducible entry
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
the official public repositories. Future SDK artifacts must use their separate,
exclusive repository and verification contract.

SDK artifact resolution is not wired yet. Before enabling it, implement the reviewed
exact-version/manifest contract: an explicit isolated repository, exclusive SDK group
resolution, SHA-256 and metadata verification, and no fallback. Local SDK SNAPSHOTs
need isolated, sealed candidate repositories and verified manifests, rather than
ordinary mutable dependency handling. Do not copy SDK DTOs into this repository.

The shared Java 21 architecture-rule artifact and a separate `architectureTest`
task remain future foundation work. The scaffold does not provide that task or
claim an architecture gate passed. Rule code must stay out of ordinary Java 17
tests, production classpaths, and published artifacts.

## Code and Review

Use Google Java Format through Spotless. Keep comments for constraints that code
cannot express; cite SPEC sections where helpful. Test behavioral changes with
JUnit 5; core structures need jqwik tests against an independent naive oracle.
Fakes cannot certify real checkpoint routing, SDK integrity, S3 semantics, or service
recovery. Read [AGENTS.md](../AGENTS.md) and [CONTRIBUTING.md](../CONTRIBUTING.md).

CI is prepared for GitHub on Linux/Windows with JDK 17/21. Hosted CI results only exist
after the repository is pushed and workflows execute. Public hosting, branch
protection, and private security reporting are external setup steps.
