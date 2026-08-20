package by.vsdev.cpt.core.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

class AndroidDatabaseProvider(
    private val context: Context,
) : DatabaseProvider {
    private val instance: AppDatabase by lazy {
        Room
            .databaseBuilder<AppDatabase>(
                context = context.applicationContext,
                name = context.applicationContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
            ).setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    override fun database(): AppDatabase = instance
}
