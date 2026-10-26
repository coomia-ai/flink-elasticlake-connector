# Contributor and Agent Instructions

This is an independent Connector repository. Read [README.md](README.md),
[development conventions](docs/development.md), and
[the initialization record](docs/development/bootstrap.md) before editing.

## Scope and Authority

- The current repository is a scaffold. Do not implement or claim an approved Sink,
  recovery protocol, SDK ABI, or serializer from candidate reference signatures.
  Read the relevant current upstream SPEC and its blocking reviews before behavior work.
- Upstream design sources and initialization hashes are recorded in
  [design-baseline.json](docs/development/design-baseline.json). Refresh design evidence
  deliberately when the relevant SPEC changes; initialization hashes are historical.
- This bootstrap does not close upstream I-06, foundation, or runtime gates. Future
  upstream task assignments must follow their task-pack and review procedures.

## Architecture Rules

- ElasticLake/Paimon owns business data. Connector spool is delivery/recovery input.
  Use the approved Java SDK and remote API; do not import database internals.
- Preserve original request identity and payload during recovery. Treat unknown
  outcomes, unreadable state, incomplete inventory, and corruption as explicit errors.
- Checkpoint completion does not imply cross-table atomic visibility. Validate real
  Flink topology, aborts, and redistribution before claiming parallel recovery support.
- Use injectable Clock/RandomSource/Storage ports for business behavior. Production
  adapters belong in `..inject.production..`; no direct system time/random/filesystem
  calls outside approved adapters. Do not modify frozen contracts without review.

## Build and Quality

- Gradle 8.x only; use the wrapper and `gradle/libs.versions.toml`. No Maven build.
- Connector production code and ordinary tests target Java 17, as specified by the
  Connector engineering contract. Shared architecture rules run separately on Java 21.
- Flink remains `compileOnly`. No implicit `mavenLocal`, source substitution, bare
  dependency JARs, or sibling checkout assumptions. SDK artifacts require exact
  versions and verified manifests before integration. Add no GPL/LGPL dependencies.
- Format with `bash ./gradlew spotlessApply`; verify with `bash ./gradlew build`.
  On Windows use `.\gradlew.bat`. Test behavior changes and use `*PropertyTest`
  with a naive oracle for core data structures. Never remove tests to hide failure.
- Keep comments about constraints, not narration. Include useful locating context in
  errors. Keep English and Chinese guides consistent and update CHANGELOG.md.
- Keep generated outputs out of Git; the Gradle wrapper JAR is the binary exception.
  Preserve existing work. Shared build/API changes require affected-owner review.
- Use `feature/{module}-{task}` branches and the Chinese commit prefixes documented
  in CONTRIBUTING.md. Do not push or publish unless the user authorizes that action.
