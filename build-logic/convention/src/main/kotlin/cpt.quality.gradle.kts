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
}
