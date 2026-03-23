pluginManagement {
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Google API Client and related artifacts (google-api-client-android,
        // google-http-client-gson, google-oauth-client) resolve from Maven Central.
        // No additional repos are needed beyond google() + mavenCentral().
    }
}

rootProject.name = "ExpenseTracker"
include(":app")
