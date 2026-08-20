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

// The plain `detekt` task detekt's own plugin registers is wired for a classic src/main/kotlin
// layout and reports NO-SOURCE for every KMP module — the real per-source-set analysis happens
// in tasks like `detektMetadataCommonMain`/`detektJvmMain`/`detektIosArm64Main`, which aren't
// otherwise attached to `check`. Wire every one of them in, so `./gradlew check`/`build` actually
// runs detekt instead of silently skipping it.
tasks.named("check") {
    dependsOn(
        tasks.matching { task ->
            task.name.startsWith("detekt") &&
                (task.name.endsWith("Main") || task.name.endsWith("Test")) &&
                !task.name.contains("Baseline")
        },
    )
}
