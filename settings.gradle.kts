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
        providers.gradleProperty("mavenRepository").orNull?.let { mirror ->
            maven { url = uri(mirror) }
        }
        mavenCentral()
    }
}

rootProject.name = "flink-elasticlake-connector"
include("flink-connector", "connector-recovery-s3")
