rootProject.name = "crypto-portfolio-tracker"

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":core:model")
include(":core:common")
include(":core:network")
include(":core:secrets")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":core:designsystem")
include(":core:navigation")
include(":core:di")

include(":feature:portfolio")
include(":feature:wallets")
include(":feature:exchanges")
include(":feature:customassets")
include(":feature:settings")

include(":app:shell")
include(":androidApp")
include(":desktopApp")
