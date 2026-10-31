# Contributor and Agent Instructions

This is an independent Connector repository. Read [README.md](README.md),
[development conventions](docs/development.md), and
[the initialization record](docs/development/bootstrap.md) before editing.
For build or SDK artifact work also read [the Connector foundation record](docs/development/csn-i/I-06.md).
Foundation fixtures are explicitly non-product code; they do not approve Connector API/runtime behavior.

## 上游需求与工作区导航

两个项目的需求统一维护在 `elasticlake` 仓库，本仓库保留自身开发与验证文档，不另建需求事实源。
本地双仓库布局下，先查 [跨仓库正式导航](../elasticlake/docs/development/workspace-navigation.md)和 [当前执行路线](../elasticlake/docs/CURRENT-ROUTE.md)，再从上游 [需求索引](../elasticlake/docs/requirements/README.md)、[SPEC 索引](../elasticlake/docs/specs/README.md)定位任务正文、阻塞评审和任务卡。
上述相对路径仅用于本地文档导航，不构成构建依赖。单独 checkout 本仓库时，按 [design-baseline.json](docs/development/design-baseline.json)定位上游设计来源并获取当前任务材料；初始化 hash 仅供追溯，不代表当前授权。
材料缺失时明确报告，不猜测合同或推进依赖这些材料的行为开发。本节不改变下方独立构建与阶段门禁规则。

## Scope and Authority

- The current repository has a logical API implementation; Sink runtime is pending. Do not implement or claim an approved Sink,
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
