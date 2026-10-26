# 贡献指南

[English](CONTRIBUTING.md) | 简体中文

欢迎贡献文档、构建工具、可复现探针及经过评审的连接器工作。修改前请阅读
[项目状态](README.zh-CN.md)与[开发约定](docs/development.zh-CN.md)。

## 提出变更

通过 issue 说明问题、预期行为和验收标准；小型修复可直接提交 PR。公共 API、状态
格式、交付语义及恢复行为必须先有获批设计。上游 SPEC-26 的 runtime 授权仍待完成，
骨架构建通过不代表运行能力获批，详见[设计来源](docs/development/bootstrap.md)。

## 开发与验证

1. 使用类似 `feature/flink-connector-checkpoint-probe` 的分支。
2. 控制改动范围，并遵守 [AGENTS.md](AGENTS.md)。
3. 行为变更新增有效测试；核心数据结构需要与朴素实现对拍的属性测试，类名以
   `PropertyTest` 结尾。
4. 执行 `bash ./gradlew spotlessApply` 和 `bash ./gradlew build`；Windows 使用
   `.\gradlew.bat` 替代 `bash ./gradlew`。
5. 同步更新中英文文档，用户可见变更登记到 [CHANGELOG.md](CHANGELOG.md)。

仅使用 Gradle；依赖及插件版本集中在 `gradle/libs.versions.toml`，依赖变更时评审并
重新生成锁文件。保留第三方声明，禁止 GPL/LGPL 依赖。密钥、日志、构建产物及依赖
JAR 不入库。

## 提交 PR

说明问题、变更后的行为、设计依据及实际验证结果，明确未运行项。禁止吞错、丢弃
待交付数据、禁用失败测试，或将 mock 结果当成真实 Flink / 数据库验证。公共构建及
公共合同变更需受影响 owner 评审。

Commit 标题使用 `增加:`、`修复:`、`更新:`、`测试:`、`删除:`、`临时:`、`恢复:`
或 `合并:` 前缀，例如 `增加: Connector 双语开发指南`。

## 贡献许可

除非另行明确声明，主动提交并请求纳入项目的贡献采用 [Apache License 2.0](LICENSE)。
仅提交有权贡献的内容，并遵守[社区行为规范](CODE_OF_CONDUCT.zh-CN.md)。安全问题
按[安全政策](SECURITY.zh-CN.md)报告。
