// `apply false` here only resolves each plugin's version for the whole build; actual module setup
// comes from the `cpt.*` convention plugins (build-logic/convention), which apply these by id.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Parses the same version.xcconfig that iosApp/Configuration/Config.xcconfig `#include`s, so
// Android/Desktop read the exact values iOS does.
private val versionProps =
    file("version.xcconfig").readLines()
        .mapNotNull { line ->
            val trimmed = line.substringBefore("//").trim()
            if (trimmed.isEmpty() || !trimmed.contains("=")) {
                null
            } else {
                val (key, value) = trimmed.split("=", limit = 2)
                key.trim() to value.trim()
            }
        }
        .toMap()

extra["cptVersionName"] = versionProps.getValue("MARKETING_VERSION")
extra["cptVersionCode"] = versionProps.getValue("CURRENT_PROJECT_VERSION").toInt()
