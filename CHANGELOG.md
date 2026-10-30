# Changelog / 变更记录

User-visible changes are recorded here. No version has been released.
/ 本文件记录用户可见变更，当前尚未发布版本。

## Unreleased / 未发布

### Added / 新增

- Deterministic new-part key framing with independent epoch/key goldens, original
  opaque-identity checks and source/resource evidence; recovery retains old keys.
  / 新part确定性key frame、独立epoch/key黄金、原opaque校验及源/资源证据；恢复保原key。
- Pure generation/CAS, canonical epoch frame, original identity, pending-manager
  and callback budget models with naive-oracle property checks.
  / 纯代际/CAS、epoch frame、原身份、待回调槽位及预算模型，含朴素对拍属性测试。
- Flink-independent Connector API v3 with defensive values, local closed-branch
  checks, isolated logical variant publication and a Java17 RecoveryStore consumer.
  / Flink 无关的 Connector API v3、防御性值对象与局部分支校验、独立逻辑变体及 Java17 消费者。
- Replayable engineering rejection controls for missing SDK repositories, private
  Maven fallback attempts and shared-rule leakage into test classpaths or JARs.
  / 可重放的缺SDK仓库、私有Maven回退尝试及共享规则泄漏拒绝控制。
- Opt-in SDK artifact verification, logical API/provider isolation, separate Java
  17/21 checks and sealed local non-product foundation candidates with evidence.
  / 可选 SDK 制品核验、逻辑 API/provider 隔离、Java 17/21 分离检查，以及附带证据的本地非产品基础候选。
- Independent Gradle 8.12 build with two Java 17 module scaffolds.
  / 独立 Gradle 8.12 构建及两个 Java 17 模块骨架。
- Apache License 2.0, NOTICE, bilingual README and community documentation.
  / Apache License 2.0、NOTICE、双语 README 及社区文档。
- Formatting, dependency locking, GitHub CI, and contribution templates.
  / 格式检查、依赖锁、GitHub CI 及贡献模板。
- GitHub repository links, clone instructions, and a CI status badge.
  / GitHub 仓库链接、克隆命令及 CI 状态徽章。

Sink runtime, SDK HTTP integration and recovery implementations remain pending.
/ Sink runtime、SDK HTTP 接入及恢复实现仍待完成。
