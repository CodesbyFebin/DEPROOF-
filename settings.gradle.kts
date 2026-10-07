pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
        maven { url = uri("https://s01.oss.sonatype.org/content/repositories/releases") }
        maven { url = uri("https://jitpack.io") }
        mavenCentral()
    }
}

rootProject.name = "Deproof"
include(":app")
