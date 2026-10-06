pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://maven.solanafoundation.org")
            content {
                includeGroup("org.solana")
                includeGroup("com.solanomobile")
            }
        }
    }
}

rootProject.name = "Deproof"
include(":app")
