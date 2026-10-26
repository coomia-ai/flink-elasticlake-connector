description = "Apache Flink 1.20 sink integration for ElasticLake (scaffold)"

base {
    archivesName.set("elasticlake-flink-connector-1.20")
}

dependencies {
    compileOnly(libs.flink.streaming.java)
}
