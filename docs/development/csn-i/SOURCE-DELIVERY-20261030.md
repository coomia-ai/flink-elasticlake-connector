# Source delivery / 源码交付 — 2026-10-30

This checkpoint delivers the maintained Connector sources to GitHub `main`, as
explicitly requested by the project owner. It includes the logical API v3, pure
models, tests, artifact-verification/build mechanisms, and bilingual documentation.
It is a source checkpoint; no version tag or functional Connector artifact is released.

按项目负责人要求，本次将维护中的 Connector 源码提交到 GitHub `main`：逻辑 API v3、
纯模型、测试、制品核验及构建机制和双语文档。此次交付源码，不创建版本标签或发布可用制品。
旧报告中的“尚未提交推送”描述其各自日期的历史状态，以本记录及远端提交为准。

## Validation / 验证

- `spotlessApply`: passed; Java/build sources were already formatted.
- `:flink-connector:archiveModelEvidence --rerun-tasks`, fresh
  `modelRunId=source-main-20261030-01`: passed, 23 tasks executed in 52 seconds.
- Actual Java 17 ordinary tests and separate Java 21 architecture rules; original
  XML totals are 250 ordinary nodes plus 22 architecture rules, zero failures,
  errors or skips. Module checks include dependency-byte verification, accepted
  logical-API class identity, Java 17 bytecode, packaging and formatting.
- Fixed SDK/rules manifests remain those in [I-07-CONNECTOR](I-07-CONNECTOR.md).
  The 64 captured source inputs are unchanged after execution; 104 dependency
  records are retained. Local raw evidence remains under the ignored
  `build/model-evidence/source-main-20261030-01/` directory.

本轮先格式化，再全量重跑现有模块检查：250 个普通测试节点与 22 条架构规则，
零失败、错误、跳过。64 份捕获输入在执行后复核未变；104 条依赖记录保留。
这是本地复验，不替代此前独审，也不新增 runtime、数据库或性能资格。

Wrapper download from the official Gradle distribution endpoint timed out before
any task ran. The successful checks used the already installed Gradle 8.12 with
JDK 21 as launcher, JDK 17 for production/ordinary tests, and JDK 21 for architecture
checks. The wrapper configuration was unchanged; this run does not prove a fresh
wrapper download works.

Wrapper 官方下载超时后，使用本机已有的同版本 Gradle 8.12 完成检查；
未修改 Wrapper，也不把此次结果记为全新 Wrapper 下载成功。

## Reproduction and open gates / 重放与开放门禁

Use the explicit SDK/rules repositories, manifests and trusted SHA properties in
[I-07-CONNECTOR](I-07-CONNECTOR.md), plus both JDK installations. The pure-model
build also requires the original accepted API candidate at the relative path in
[acceptance.json](../../../api-publication/acceptance.json); obtain the entire
sealed candidate separately. A fresh source clone does not contain these generated
artifacts. Do not substitute arbitrary JARs or edit the accepted receipt.

重放需显式提供受信 SDK、规则制品及两个 JDK；纯模型构建还需要按原接收清单路径提供
完整的已接收 API 候选。生成制品不随源码入库，不可用任意 JAR 或修改收据替代。

The existing GitHub workflow runs `build` but has no sealed SDK/rules/API-candidate
provisioning. With these sources it will reject missing inputs; hosted CI is not
claimed green. Artifact distribution and a complete Connector/provider build remain
open work. No enforcement is disabled to make the workflow pass.

现有 GitHub 工作流直接执行 `build`，尚未供应受信 SDK、规则及 API 候选；
源码更新后会因缺少输入而明确失败，不声称远端 CI 通过。制品分发及完整
Connector/provider 构建仍待接线，本次不关闭或绕过这些校验。

Production Sink, Flink lifecycle/state serializers, SDK HTTP integration, S3
RecoveryStore, database interoperability and performance qualification remain
pending. JARs, classes, caches, raw logs and generated evidence are excluded from Git.

生产 Sink、Flink 生命周期与状态序列化、SDK HTTP 接入、S3 RecoveryStore、
真实数据库互通及性能资格仍待完成。JAR、class、缓存、原始日志及生成证据不入 Git。
