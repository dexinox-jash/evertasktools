//
// Project-level build.gradle.kts for Ever Task Tools
// Android - Kotlin/Jetpack Compose
//

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" apply false
    id("com.google.devtools.ksp") version "1.9.21-1.0.15" apply false
}

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
    }
}

// Task to clean build directories
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

// Version constants for the project
object Versions {
    const val COMPILE_SDK = 34
    const val TARGET_SDK = 34
    const val MIN_SDK = 26
    const val VERSION_CODE = 1
    const val VERSION_NAME = "1.0.0"
}
