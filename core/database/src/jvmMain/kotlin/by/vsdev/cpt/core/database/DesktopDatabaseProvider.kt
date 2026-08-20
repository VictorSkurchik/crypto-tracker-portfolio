package by.vsdev.cpt.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

class DesktopDatabaseProvider(
    appDataDir: File = File(System.getProperty("user.home"), ".crypto-portfolio-tracker"),
) : DatabaseProvider {
    @Suppress("InjectDispatcher")
    private val instance: AppDatabase by lazy {
        appDataDir.mkdirs()
        Room
            .databaseBuilder<AppDatabase>(name = File(appDataDir, DATABASE_FILE_NAME).absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    override fun database(): AppDatabase = instance
}
