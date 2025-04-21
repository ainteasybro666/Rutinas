plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.navigation.safeargs)
    id("org.jetbrains.kotlin.plugin.parcelize")
    //alias(libs.plugins.kotlin.parcelize)

}

android {
    namespace = "com.example.rutinas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.rutinas"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    kotlin {
        jvmToolchain(17)
    }
    kotlinOptions {
        jvmTarget = "17"

    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
        //dataBinding = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Timber?
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // UI
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Gson
    implementation(libs.gson)

    // Room
    implementation(libs.androidx.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui.tooling.android)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.recyclerview)

    //Navigation
    implementation(libs.androidx.navigation.fragment.ktx)

    // Material 3
    implementation(platform(libs.androidx.compose.bom))

    // Si usas Compose
    implementation(libs.hilt.android.testing)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.timber)


    implementation(libs.androidx.recyclerview) {
        attributes {
            attribute(
                Attribute.of("org.gradle.jvm.version", String::class.java),
                "17"
            )
        }
    }
}

/*
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.recyclerview" && requested.name == "recyclerview") {
            useVersion("1.3.2") // O la versión específica que necesites
            because("Forzar versión compatible con Kotlin 1.9.22")
        }
    }
}*/
