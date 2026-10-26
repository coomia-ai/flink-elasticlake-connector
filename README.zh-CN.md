# Flink ElasticLake Connector

[English](README.md) | 简体中文

[![CI](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml)

面向多模态湖仓数据库 [ElasticLake](https://gitee.com/spancer/elasticlake) 的独立
Apache Flink 连接器项目，用于流式数据写入。

**当前状态：工程骨架，pre-alpha。** 已提供构建配置、模块目录和社区文档；Sink、SDK
接入、状态序列化和 S3 恢复实现尚未开发。当前没有发布连接器制品，也不能运行入库作业。

## 设计方向

计划通过 ElasticLake Java SDK 调用远程 HTTP API；连接器不内嵌数据库、不绕过服务端
直接写 Paimon，也不负责构建索引。

```text
Flink 数据源 -> 业务转换 -> ElasticLake Sink
                                |
                        可恢复 spool + checkpoint
                                |
                       ElasticLake Java SDK
                                |
                       ElasticLake HTTP API
```

候选方案在 checkpoint 完成前封存可恢复请求，完成后才提交；恢复时保留原请求身份和
原始内容。这些属于待实现、待验证的设计目标。一个 checkpoint 不代表各 part 或各表
原子可见，当前不承诺生产级交付语义。

## 构建

安装 JDK 17 或 21 并配置 `JAVA_HOME`。生产字节码目标为 Java 17。Wrapper 使用
Gradle 8.12，首次构建需访问 Gradle 分发站点、Gradle Plugin Portal 和 Maven Central。

```bash
git clone https://github.com/coomia-ai/flink-elasticlake-connector.git
cd flink-elasticlake-connector
```

Linux / macOS：

```bash
bash ./gradlew build
```

Windows PowerShell：

```powershell
.\gradlew.bat build
```

骨架构建成功仅说明构建配置、格式检查和打包通过。当前没有 runtime 测试，`test`
任务显示 `NO-SOURCE`；生成的 JAR 不包含可用的连接器实现。

## 模块

| 目录 | 规划制品坐标 | 职责 | 状态 |
| --- | --- | --- | --- |
| `flink-connector` | `io.elasticlake:elasticlake-flink-connector-1.20` | Flink Sink、checkpoint 装配、提交与恢复接入 | 骨架 |
| `connector-recovery-s3` | `io.elasticlake:elasticlake-connector-recovery-s3` | 可选的持久化恢复存储 | 骨架 |

以上是设计预留坐标，尚未发布。Flink 依赖为 `compileOnly`，由 Flink 集群提供。
可选 S3 模块暂未接入 provider 依赖或实现。

## 兼容目标

| 组件 | 基线 | 含义 |
| --- | --- | --- |
| Java | 17 | 生产字节码目标；发布前必须验证 Java 17 实际执行 |
| Apache Flink | 1.20.3 | SPEC 目标版本；runtime、拓扑及重分片验证待完成 |
| Gradle | 8.12 | 独立 Wrapper 和版本目录 |
| ElasticLake SDK / 服务端 | 未接入 | 接入前须绑定精确 SDK 版本及 SHA-256 清单 |

这些目标不代表已支持其他 Flink 版本、SQL/Table 工厂、Source 连接器或生产交付语义。

## 文档

- [文档索引](docs/README.zh-CN.md)
- [架构与交付语义](docs/architecture.zh-CN.md)
- [开发与构建约定](docs/development.zh-CN.md)
- [路线图](docs/roadmap.zh-CN.md)
- [发布检查](docs/releasing.zh-CN.md)
- [贡献指南](CONTRIBUTING.zh-CN.md)、[社区行为规范](CODE_OF_CONDUCT.zh-CN.md)、[安全政策](SECURITY.zh-CN.md)
- [初始化范围与设计来源](docs/development/bootstrap.md)

源码托管于 [GitHub](https://github.com/coomia-ai/flink-elasticlake-connector)，普通缺陷
及建议通过 [Issue](https://github.com/coomia-ai/flink-elasticlake-connector/issues) 跟踪。
安全问题请私密报告；首次发布前还需配置制品发布及可验证的维护者联系方式。

## 许可证

Copyright 2026 The Flink ElasticLake Connector Authors.
本项目采用 [Apache License 2.0](LICENSE)，归属声明见 [NOTICE](NOTICE)。
本项目独立维护，不属于 Apache Software Foundation 项目。
