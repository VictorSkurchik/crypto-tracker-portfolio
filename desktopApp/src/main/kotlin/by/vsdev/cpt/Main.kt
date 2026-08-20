package by.vsdev.cpt

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import by.vsdev.cpt.app.shell.CptApp
import by.vsdev.cpt.app.shell.appFeatureModules
import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.database.DesktopDatabaseProvider
import by.vsdev.cpt.core.di.initKoin
import by.vsdev.cpt.core.secrets.DesktopSecretStore
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.dsl.module
import java.io.File
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.Properties
import kotlin.system.exitProcess

/**
 * Same app-data directory convention used by [DesktopSecretStore] and [DesktopDatabaseProvider]:
 * a single well-known directory under the user's home holds all of this app's local state
 * (secrets, database, and now window-state/lock/crash-log files too).
 */
private val appDataDir = File(System.getProperty("user.home"), ".crypto-portfolio-tracker")

private const val DEFAULT_WINDOW_WIDTH_DP = 1200
private const val DEFAULT_WINDOW_HEIGHT_DP = 800

fun main() {
    appDataDir.mkdirs()
    installCrashLogger()

    // Held for the entire process lifetime via this top-level `val` (never closed, never let go
    // out of scope) so the OS-level lock isn't released early; if another instance already holds
    // it, this call prints a message to stderr and exits the process before Koin/DB/UI init.
    @Suppress("UNUSED_VARIABLE")
    val instanceLock = acquireSingleInstanceLockOrExit()

    val desktopPlatformModule =
        module {
            single<SecretStore> { DesktopSecretStore() }
            single<DatabaseProvider> { DesktopDatabaseProvider() }
        }
    initKoin(desktopPlatformModule, appFeatureModules)

    val savedWindowState = loadWindowState()

    application {
        val windowState =
            rememberWindowState(
                size = savedWindowState?.size ?: DpSize(DEFAULT_WINDOW_WIDTH_DP.dp, DEFAULT_WINDOW_HEIGHT_DP.dp),
                position = savedWindowState?.position ?: WindowPosition.PlatformDefault,
            )
        Window(
            onCloseRequest = {
                saveWindowState(windowState)
                exitApplication()
            },
            title = "Crypto Portfolio Tracker",
            state = windowState,
        ) {
            CptApp()
        }
    }
}

/**
 * Installs a process-wide uncaught-exception handler that appends a timestamp and stack trace to
 * a plain local text file, then delegates to whatever default handler was previously installed so
 * normal JVM termination/reporting behavior is preserved. Purely local: no network calls, no
 * third-party crash-reporting SDK.
 */
private fun installCrashLogger() {
    val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
    val crashLogFile = File(appDataDir, "crash_log.txt")
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            appDataDir.mkdirs()
            crashLogFile.appendText(
                "[${Instant.now()}] Uncaught exception on thread '${thread.name}':\n" +
                    "${throwable.stackTraceToString()}\n\n",
            )
        }
        previousHandler?.uncaughtException(thread, throwable)
    }
}

/**
 * Acquires an exclusive file lock guarding against a second concurrent instance of this app
 * running against the same local database/secret store. If the lock is already held, this prints
 * a message to stderr and terminates the process before any Koin/database/UI setup happens.
 */
private fun acquireSingleInstanceLockOrExit(): FileLock {
    val lockFile = File(appDataDir, "instance.lock")
    val channel = FileChannel.open(lockFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
    val lock =
        try {
            channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        }
    if (lock == null) {
        System.err.println(
            "Crypto Portfolio Tracker is already running. Only one instance can run at a time " +
                "to avoid conflicting local database/secret-store access. Exiting.",
        )
        channel.close()
        exitProcess(1)
    }
    return lock
}

private data class SavedWindowState(
    val size: DpSize,
    val position: WindowPosition,
)

private fun windowStateFile(): File = File(appDataDir, "window_state.properties")

/** Reads the previously persisted window bounds, or `null` if there is none or it fails to parse. */
private fun loadWindowState(): SavedWindowState? {
    val file = windowStateFile()
    if (!file.exists()) return null
    return runCatching {
        val properties = Properties().apply { file.inputStream().use { load(it) } }
        val width = requireNotNull(properties.getProperty("width")).toFloat()
        val height = requireNotNull(properties.getProperty("height")).toFloat()
        val x = properties.getProperty("x")?.toFloat()
        val y = properties.getProperty("y")?.toFloat()
        SavedWindowState(
            size = DpSize(width.dp, height.dp),
            position = if (x != null && y != null) WindowPosition.Absolute(x.dp, y.dp) else WindowPosition.PlatformDefault,
        )
    }.getOrNull()
}

/** Persists the current window bounds so the next launch can restore them via [loadWindowState]. */
private fun saveWindowState(state: WindowState) {
    runCatching {
        val properties = Properties()
        properties.setProperty(
            "width",
            state.size.width.value
                .toString(),
        )
        properties.setProperty(
            "height",
            state.size.height.value
                .toString(),
        )
        val position = state.position
        if (position is WindowPosition.Absolute) {
            properties.setProperty("x", position.x.value.toString())
            properties.setProperty("y", position.y.value.toString())
        }
        appDataDir.mkdirs()
        windowStateFile().outputStream().use { properties.store(it, null) }
    }
}
