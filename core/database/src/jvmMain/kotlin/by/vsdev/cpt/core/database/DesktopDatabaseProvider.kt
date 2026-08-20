package by.vsdev.cpt.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * Wallet addresses and balances are real financial data, so this locks the containing directory
 * to owner-only — on POSIX filesystems that alone blocks other local accounts from opening the
 * `.db` file or any `-wal`/`-shm` sidecar SQLite creates, without having to chase each one.
 */
class DesktopDatabaseProvider(
    appDataDir: File = File(System.getProperty("user.home"), ".crypto-portfolio-tracker"),
) : DatabaseProvider {
    private val databaseFile = File(appDataDir, DATABASE_FILE_NAME)

    @Suppress("InjectDispatcher")
    private val instance: AppDatabase by lazy {
        appDataDir.mkdirs()
        restrictToOwner(appDataDir.toPath())
        val database =
            Room
                .databaseBuilder<AppDatabase>(name = databaseFile.absolutePath)
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        if (databaseFile.exists()) restrictToOwner(databaseFile.toPath())
        database
    }

    override fun database(): AppDatabase = instance

    private fun restrictToOwner(path: Path) {
        if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
            val permissions = if (Files.isDirectory(path)) OWNER_ONLY_DIR_PERMISSIONS else OWNER_ONLY_FILE_PERMISSIONS
            Files.setPosixFilePermissions(path, permissions)
        } else {
            // Non-POSIX filesystem, e.g. Windows.
            val file = path.toFile()
            file.setReadable(false, false)
            file.setReadable(true, true)
            file.setWritable(false, false)
            file.setWritable(true, true)
            if (file.isDirectory) {
                file.setExecutable(false, false)
                file.setExecutable(true, true)
            }
        }
    }

    private companion object {
        val OWNER_ONLY_FILE_PERMISSIONS: Set<PosixFilePermission> =
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        val OWNER_ONLY_DIR_PERMISSIONS: Set<PosixFilePermission> =
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
    }
}
