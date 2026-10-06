// build.gradle.kts - Root Project
// Deproof P1 MVP

plugins {
    id("com.android.application") version "8.1.2" apply false
    id("com.android.library") version "8.1.2" apply false
    kotlin("android") version "1.9.10" apply false
    kotlin("jvm") version "1.9.10" apply false
    kotlin("plugin.serialization") version "1.9.10" apply false
    // id("com.google.dagger.hilt.android") version "2.48" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

tasks.register("printProjectInfo") {
    doLast {
        println("""
        ╔════════════════════════════════════════╗
        ║      Deproof P1 MVP - Build System    ║
        ╚════════════════════════════════════════╝

        Project: ${rootProject.name}
        Gradle: ${gradle.gradleVersion}
        Kotlin: 1.9.10
        Android SDK Compile: 34
        Min SDK: 28

        Modules:
          - app (Android application)

        Build Tasks:
          - ./gradlew build              # Full build (debug + release)
          - ./gradlew assembleDebug      # Debug APK
          - ./gradlew assembleRelease    # Release APK (requires keystore)
          - ./gradlew testDebugUnitTest  # Unit tests
          - ./gradlew connectedAndroidTest # Device tests

        """.trimIndent())
    }
}

// Task to display all available tasks
tasks.register("showTasks") {
    doLast {
        println("Available tasks:")
        rootProject.allprojects.forEach { project ->
            project.tasks.forEach { task ->
                println("  ${project.name}:${task.name}")
            }
        }
    }
}
