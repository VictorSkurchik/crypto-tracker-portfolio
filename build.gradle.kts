// Every module's actual setup comes from a `cpt.*` convention plugin (see build-logic/convention).
// The block below doesn't configure anything itself — `apply false` here is what lets those
// convention plugins later `id("com.android.application")` etc. with no version of their own:
// this is where each plugin's version (from the catalog) gets resolved for the whole build.
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

// `version.xcconfig` at the repo root is the single source of truth for the app's version,
// shared with iosApp/Configuration/Config.xcconfig via `#include`. Parsed here (plain
// `KEY = value` lines, xcconfig syntax) so Android/Desktop read the exact same values.
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
