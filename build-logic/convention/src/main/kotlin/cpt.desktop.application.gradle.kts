plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("cpt.quality")
}

val libs = catalogLibs

kotlin {
    jvmToolchain(libs.intVersion("jdk"))
}
