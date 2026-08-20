plugins {
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

ktlint {
    filter {
        exclude { entry -> entry.file.path.contains("${File.separator}build${File.separator}") }
    }
}

detekt {
    buildUponDefaultConfig = true
    autoCorrect = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude { entry -> entry.file.path.contains("${File.separator}build${File.separator}") }
}

// The plain `detekt` task reports NO-SOURCE for every KMP module (it expects a classic
// src/main/kotlin layout); the real analysis runs in per-source-set tasks like
// `detektMetadataCommonMain`/`detektJvmMain`, which aren't attached to `check` by default.
tasks.named("check") {
    dependsOn(
        tasks.matching { task ->
            task.name.startsWith("detekt") &&
                (task.name.endsWith("Main") || task.name.endsWith("Test")) &&
                !task.name.contains("Baseline")
        },
    )
}
