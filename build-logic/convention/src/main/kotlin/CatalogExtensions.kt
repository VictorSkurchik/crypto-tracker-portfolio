import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/**
 * Precompiled script plugins in this project can't use the generated type-safe `libs` accessor
 * (that's only available in regular build.gradle.kts files, not in the .gradle.kts sources that
 * make up the convention plugins themselves) — so every convention plugin reads the catalog
 * through this instead.
 */
val Project.catalogLibs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

fun VersionCatalog.intVersion(alias: String): Int = version(alias).toInt()

fun VersionCatalog.library(alias: String): Provider<org.gradle.api.artifacts.MinimalExternalModuleDependency> =
    findLibrary(alias).get()

fun VersionCatalog.plugin(alias: String): Provider<org.gradle.plugin.use.PluginDependency> =
    findPlugin(alias).get()
