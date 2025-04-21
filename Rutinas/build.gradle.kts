// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) version "1.9.22" apply false
    alias(libs.plugins.hilt.android) version "2.51.1" apply false
    alias(libs.plugins.ksp) version "1.9.22-1.0.17" apply false  // KSP para Kotlin 1.9.0
    id("androidx.navigation.safeargs.kotlin") version "2.7.7" apply false

    //classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    //alias(libs.plugins.hilt) version "2.48.1" apply false
}