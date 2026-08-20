package by.vsdev.cpt.core.secrets

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.Properties
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Desktop has no universal OS-keyring API from pure JVM, so this is the weakest of the three
 * platform implementations: values are AES-256-GCM encrypted with a key generated on first run
 * and stored alongside the ciphertext in a file under the user's home directory.
 *
 * Residual risk (read before assuming this is "fixed"): the encryption key (`secrets.key`) and
 * the ciphertext (`secrets.properties`) both live on disk in the same directory, protected only
 * by OS file/directory permissions. This class now enforces and *re-verifies* owner-only
 * permissions on the directory and both files on every access rather than only setting them once
 * and hoping — a widened permission is treated as tampering and fails loudly instead of silently
 * decrypting anyway. That raises the bar against other unprivileged local accounts, but it is a
 * ceiling, not a real fix: any process running as the *same* OS user (malware, another app, a
 * co-worker with a shell on a shared machine) can still read both files and decrypt everything,
 * because the key itself is stored in directly-usable form.
 *
 * The actual fix is to stop persisting the DEK in directly-usable form at all: derive/wrap it from
 * a user-entered master passphrase via `PBKDF2WithHmacSHA256` (already built into the JDK's
 * `SecretKeyFactory` — no new dependency needed) so only someone who knows the passphrase can
 * unwrap it. That is a real product/UX change (a passphrase prompt + unlock flow on app start,
 * wired through desktopApp's DI, which constructs this class with a no-arg call today) that needs
 * product sign-off and touches modules outside `core/secrets`, so it is intentionally not done
 * here. True OS-keyring parity (macOS Keychain / Windows DPAPI / Linux libsecret) would need
 * native bindings that can't be built and tested cross-platform in this environment.
 */
@OptIn(ExperimentalEncodingApi::class)
class DesktopSecretStore(
    appDataDir: File = File(System.getProperty("user.home"), ".crypto-portfolio-tracker"),
) : SecretStore {
    private val storeFile = File(appDataDir, "secrets.properties")
    private val keyFile = File(appDataDir, "secrets.key")

    init {
        appDataDir.mkdirs()
        val dirPath = appDataDir.toPath()
        restrictToOwner(dirPath)
        verifyOwnerOnly(dirPath)
    }

    private val secretKey: SecretKeySpec by lazy {
        if (!keyFile.exists()) {
            val bytes = ByteArray(AES_256_KEY_BYTES)
            SecureRandom().nextBytes(bytes)
            keyFile.writeBytes(bytes)
        }
        val keyPath = keyFile.toPath()
        restrictToOwner(keyPath)
        verifyOwnerOnly(keyPath)
        SecretKeySpec(keyFile.readBytes(), "AES")
    }

    override suspend fun store(
        key: String,
        value: String,
    ) {
        val properties = loadProperties()
        properties.setProperty(key, encrypt(value))
        saveProperties(properties)
    }

    override suspend fun retrieve(key: String): String? {
        val encrypted = loadProperties().getProperty(key) ?: return null
        return decrypt(encrypted)
    }

    override suspend fun remove(key: String) {
        val properties = loadProperties()
        properties.remove(key)
        saveProperties(properties)
    }

    private fun loadProperties(): Properties =
        Properties().apply {
            if (storeFile.exists()) {
                val storePath = storeFile.toPath()
                verifyOwnerOnly(storePath)
                storeFile.inputStream().use { load(it) }
            }
        }

    private fun saveProperties(properties: Properties) {
        storeFile.outputStream().use { properties.store(it, null) }
        val storePath = storeFile.toPath()
        restrictToOwner(storePath)
        verifyOwnerOnly(storePath)
    }

    private fun encrypt(plaintext: String): String {
        val iv = ByteArray(GCM_IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encode(iv) + ":" + Base64.encode(ciphertext)
    }

    private fun decrypt(encoded: String): String? {
        val parts = encoded.split(":", limit = 2)
        if (parts.size != 2) return null
        val iv = Base64.decode(parts[0])
        val ciphertext = Base64.decode(parts[1])
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    /**
     * Restricts [path] (a file or directory) to owner-only access. Prefers the POSIX permission
     * API — it sets the exact permission bitset in one atomic call, unlike the legacy
     * [File.setReadable]/[File.setWritable]/[File.setExecutable] calls (used as a fallback here
     * for non-POSIX filesystems, e.g. Windows) which are separate per-bit operations whose
     * success/failure was previously discarded via a blanket `runCatching {}`.
     */
    private fun restrictToOwner(path: Path) {
        if (supportsPosixPermissions(path)) {
            val permissions = if (Files.isDirectory(path)) OWNER_ONLY_DIR_PERMISSIONS else OWNER_ONLY_FILE_PERMISSIONS
            Files.setPosixFilePermissions(path, permissions)
        } else {
            val file = path.toFile()
            check(file.setReadable(false, false) && file.setReadable(true, true)) {
                "Failed to restrict read access to ${file.name}"
            }
            check(file.setWritable(false, false) && file.setWritable(true, true)) {
                "Failed to restrict write access to ${file.name}"
            }
            if (file.isDirectory) {
                check(file.setExecutable(false, false) && file.setExecutable(true, true)) {
                    "Failed to restrict traversal of ${file.name}"
                }
            }
        }
    }

    /**
     * Re-checks that [path] still carries owner-only permissions and fails loudly instead of
     * silently operating on a secret/key file whose permissions were widened after the fact
     * (e.g. by a misconfigured backup tool, a careless `chmod`, or actual tampering).
     */
    private fun verifyOwnerOnly(path: Path) {
        if (!supportsPosixPermissions(path)) return
        val allowed = if (Files.isDirectory(path)) OWNER_ONLY_DIR_PERMISSIONS else OWNER_ONLY_FILE_PERMISSIONS
        val actual = Files.getPosixFilePermissions(path)
        if (!allowed.containsAll(actual)) {
            throw SecurityException(
                "Refusing to use ${path.fileName}: its permissions ($actual) are broader than " +
                    "owner-only ($allowed). This may mean the file was tampered with or exposed to " +
                    "other local accounts; fix its permissions before retrying.",
            )
        }
    }

    private fun supportsPosixPermissions(path: Path): Boolean = path.fileSystem.supportedFileAttributeViews().contains("posix")

    private companion object {
        const val AES_256_KEY_BYTES = 32
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
        val OWNER_ONLY_FILE_PERMISSIONS: Set<PosixFilePermission> =
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)
        val OWNER_ONLY_DIR_PERMISSIONS: Set<PosixFilePermission> =
            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)
    }
}
