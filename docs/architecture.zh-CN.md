# 架构说明

[English](architecture.md) | 简体中文

**设计状态：** 以下描述上游候选方向；runtime 授权、实现及真实服务验证仍待完成。
准确设计基线见[初始化记录](development/bootstrap.md)。

## 系统边界

连接器将业务 mutation 转为 ElasticLake Java SDK 请求。校验、WAL、Paimon 持久化、
索引、RowID 执行和查询可见性均由 ElasticLake 负责。Paimon 是数据库唯一 SoT；
连接器 spool 仅保存可恢复的交付输入，不作为业务查询存储。

Flink API 由集群提供；连接器生产依赖不内嵌数据库 `core-api`、server、Paimon、
Lucene 或日志实现。可选 S3 provider 将消费获批的逻辑恢复 SPI，Sink 不与具体
存储实现耦合。

## 候选写入链路

1. Writer 校验完整 mutation，写入有界、不可变 spool chunk，不直接写数据库。
2. checkpoint assembler 核验所有 writer 的 summary 及完整源覆盖，封存准确请求
   字节、身份和完整 manifest。
3. 真实完成的 checkpoint 授权 commit callback；先记录持久化发送 intent，再经
   SDK 发出请求。
4. 提交流程收敛原请求，逐表 finalize，并持久化 delivery report 后才能报告成功。

候选拓扑为并行 Writer 和一个逻辑提交 lane。标准 Sink2 可能创建多个物理 Committer
子任务，必须通过真实 Flink 1.20.3 验证拓扑、summary 路由、checkpoint abort 及状态
重分配，才能授予并行或重分片 profile；assembler 并行度为一不是证明。

## 交付与恢复

checkpoint 完成只授予提交资格，不构成跨表数据库事务；part 与表可能逐步可见。
durable 确认、NRT 可见、landed 数据及 finalized delivery 是不同观测。当前没有
exactly-once、延迟、吞吐或自动 failover 保证。

恢复必须保留封存内容、请求 key、源覆盖、checkpoint 身份及最初尝试信息。未知
结果需要按原请求核定，禁止换身份绕过未知结果、丢弃 abort checkpoint 的待办，
或把不可读库存当作空库存。

首次启动、首个 checkpoint 完成前失败及 checkpoint 恢复是不同的受控路径，需可信
许可、完整恢复库存以及旧 run 和旧 I/O 已停止的证据。当前骨架没有实现 serializer、
状态格式或可信控制面。

## 失败及资源约束

错误应含定位上下文并停止不安全工作。不支持的 profile、缺失状态、损坏内容、源
覆盖不完整、依赖不匹配及预算耗尽均必须明确失败。spool、队列、请求、重试及
callback 时间必须有经测量的上界；future 完成不能单独证明底层 I/O 已停止。

这些性质需要属性测试、模型测试及真实 Flink / 数据库证据，空模块构建无法证明。
