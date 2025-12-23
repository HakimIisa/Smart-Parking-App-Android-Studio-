pluginManagement {
    plugins {
        id("com.android.application") version "8.4.0" apply false // or use 8.9.0 if available/stable
        id("com.google.gms.google-services") version "4.4.2" apply false
        id("org.jetbrains.kotlin.android") version "1.9.22" apply false // update Kotlin if needed

    }

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Smart Parking Application"
include(":app")
