# I-07-CONNECTOR logical API / 逻辑API

Date: 2026-10-28. Status: exact candidate04 independently reviewed and accepted as the authorized I-16 input.

The project owner explicitly accepted the independently reviewed logical contract
and build mechanisms and instructed implementation of I-07-CONNECTOR followed by
I-16, with the existing read-only reviewer checking delivery. This is a limited
implementation phase, not a grant of the original complete FOUNDATION or runtime.
Upstream authority: `OWNER-APPROVAL-CONNECTOR-DEVELOPMENT-2026-10-28.md` and
`connector-implementation-authorization-20261028.json` in the upstream repository.

开工核验：参考ConnectorApi.v3的46公开类型及8接口已与批准SDK API真实Java17链接；
该SDK清单616d10fa、运行JAR320fc2ee保持。I-06候选04的独立源集、唯一API输出、精确制品
及17/21隔离经独审确认。候选7输入及历史8冻结匹配，I02-R1闭环，范围内无P0/P1。
原任务包静态绑定已捕获，原schema-v3完整门派发未被冒充通过；本次使用用户直接限定实施授权，
只实现本卡白名单及协调人持有的必要构建接线，不将整卡或完整FOUNDATION置DONE/GRANTED。

Implementation: reference ABI, local non-null/nonnegative checks and defensive
copies; exact logical API variant; Java17 RecoveryStore consumer; negative controls.
SDK ports such as CallBudget keep their original authority/identity and are not
converted into caller-created substitutes. No SDK source is a compile dependency.

缺字段、代际换代及溢出、permit/epoch错绑定分别保留I-17、I-16和I-21义务；本卡不实现
网络、Flink生命周期、serializer或S3。实际测试、制品和独审结论将在完成后写入。

Independent delivery review: remaining blocking P0/P1=0/0, no required API evidence
unchecked. The reviewer rehashed source/dependency/class/repository/evidence entries,
verified the 32-node acyclic task graph, relocated consumer and each rejection log.
The coordinator accepted only the exact API input under the user's explicit
API→review→model instruction. [Acceptance](../../../api-publication/acceptance.json)
does not grant complete FOUNDATION, CI/full build, runtime, database or performance.

## Current candidate / 当前准确候选

Local candidate `i07-connector-api-20261028-04`:

- Manifest SHA256: `48d75c4830b18eead2eca3c08980f87c6dde5889e9ab8631c892ede47c03b9f2`.
- Logical API JAR SHA256: `1cbb28e353289b277a6acc609c44e42498760fa1e6b318387d45b32992b3226e` (62332 bytes).
- 48 captured source/build/compiler/init inputs, 104 dependency records, 48 API
  classes, 25 repository files and 55 archived evidence files. Capture and sealing
  revalidate the original SDK/rules pins and source-bound dependency byte baseline.
- 23 tasks all executed in 143 seconds, including formatting checks. Original
  archived XML has 177 ordinary Java17 tests (174 constructor/branch cases and
  3 property/example tests) plus 22 shared architecture Java21 rules; zero
  failures/errors/skips. Two properties ran 300 checks each, with fixed seeds.
- API-only compilation with SDK arguments and no rules repository succeeded:
  3 tasks executed in 25 seconds; no main/provider/HTTP tasks.

The candidate archives its original classes, XML, input snapshots, compiler/runtime
versions and task graph. Subsequent Gradle output cleanup cannot replace that evidence.
The relocated candidate04 consumer completed five tasks in 78 seconds on real
Java17.0.21.0.2+1-LTS, resolving exactly the logical API and approved SDK JARs.
Its concrete RecoveryStore consumer implements all 14 methods for ABI linkage;
these deliberately unsupported test methods are not a RecoveryStore implementation.
Eight local-contract regressions now reject, both 431-line and strict 484-line
reference surfaces match, and ten metadata/classifier/SHA controls reject at their
expected checks. Three private source controls also reject Wrapper drift, external
compiler inputs and byte-baseline mismatch. Candidate03 fixture repair remains historical.

The compact [evidence summary](api-evidence-20261028.json) records all exact identities
and raw-evidence SHA without committing generated classes or logs. No CI or full
Connector build/install qualification is inferred from these API-only checks.

## Replay / 重放

Provide these caller-supplied properties; no sibling checkout defaults are used:

```text
-Porg.gradle.java.installations.paths=<jdk17>,<jdk21>
-PsdkRepository=<sdk-repository> -PsdkManifest=<sdk-candidate.json>
-PsdkManifestSha256=616d10fab067bb10b66f00d61e3de2aa64ed64d4f38619e5973bc6417132198a
-PrulesRepository=<rules-repository> -PrulesManifest=<rules-sdk-candidate.json>
-PrulesManifestSha256=f95c9fa88ec481f3b361b0bb83e2f9faf8ebd7ea078f9ed2832304a3e52ca42f
-PconnectorApiOnly=true
```

Format before capture: `./gradlew spotlessApply` with the SDK properties. Test:
`./gradlew :flink-connector:test :flink-connector:architectureTest`. Seal only to
a fresh local ID: add `-PapiCandidateId=<fresh-id>` and execute
`:flink-connector:sealLogicalApiCandidate --rerun-tasks`. Windows uses `gradlew.bat`.
The logical classifier is on `io.elasticlake:elasticlake-flink-connector-1.20:0.1.0-SNAPSHOT`.
Its `connectorApiElements` variant has `io.elasticlake.connector.surface=logical-api`.
It contains API classes only; its sole library dependency is the approved SDK API.

Copy `api-consumer-tests`, the entire sealed API candidate and the SDK candidate
to another directory. Run the consumer with explicit `connectorRepository`,
`connectorManifest`, `connectorManifestSha256`, `sdkRepository`, `sdkManifest`
and `sdkManifestSha256`; execute `run verifyLogicalApiNegativeControls` with
`-PnegativeRunId=<fresh-id>`. Rejection-control directories are never overwritten.

Source controls use `api-publication/verify-source-controls.ps1` with explicit
Gradle/JDK17 paths, SDK/rules repository/manifest/trusted SHA and a fresh `RunId`.
They mutate private copies only. SDK/rules original repositories remain read-only.

API-only architecture mode scans the nonempty actual API output with all six shared
groups/22 rules, on Java21. It does not claim nonempty runtime/provider scans;
ordinary full-build checks still require those outputs separately.

## Independent findings / 独审修订

- Local branch P1: disposition/output/DLQ, ACKED/LANDED ack, report/outbox and rejected
  restore/reason constraints repaired without changing signatures. Eight counterexamples
  reproduce on original candidate02 JAR; typed mixed business/DLQ remains unsupported.
- Input binding P1: Wrapper, actual compiler/source-set inputs and init scripts are
  captured; external sources reject. Source/dependency/pin/compiler inputs are checked
  before javac and sealing. All eight compile/runtime configurations are checked against
  the maintained byte baseline, not merely recorded from the current cache.
- Strict ABI reference is independently generated from the original reference classes,
  including ordered components, constructors, enum constants and permitted branches.
  [Provenance](../../../api-publication/reference-abi-provenance.json) records source,
  original archive, generator and output SHA; it is not generated from the product.

首候选01因校验closure调用了错误的root project而seal失败，原件保留。候选02保留原8项
非法接受反证；候选03的消费者fixture变量命名冲突导致编译失败，也保留原日志。
修正fixture后的03消费仅用于核实夹具，不重签03；最终交付须新的准确候选及完整重验。
首轮source01 init脚本调用错误未计目标拒绝，source02三项才计准确通过。
