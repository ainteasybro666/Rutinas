plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    // Ojo, ajustamos la versión para que no sea "2.0.0" sino algo estable
    // y compatible con Kotlin 1.9.21 + Compose 1.5.6.
    // Por eso, en libs.versions.toml también reflejaremos el cambio.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    // Eliminamos id("kotlin-kapt") si no queremos usar kapt,
    // o lo dejamos si hay otras dependencias que sólo usan kapt.
    // Pero si queremos centrarnos en KSP, podemos quitarlo.
    // id("kotlin-kapt")  // -> Se comenta si sólo usas ksp
}

android {
    namespace = "com.example.rutinas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.rutinas"
        minSdk = 33
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // Importante: Aquí nos aseguramos de no usar "2.0" para languageVersion ni apiVersion
        // porque KSP ni Hilt están 100% soportados con Kotlin 2.x
        // Deja que "1.9" sea la base:
        // freeCompilerArgs += ["-Xlanguage-version=1.9", "-Xapi-version=1.9"]
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // Alinear con la versión en libs.versions.toml -> compose-compiler = "1.5.6"
        kotlinCompilerExtensionVersion = "1.5.7"
    }
//hilt {
        //enableAggregatingTask = false
    }
//}

dependencies {

    // Android / core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM y componentes principales
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    // Material 3, íconos, etc.
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended.v176)
    // Si usaras material normal (no M3), lo añades:
    // implementation("androidx.compose.material:material:<versión>")

    // Navegación y Hilt Navigation
    implementation(libs.androidx.navigation.compose)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt principal (Dagger)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Hilt + WorkManager
    // Para que Hilt funcione con WorkManager a través de KSP,
    // necesitamos agregar "androidx.hilt:hilt-compiler" también con ksp:
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Coroutines/Gson/WorkManager
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.gson)
    // Si usas WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    // Nota: ya agregaste "work-runtime-ktx" y "work-runtime"
    // Asegúrate de no duplicar tus libs (ver libs.versions.toml)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}