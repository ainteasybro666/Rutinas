pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "1.9.24"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

tasks.register("clean", Delete::class) {
    Files.delete(rootProject.buildDir)
}

rootProject.name = "RutinasApp"
include(":app")

kotlin {
    jvmToolchain(8)
}