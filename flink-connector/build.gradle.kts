description = "Apache Flink 1.20 sink integration for ElasticLake (scaffold)"

base {
    archivesName.set("elasticlake-flink-connector-1.20")
}

dependencies {
    compileOnly(libs.flink.streaming.java)
}

apply(from = rootProject.file("gradle/connector-api.gradle"))

apply(from = rootProject.file("api-publication/build.gradle"))

apply(from = rootProject.file("gradle/connector-model.gradle"))
