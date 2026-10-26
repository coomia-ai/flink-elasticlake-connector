# Contributing

English | [简体中文](CONTRIBUTING.zh-CN.md)

Contributions to documentation, build tooling, reproducible probes, and reviewed
connector work are welcome. Read [the project status](README.md) and
[development conventions](docs/development.md) before changing code.

## Propose a Change

Open an issue describing the problem, expected behavior, and acceptance criteria.
Small fixes can go directly to a pull request. Public APIs, state formats, delivery
semantics, and recovery behavior need an approved design before implementation.
The upstream SPEC-26 runtime approval remains pending; a scaffold build grants no
runtime capability. See [design provenance](docs/development/bootstrap.md).

## Develop and Verify

1. Use a branch such as `feature/flink-connector-checkpoint-probe`.
2. Keep changes focused and follow [AGENTS.md](AGENTS.md).
3. Add meaningful tests for behavior changes. Core data structures need property
   tests against a naive oracle; name these `*PropertyTest`.
4. Run `bash ./gradlew spotlessApply` and `bash ./gradlew build`; on Windows use
   `.\gradlew.bat` instead of `bash ./gradlew`.
5. Update English and Chinese documentation together and record user-visible
   changes in [CHANGELOG.md](CHANGELOG.md).

Use Gradle only. Manage dependency and plugin versions in
`gradle/libs.versions.toml`; review and regenerate dependency locks when changing
dependencies. Preserve third-party attribution and add no GPL/LGPL dependencies.
Keep secrets, logs, build outputs, and dependency JARs out of Git.

## Submit a Pull Request

Explain the problem, resulting behavior, design references, and actual validation.
Mark tests that were not run. Never suppress errors, drop pending data, disable
failing tests, or present mock results as real Flink/database validation.
Changes to shared builds or public contracts need review from affected owners.

Commit subjects follow the project convention: `增加:`, `修复:`, `更新:`, `测试:`,
`删除:`, `临时:`, `恢复:`, or `合并:`. For example:
`增加: Connector 双语开发指南`.

## Contribution License

Unless explicitly stated otherwise, contributions intentionally submitted for
inclusion are licensed under the [Apache License 2.0](LICENSE). Submit only work
you have the right to contribute. Follow the [Code of Conduct](CODE_OF_CONDUCT.md).
Report vulnerabilities through [SECURITY.md](SECURITY.md).
