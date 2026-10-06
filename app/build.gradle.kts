// build.gradle.kts - Deproof App Module
// Complete configuration for P1 MVP

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    // Optional: Hilt for DI
    // id("com.google.dagger.hilt.android")
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

        // BuildConfig fields (default - production)
        buildConfigField("String", "API_ENDPOINT", "\"https://api.mainnet-beta.solana.com\"")
        buildConfigField("String", "SKR_MINT", "\"SKRbvo6Gf7GoNcKKqqyckfjxN2PEVEqJf3rUKdPbdYu\"")
        buildConfigField("String", "TOKEN_PROGRAM", "\"TokenkegQfeZyiNwAJsyFbPVwwQkYk5LWV2BXVBq\"")
        buildConfigField("String", "BUILD_DATE", "\"${System.currentTimeMillis()}\"")
        buildConfigField("String", "ENVIRONMENT", "\"production\"")
    }

    flavorDimensions += "environment"

    productFlavors {
        create("production") {
            dimension = "environment"
            applicationIdSuffix = ""
            versionNameSuffix = ""

            buildConfigField("String", "API_ENDPOINT", "\"https://api.mainnet-beta.solana.com\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
        }

        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"

            buildConfigField("String", "API_ENDPOINT", "\"https://api.devnet.solana.com\"")
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("Boolean", "DEBUG_MODE", "false")

            // Apply signing configuration if available
            signingConfig = signingConfigs.findByName("release")
        }

        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "DEBUG_MODE", "true")
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("DEPROOF_KEYSTORE_PATH") ?: ""
            val keystorePassword = System.getenv("DEPROOF_KEYSTORE_PASSWORD") ?: ""
            val keyAlias = System.getenv("DEPROOF_KEY_ALIAS") ?: ""
            val keyPassword = System.getenv("DEPROOF_KEY_PASSWORD") ?: ""

            if (keystorePath.isNotEmpty() && keystorePassword.isNotEmpty() &&
                keyAlias.isNotEmpty() && keyPassword.isNotEmpty()) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            } else if (keystorePath.isNotEmpty()) {
                // Partial configuration provided - log warning
                println("⚠️  WARNING: Incomplete signing configuration detected.")
                println("Set all of: DEPROOF_KEYSTORE_PATH, DEPROOF_KEYSTORE_PASSWORD, DEPROOF_KEY_ALIAS, DEPROOF_KEY_PASSWORD")
                println("Release builds will not be signed without complete configuration.")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-Xopt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
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

    // Lint options
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
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("androidx.room:room-ktx:2.6.0")
    kapt("androidx.room:room-compiler:2.6.0")

    // DataStore (Preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Mobile Wallet Adapter (TODO: use correct version when available)
    // implementation("com.solanomobile:walletadapterkit:2.0.7")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Serialization (alternative to Gson)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Annotations for Null Safety
    implementation("androidx.annotation:annotation:1.7.1")

    // Permissions
    implementation("pub.devrel:easypermissions:3.0.0")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")

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
    testImplementation("org.mockito:mockito-core:5.5.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.11.0")

    // Compose Testing
    testImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // Room Testing
    testImplementation("androidx.room:room-testing:2.6.0")

    // Instrumented Tests (Android Device Tests)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")

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

// Run before build
tasks.whenTaskAdded {
    if (name == "preBuild") {
        dependsOn("printBuildInfo")
    }
}
