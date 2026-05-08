plugins {
    kotlin("android")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    implementation(project(":shared"))

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // Koin
    implementation("io.insert-koin:koin-android:3.5.0")
    implementation("io.insert-koin:koin-androidx-compose:3.5.0")

    // Glance Widgets
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("androidx.glance:glance-material3:1.0.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // SQLDelight Android Driver
    implementation("app.cash.sqldelight:android-driver:2.0.1")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

android {
    namespace = "com.evertask.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.evertask.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
            ?: findProperty("RELEASE_KEYSTORE_PATH") as? String
        val releaseKeystorePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            ?: findProperty("RELEASE_KEYSTORE_PASSWORD") as? String
        val releaseKeyAlias = System.getenv("RELEASE_KEY_ALIAS")
            ?: findProperty("RELEASE_KEY_ALIAS") as? String
        val releaseKeyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            ?: findProperty("RELEASE_KEY_PASSWORD") as? String

        if (releaseKeystorePath != null && releaseKeystorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        java.setSrcDirs(listOf("src/androidMain/kotlin"))
        kotlin.setSrcDirs(listOf("src/androidMain/kotlin"))
        res.setSrcDirs(listOf("src/androidMain/res"))
        assets.setSrcDirs(listOf("src/androidMain/assets"))
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}
