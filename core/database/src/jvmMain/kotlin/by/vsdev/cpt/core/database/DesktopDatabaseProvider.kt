package by.vsdev.cpt.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

/**
 * The on-device cache holds wallet addresses and account balances -- real financial/privacy data,
 * not just app plumbing -- so on a shared machine it deserves the same owner-only file permissions
 * `DesktopSecretStore` (`core/secrets`) already applies to its own files, rather than being left
 * at whatever default permissions SQLite/the OS umask happens to create its files with. Locking
 * down the containing directory to owner-only is what actually matters here: on POSIX filesystems
 * a directory without group/other execute permission blocks other local accounts from opening
 * *any* file inside it (the main `.db` file plus the `-wal`/`-shm` sidecar files SQLite's WAL mode
 * creates on demand) regardless of those files' own individual permissions, so this doesn't need
 * to chase every sidecar file Room/SQLite might create over the app's lifetime.
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
        // Belt-and-suspenders: also lock the main file itself once Room/SQLite has created it,
        // in addition to the directory-level protection above.
        if (databaseFile.exists()) restrictToOwner(databaseFile.toPath())
        database
    }

    override fun database(): AppDatabase = instance

    private fun restrictToOwner(path: Path) {
        if (path.fileSystem.supportedFileAttributeViews().contains("posix")) {
            val permissions = if (Files.isDirectory(path)) OWNER_ONLY_DIR_PERMISSIONS else OWNER_ONLY_FILE_PERMISSIONS
            Files.setPosixFilePermissions(path, permissions)
        } else {
            // Non-POSIX filesystem (e.g. Windows): best-effort fallback via the legacy File API.
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
