# Releasing

English | [简体中文](releasing.zh-CN.md)

There is no release or remote publishing task yet. Do not distribute the scaffold
JARs as a functional connector. Before the first release:

1. Verify the [GitHub repository](https://github.com/coomia-ai/flink-elasticlake-connector)
   URL and publication metadata. Configure verified maintainer/security contacts,
   branch protection, and a controlled artifact repository; do not invent upload targets.
2. Obtain the required API, state-format, recovery, and runtime approvals. Validate
   real SDK/server/Flink behavior and supported deployment profiles independently.
3. Build from a clean source baseline with the wrapper, reviewed version catalog,
   dependency locks, exact SDK manifest, and JDK 17 execution. Run Java 21 architecture
   rules separately and retain the actual reports.
4. Prepare immutable release versions, publication metadata, sources, Javadoc,
   LICENSE/NOTICE, dependency attribution, and SHA-256 manifests. Review dependency
   licenses and an SBOM. Review signatures and repository credentials before publishing.
5. Inspect artifact contents: no Flink runtime, database internals, test rules,
   secrets, duplicate logical API classes, or unapproved provider dependencies.
   Install into a clean Flink runtime and validate classloader identity.
6. Update both language editions, compatibility results, security support policy,
   changelog and upgrade/recovery instructions. Explicitly document unsupported cases.
7. Have affected owners review the exact release inputs and results, then publish
   through the configured process and verify the downloaded artifacts and checksums.

Publishing artifacts, creating release tags, and announcing releases are separate
actions from local project initialization. A CI build alone is not a release gate.
