# 开发指南

[English](development.md) | 简体中文

## 环境与命令

使用 JDK 17 或 21，配置 `JAVA_HOME`，通过入库的 Gradle 8.12 Wrapper 构建；编译
采用 `--release 17`。JDK 21 构建不能替代 Java 17 实际执行证据。骨架构建不需要
数据库、Docker、SDK 源码或 S3 凭据。

| 操作 | Linux / macOS 命令 |
| --- | --- |
| 列出模块 | `bash ./gradlew projects` |
| 构建及检查 | `bash ./gradlew build` |
| 格式化 | `bash ./gradlew spotlessApply` |
| 格式检查 | `bash ./gradlew spotlessCheck` |
| 运行后续测试 | `bash ./gradlew test` |
| 查看依赖 | `bash ./gradlew :flink-connector:dependencies` |
| 重新生成依赖锁 | `bash ./gradlew :flink-connector:dependencies :connector-recovery-s3:dependencies --write-locks` |

Windows 将 `bash ./gradlew` 替换为 `.\gradlew.bat`。当前 `test` 任务没有测试源；
已配置 JUnit 5 与 jqwik，供后续行为测试使用。不要添加无意义的通过测试掩盖状态。

## 目录

```text
flink-connector/          Sink 接入骨架
connector-recovery-s3/    可选 provider 骨架
gradle/                  Wrapper 及依赖、插件版本目录
docs/                    双语指南与初始化记录
.github/                 CI、依赖更新、issue 及 PR 模板
```

包名以 `io.elasticlake.connector` 开头，当前 Java 源码只有 package 文档。JAR 包含
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

两个参数均为可选项，仅影响构建依赖，默认仍使用官方公共仓库。未来 SDK 制品必须
使用单独的独占仓库及核验合同。

SDK 制品解析尚未接线。启用前须实现经评审的精确版本与清单合同：显式隔离仓库、
SDK group 独占解析、SHA-256 及元数据核验，禁止回退。本地 SDK SNAPSHOT 必须
使用隔离封存的候选仓库及核验清单，不能按普通可变依赖处理。禁止复制 SDK DTO。

Java 21 共享架构规则制品和独立 `architectureTest` 任务属于后续基础工作；骨架当前
不提供该任务，也未通过架构门。规则代码不得进入普通 Java 17 测试、生产 classpath
或发布制品。

## 编码与评审

通过 Spotless 执行 Google Java Format。注释只解释代码无法表达的约束，必要时引用
SPEC 章节。行为测试采用 JUnit 5；核心结构采用 jqwik 并与独立朴素实现对拍。
fake 不能证明真实 checkpoint 路由、SDK 完整性、S3 语义或服务恢复。请阅读
[AGENTS.md](../AGENTS.md)与[贡献指南](../CONTRIBUTING.zh-CN.md)。

已准备 GitHub Linux/Windows、JDK 17/21 CI；只有推送仓库并实际执行工作流后才有
托管 CI 结果。公开托管、分支保护及私密安全报告属于平台侧配置。
