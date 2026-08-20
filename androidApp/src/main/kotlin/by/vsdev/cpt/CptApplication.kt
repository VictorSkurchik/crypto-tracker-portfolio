package by.vsdev.cpt

import android.app.Application
import android.content.Context
import by.vsdev.cpt.app.shell.appFeatureModules
import by.vsdev.cpt.core.database.AndroidDatabaseProvider
import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.di.initKoin
import by.vsdev.cpt.core.secrets.AndroidSecretStore
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.dsl.module
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CptApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
        val androidPlatformModule =
            module {
                single<Context> { this@CptApplication }
                single<SecretStore> { AndroidSecretStore(get()) }
                single<DatabaseProvider> { AndroidDatabaseProvider(get()) }
            }
        initKoin(androidPlatformModule, appFeatureModules)
    }

    /** Purely local crash log under [filesDir] — this app has no analytics/telemetry by design. */
    private fun installCrashLogger() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { appendCrashLog(thread, throwable) }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    /** Caps the log at [MAX_CRASH_LOG_BYTES] by dropping prior contents — simple cap, no rotation. */
    private fun appendCrashLog(
        thread: Thread,
        throwable: Throwable,
    ) {
        val logFile = File(filesDir, CRASH_LOG_FILE_NAME)
        if (logFile.length() > MAX_CRASH_LOG_BYTES) {
            logFile.delete()
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())
        val header = "--- $timestamp | thread=${thread.name} ---"
        logFile.appendText("\n$header\n${throwable.stackTraceToString()}\n")
    }

    private companion object {
        const val CRASH_LOG_FILE_NAME = "crash_log.txt"

        // ~1 MB — plenty for a plain-text stack-trace dump, small enough to never be a problem.
        const val MAX_CRASH_LOG_BYTES = 1_000_000L
    }
}
