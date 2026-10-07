// build.gradle.kts - Deproof App Module
// Complete configuration for P1 MVP

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.deproof.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.deproof.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-p1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // BuildConfig fields
        buildConfigField("String", "API_ENDPOINT", "\"https://api.mainnet-beta.solana.com\"")
        buildConfigField("String", "SKR_MINT", "\"SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3\"")  // official mint
        // SPL Token Program v1. Consistent with Programs.TOKEN in Core.kt (43 chars, 32 bytes).
        // The previous value was 40 chars — invalid Solana address length.
        // Requires on-chain verification before mainnet use — see Core.kt audit comment.
        buildConfigField("String", "TOKEN_PROGRAM", "\"TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA\"")
        buildConfigField("String", "BUILD_DATE", "\"${System.currentTimeMillis()}\"")
        buildConfigField("String", "WALLET_IDENTITY_URI", "\"https://deproof.app\"")
    }

    val keystorePath = System.getenv("DEPROOF_KEYSTORE_PATH")
    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                keyAlias = System.getenv("DEPROOF_KEY_ALIAS") ?: "deproof-key"
                keyPassword = System.getenv("DEPROOF_KEY_PASSWORD") ?: ""
                storeFile = file(keystorePath)
                storePassword = System.getenv("DEPROOF_KEYSTORE_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("Boolean", "DEBUG_MODE", "false")
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
    }

    // Release signing uses env vars: DEPROOF_KEYSTORE_PATH, DEPROOF_KEYSTORE_PASSWORD,
    // DEPROOF_KEY_ALIAS, DEPROOF_KEY_PASSWORD — omitting them produces an unsigned APK.

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // Exclude duplicates
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/AL2.0"
            excludes += "META-INF/LGPL2.1"
        }
    }

    // Lint options - CI rebuild trigger
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    // Test options
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.activity:activity-ktx:1.8.1")

    // Compose UI
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.foundation:foundation:1.5.4")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.2")

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Mobile Wallet Adapter (TODO: use correct version when available)
    // implementation("com.solanomobile:walletadapterkit:2.0.7")

    // JSON processing (Jackson)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // BouncyCastle (Ed25519 signing)
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Serialization (alternative to Gson) - compatible with Kotlin 1.8.22
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Permissions
    implementation("pub.devrel:easypermissions:3.0.0")

    // Logging (using Android's built-in Log instead of timberkt)
    // implementation("com.github.ajalt.timberkt:timberkt:1.0.2")

    // Optional: Hilt for Dependency Injection
    // implementation("com.google.dagger:hilt-android:2.48")
    // kapt("com.google.dagger:hilt-compiler:2.48")
    // implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // ==============
    // Testing
    // ==============

    // Unit Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.2")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

    // Compose Testing
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // Room Testing
    testImplementation("androidx.room:room-testing:2.8.4")

    // Robolectric (required by data-layer unit tests that use Android APIs on JVM)
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")

    // Instrumented Tests (Android Device Tests)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.2")
    androidTestImplementation("io.mockk:mockk-android:1.13.7")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

tasks.register("printBuildInfo") {
    doLast {
        println("=== Deproof Build Info ===")
        println("Namespace: ${android.namespace}")
        println("Version: ${android.defaultConfig.versionName}")
        println("Min SDK: ${android.defaultConfig.minSdk}")
        println("Target SDK: ${android.defaultConfig.targetSdk}")
        println("Compile SDK: ${android.compileSdk}")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Run before build
tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("printBuildInfo")
    }
}
