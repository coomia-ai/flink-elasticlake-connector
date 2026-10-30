# Flink ElasticLake Connector

[English](README.md) | 简体中文

[![CI](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/coomia-ai/flink-elasticlake-connector/actions/workflows/ci.yml)

面向多模态湖仓数据库 [ElasticLake](https://gitee.com/spancer/elasticlake) 的独立
Apache Flink 连接器项目，用于流式数据写入。

**当前状态：逻辑 API 与纯模型，pre-alpha。** 已实现 Flink 无关的 v3 API、构造及不可变测试，
并提供独立发布/消费者验证入口，详见 [API 交付记录](docs/development/csn-i/I-07-CONNECTOR.md)。生产 Sink、
SDK HTTP 接入、状态序列化和 S3 恢复实现仍待完成。当前未发布连接器制品，尚不能运行入库作业。

代际、epoch 身份、待回调槽位及预算模型的实证见 [模型交付记录](docs/development/csn-i/I-16.md)，
已消费独立 epoch/key 黄金并检查原始 opaque 身份；完整夹具与 profile 合流资格仍待完成。

当前源码提交范围及制品/CI 前置条件见[源码交付记录](docs/development/csn-i/SOURCE-DELIVERY-20261030.md)。
现有远端工作流尚缺受信制品输入，不能作为已通过的构建门禁。

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

安装 JDK 17 并配置 `JAVA_HOME`，架构测试另需 JDK 21。生产字节码目标为 Java 17。Wrapper 使用
Gradle 8.12，首次构建需访问 Gradle 分发站点、Gradle Plugin Portal 和 Maven Central。

```bash
git clone https://github.com/coomia-ai/flink-elasticlake-connector.git
cd flink-elasticlake-connector
```

Linux / macOS：

```bash
bash ./gradlew :flink-connector:compileConnectorApiJava \
  -PsdkRepository=<sealed-sdk-repository> \
  -PsdkManifest=<sdk-candidate.json> -PsdkManifestSha256=<trusted-sha>
```

Windows PowerShell：

```powershell
.\gradlew.bat :flink-connector:compileConnectorApiJava `
  -PsdkRepository=<sealed-sdk-repository> `
  -PsdkManifest=<sdk-candidate.json> -PsdkManifestSha256=<trusted-sha>
```

API 源码存在后必须显式提供 SDK 仓库、清单和受信 SHA；单独编译不需要 SDK 源码或 HTTP。
测试及 API 封存另需显式共享规则制品，完整命令见交付记录。完整 Connector 构建/安装仍待完成，
既有骨架 CI 不代表新 API 路径已通过。可选探针继续使用独立非产品源码。
可选构建 fixture 使用独立源码目录、依赖锁和输出目录，其测试不证明 Sink 行为。
前置条件、命令与证据见[双语构建基础记录](docs/development/csn-i/I-06.md)。
生成的 JAR 不包含可用的连接器实现。

## 模块

| 目录 | 规划制品坐标 | 职责 | 状态 |
| --- | --- | --- | --- |
| `flink-connector` | `io.elasticlake:elasticlake-flink-connector-1.20` | 逻辑 API；后续 Sink、装配、提交与恢复接入 | API 已实现，运行待完成 |
| `connector-recovery-s3` | `io.elasticlake:elasticlake-connector-recovery-s3` | 可选的持久化恢复存储 | 骨架 |

以上是设计预留坐标，尚未发布。Flink 依赖为 `compileOnly`，由 Flink 集群提供。
可选 S3 模块暂未接入 provider 依赖或实现。

## 兼容目标

| 组件 | 基线 | 含义 |
| --- | --- | --- |
| Java | 17 | 生产字节码目标；发布前必须验证 Java 17 实际执行 |
| Apache Flink | 1.20.3 | SPEC 目标版本；runtime、拓扑及重分片验证待完成 |
| Gradle | 8.12 | 独立 Wrapper 和版本目录 |
| ElasticLake SDK / 服务端 | SDK API 构建接入 | 必须显式提供封存仓库及受信清单 SHA；HTTP/服务端行为待验证 |

这些目标不代表已支持其他 Flink 版本、SQL/Table 工厂、Source 连接器或生产交付语义。

## 文档

- [文档索引](docs/README.zh-CN.md)
- [架构与交付语义](docs/architecture.zh-CN.md)
- [开发与构建约定](docs/development.zh-CN.md)
- [路线图](docs/roadmap.zh-CN.md)
- [发布检查](docs/releasing.zh-CN.md)
- [贡献指南](CONTRIBUTING.zh-CN.md)、[社区行为规范](CODE_OF_CONDUCT.zh-CN.md)、[安全政策](SECURITY.zh-CN.md)
- [初始化范围与设计来源](docs/development/bootstrap.md)
- [SDK 制品与 Connector 构建基础](docs/development/csn-i/I-06.md)

源码托管于 [GitHub](https://github.com/coomia-ai/flink-elasticlake-connector)，普通缺陷
及建议通过 [Issue](https://github.com/coomia-ai/flink-elasticlake-connector/issues) 跟踪。
安全问题请私密报告；首次发布前还需配置制品发布及可验证的维护者联系方式。

## 许可证

Copyright 2026 The Flink ElasticLake Connector Authors.
本项目采用 [Apache License 2.0](LICENSE)，归属声明见 [NOTICE](NOTICE)。
本项目独立维护，不属于 Apache Software Foundation 项目。
