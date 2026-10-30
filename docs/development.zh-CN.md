# 开发指南

[English](development.md) | 简体中文

## 环境与命令

使用 JDK 17 或 21，配置 `JAVA_HOME`，通过入库的 Gradle 8.12 Wrapper 构建；编译
采用 `--release 17`。JDK 21 构建不能替代 Java 17 实际执行证据。API 工作消费封存 SDK
制品，不依赖 SDK 源码、数据库、Docker 或 S3 凭据；显式仓库/清单/受信 SHA 及规则制品
参数见 [I-07-CONNECTOR](development/csn-i/I-07-CONNECTOR.md)。

| 操作 | Linux / macOS 命令 |
| --- | --- |
| 列出模块 | `bash ./gradlew projects` |
| 构建及检查 | `bash ./gradlew build` |
| 格式化 | `bash ./gradlew spotlessApply` |
| 格式检查 | `bash ./gradlew spotlessCheck` |
| 运行后续测试 | `bash ./gradlew test` |
| 查看依赖 | `bash ./gradlew :flink-connector:dependencies` |
| 重新生成依赖锁 | `bash ./gradlew :flink-connector:dependencies :connector-recovery-s3:dependencies --write-locks` |

Windows 将 `bash ./gradlew` 替换为 `.\gradlew.bat`。API 测试覆盖构造约束和深不可变值；
显式构建探针有独立链接测试，不证明交付或恢复行为。已配置 JUnit 5 与 jqwik。

## 目录

```text
flink-connector/          Sink 接入骨架
connector-recovery-s3/    可选 provider 骨架
gradle/                  Wrapper 及依赖、插件版本目录
docs/                    双语指南与初始化记录
.github/                 CI、依赖更新、issue 及 PR 模板
```

包名以 `io.elasticlake.connector` 开头，生产 Java 源码目录只有 package 文档；
非产品 fixture 单独放在 `build-support/probes`。JAR 包含
LICENSE/NOTICE，并使用可复现的条目顺序及时间戳；这些只是本地骨架产物，不是
发布候选。
待获批的公开类型建立后再启用 Javadoc 打包；当前只有 package 命名空间，没有可供
生成文档的公开 API。

## 依赖

版本集中在 `gradle/libs.versions.toml`。Flink 使用 `compileOnly`，禁止打包集群
runtime。依赖锁采用 strict 模式，更新依赖后须评审锁文件差异。依赖必须使用精确
版本，不得引入动态或 changing 依赖。公共依赖来源为 Maven Central 与 Gradle Plugin Portal，不隐式使用
`mavenLocal`、composite 或相邻源码。

网络受限时，可显式选择可信镜像：

```bash
bash ./gradlew -PpluginRepository=https://maven.aliyun.com/repository/gradle-plugin -PmavenRepository=https://maven.aliyun.com/repository/public build
```

两个参数均为可选项，仅影响构建依赖，默认仍使用官方公共仓库。SDK 和共享规则
使用单独的独占仓库。

显式基础模式必须提供 `sdkRepository`、`sdkManifest`、`sdkManifestSha256`；完整
基础构建还须提供对应 `rulesRepository`、`rulesManifest`、`rulesManifestSha256`。
核验精确版本、元数据、文件清单和制品字节，禁止回退。逻辑 API 可以只依赖 SDK API
单独编译，无需 HTTP 或架构规则制品。禁止复制 SDK DTO 或使用相邻源码替换。

`nonProductProbe=true` 选择构建 fixture 和独立严格依赖锁。普通编译与测试使用
Java 17，独立 Java 21 `architectureTest` 对非空 Java 17 输出执行原六组共享规则。
编译前捕获实际依赖，封存前复核，并将原始 XML、class 和任务图按 SHA 留存。
本地发布必须使用全新候选 ID，且仍为非产品阶段。规则不进入普通测试或生产制品。
现有 CI 尚未供应受信制品，存在 API 源码时会因缺 SDK 输入而拒绝；
详见[源码交付记录](development/csn-i/SOURCE-DELIVERY-20261030.md)。本地基础实证单独记录在[双语 I-06 报告](development/csn-i/I-06.md)，
包括重放命令和仍未通过的验收门。

## 编码与评审

通过 Spotless 执行 Google Java Format。注释只解释代码无法表达的约束，必要时引用
SPEC 章节。行为测试采用 JUnit 5；核心结构采用 jqwik 并与独立朴素实现对拍。
fake 不能证明真实 checkpoint 路由、SDK 完整性、S3 语义或服务恢复。请阅读
[AGENTS.md](../AGENTS.md)与[贡献指南](../CONTRIBUTING.zh-CN.md)。

CI 覆盖 Linux/Windows、JDK 17/21；推送后实际执行结果见
[GitHub Actions](https://github.com/coomia-ai/flink-elasticlake-connector/actions)。
分支保护及私密安全报告需在托管平台单独配置。
