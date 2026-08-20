package by.vsdev.cpt.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosDatabaseProvider : DatabaseProvider {
    private val instance: AppDatabase by lazy {
        val documentsDirectory =
            NSSearchPathForDirectoriesInDomains(
                directory = NSDocumentDirectory,
                domainMask = NSUserDomainMask,
                expandTilde = true,
            ).first() as String
        Room
            .databaseBuilder<AppDatabase>(name = "$documentsDirectory/$DATABASE_FILE_NAME")
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    override fun database(): AppDatabase = instance
}
