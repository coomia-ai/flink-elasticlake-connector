# Project Bootstrap / 工程初始化

Date / 日期：2026-10-26

## Scope / 范围

The user requested an independent, open-source-style project with English/Chinese
Markdown and Apache License 2.0. The target directory was empty and was not a Git
repository. This work initializes the Connector repository only. It does not claim
the full upstream I-06 task or modify the database/SDK build.

用户要求将空目录初始化为独立开源风格项目，提供中英文 Markdown 及 Apache License
2.0。本次仅初始化 Connector 工程，不领取完整上游 I-06，也不修改数据库或 SDK 构建。

## Preflight / 开工核验

The upstream current route and SPEC index identify SPEC-26 v0.5 and the engineering
layout contract as the applicable design. They require two independent Connector
modules, Java 17, Flink 1.20.3, Gradle 8.x, and future exact-version/SHA SDK consumption.
The referenced reviews keep formal contract approval and runtime evidence pending.
Only build preparation and documentation are covered here; candidate Java signatures,
state formats, runtime algorithms, and performance profiles are not implemented.

上游当前路线及 SPEC 索引指向 SPEC-26 v0.5 和工程布局合同：两个独立模块、Java 17、
Flink 1.20.3、Gradle 8.x，未来通过精确版本及 SHA 消费 SDK。相关评审仍保留正式合同
及 runtime 门禁；本次只做构建准备与文档，不实现候选 Java 签名、状态格式、runtime
算法或性能 profile。

Exact reviewed input hashes and the observed upstream HEAD are in
[design-baseline.json](design-baseline.json). The upstream tree
was dirty; the HEAD alone is not a complete snapshot or formal task assignment.
These hashes preserve design provenance, not authorization or gate receipts.

准确输入 SHA 和观察到的上游 HEAD 见上述清单。上游工作树存在未提交修改，HEAD
不能单独表示完整快照或正式任务派发；这些 SHA 仅用于设计追溯，不作为授权或门禁凭据。

## Deliverables / 交付

- Standalone settings/build, two module scaffolds, wrapper and catalog.
  / 独立 settings/build、两个模块骨架、Wrapper 和版本目录。
- Formatting, dependency-lock configuration, reproducible JAR settings and CI.
  / 格式检查、依赖锁配置、可复现 JAR 配置及 CI。
- LICENSE/NOTICE, bilingual guides, community policies and issue/PR templates.
  / LICENSE/NOTICE、双语指南、社区政策及 issue/PR 模板。

## Verification / 验证

Environment / 环境：Windows, Temurin JDK 21.0.10, installed Gradle 8.12;
Java compilation uses release 17. Actual Java 17 execution was not performed.
/ 使用已安装的 Gradle 8.12，编译目标为 Java 17；未执行真实 Java 17 运行验证。

The installed Gradle launcher was used with these arguments:
/ 使用已安装的 Gradle 启动器，参数如下：

```text
--no-daemon
-PpluginRepository=https://maven.aliyun.com/repository/gradle-plugin
-PmavenRepository=https://maven.aliyun.com/repository/public
:flink-connector:dependencies :connector-recovery-s3:dependencies
spotlessApply build --write-locks --console=plain
```

| Check / 检查 | Actual result / 实际结果 |
| --- | --- |
| Initial complete scaffold build / 首次完整骨架构建 | PASS, 1m 11s; 21 tasks, 14 executed and 7 up-to-date / 通过，21 个任务 |
| Ordinary locked build / 普通锁定构建 | PASS without --write-locks, 8s; 15 tasks, 4 executed and 11 up-to-date / 不更新锁文件构建通过，耗时 8 秒 |
| Tests / 测试 | Both modules report NO-SOURCE; zero runtime tests / 两模块均无测试源，不代表 runtime 验收 |
| Dependency locks / 依赖锁 | Both module lockfiles generated; runtimeClasspath empty / 两模块锁已生成，生产 runtimeClasspath 为空 |
| Main JARs / 主 JAR | Both independently readable; LICENSE/NOTICE present; no embedded Flink/database classes / 两个主包可读取，声明齐全，无内嵌 Flink 或数据库类 |
| Source JARs / 源码 JAR | NOT ACCEPTED: local E-SafeNet transformation; see below / 本机加密改写，未通过制品验收 |
| Markdown links / 文档链接 | 85 local links checked, zero missing / 85 个本地链接，零缺失 |
| YAML / 配置 | CI and Dependabot parsed; Linux/Windows x JDK17/21 matrix / 两份 YAML 解析通过，四种 CI 组合 |
| Wrapper / 引导器 | JAR SHA-256 matches official Gradle 8.12 record; distribution SHA configured / 引导 JAR 校验匹配，已配置分发校验 |

The official Wrapper distribution download was cancelled after slow progress;
this record does not claim an end-to-end Wrapper download or hosted CI success.
The local build used explicit mirrors. Full logs stay under ignored `build/`.
/ 官方 Wrapper 分发下载因速度较慢取消，本记录不声明完整下载或托管 CI 通过；本地
构建显式使用镜像，完整日志保留在被忽略的 `build/` 中。

The source JAR header contains `E-SafeNet` and `LOCK`, rather than a standard ZIP
header. Independent .NET/JDK archive readers reject it. Main JARs remain valid.
No encryption policy was changed and no decryption was attempted. Source artifact
acceptance remains open and requires an approved build/release environment.
/ 源码包文件头为 `E-SafeNet` / `LOCK`，独立 .NET/JDK 工具无法按 ZIP 读取；主包正常。
未修改加密策略，也未尝试解密。源码制品验收保留未通过状态，需在获准的构建及发布
环境中完成。

Build self-review corrected incompatible locking/dynamic-version flags and deferred
Javadoc packaging until public types exist. No public API, runtime, or state-format
implementation was introduced. This is self-review of bootstrap scope only.
/ 构建自查修复了锁配置冲突；Javadoc 打包留待公开类型建立后启用。没有引入公共 API、
runtime 或状态格式实现。本记录仅为初始化范围的自查。

## Remaining Foundation Work / 后续基础工作

Upstream foundation acceptance still requires reviewed SDK candidate publication
and SHA/metadata checks, the isolated logical API variant and acyclic task graph,
shared Java 21 architecture-rule artifacts scanning nonempty Java 17 outputs,
actual Java 17 consumers, and both independent builds. Real Flink, S3, controller,
database, and performance gates remain separate. Code hosting targets the
[GitHub repository](https://github.com/coomia-ai/flink-elasticlake-connector).
Artifact publishing, release tags, and dedicated security contacts remain unconfigured.

上游基础验收仍需经评审的 SDK 候选发布及 SHA/元数据核验、隔离逻辑 API 变体和
无环任务图、Java 21 共享规则制品对非空 Java 17 输出的扫描、真实 Java 17 消费者及
两个独立工程构建。真实 Flink、S3、控制面、数据库及性能门分别保留。源码托管使用
上述 GitHub 仓库；制品发布目标、release tag 及专用安全联系方式尚未配置。

## GitHub Initialization / GitHub 初始化

The user supplied the repository URL and authorized the initial commit and push.
The remote was empty; the initial branch is `main`. Commit `d9f0d7c` contains 47
validated source/configuration files, with no encrypted source JARs or build outputs.
All four Linux/Windows x Java 17/21 jobs passed in the
[first CI run](https://github.com/coomia-ai/flink-elasticlake-connector/actions/runs/40313763168).
This is scaffold build evidence, not SDK/Sink/runtime or release acceptance.
/ 用户提供仓库地址并授权初始提交及推送；远端原为空，初始分支为 `main`。
`d9f0d7c` 包含 47 个经核验的源码及配置文件，没有加密源码包或构建产物。
首次 CI 的 Linux/Windows x Java 17/21 四种组合均通过；这仅是骨架构建实证，
不代表 SDK、Sink、runtime 或发布验收通过。
