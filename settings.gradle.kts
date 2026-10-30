pluginManagement {
    repositories {
        providers.gradleProperty("pluginRepository").orNull?.let { mirror ->
            maven { url = uri(mirror) }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    name = "sealedSdk"
                    url = uri(providers.gradleProperty("sdkRepository").orElse("build/missing-sdk-repository").get())
                }
            }
            filter {
                includeModule("io.elasticlake", "elasticlake-client-api")
                includeModule("io.elasticlake", "elasticlake-client-http")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "sealedRules"
                    url = uri(providers.gradleProperty("rulesRepository").orElse("build/missing-rules-repository").get())
                }
            }
            filter { includeModule("io.elasticlake", "elasticlake-architecture-rules") }
        }
        providers.gradleProperty("mavenRepository").orNull?.let { mirror ->
            maven { url = uri(mirror) }
        }
        mavenCentral()
    }
}

rootProject.name = "flink-elasticlake-connector"
include("flink-connector", "connector-recovery-s3")
