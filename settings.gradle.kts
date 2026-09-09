pluginManagement {
    includeBuild("build-logic")

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
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "BodyPlan"
include(":app")
include(":core:designsystem")
include(":core:ui:coordinator")
include(":core:ui:components")
include(":core:domain")
include(":core:data")
include(":core:local")
include(":core:navigation")
include(":core:network")
include(":feature:home:api")
include(":feature:home:impl")
include(":feature:workoutlog:api")
include(":feature:workoutlog:impl")
include(":feature:dietlog:api")
include(":feature:dietlog:impl")
include(":feature:my:api")
include(":feature:my:impl")
