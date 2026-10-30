description = "Optional S3 recovery store for the ElasticLake Flink connector (scaffold)"

base {
    archivesName.set("elasticlake-connector-recovery-s3")
}

apply(from = rootProject.file("gradle/connector-provider.gradle"))
