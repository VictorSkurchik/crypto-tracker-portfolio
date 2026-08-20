package by.vsdev.cpt.core.secrets

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

/**
 * Keychain-backed secret storage. Queries are built as plain NSMutableDictionary and converted to
 * CFDictionaryRef only at the Security-framework call site via CFBridgingRetain/Release — a direct
 * `as CFDictionaryRef` cast is rejected by the compiler since Kotlin/Native treats Foundation and
 * CoreFoundation types as distinct despite Objective-C's toll-free bridging.
 */
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecretStore : SecretStore {
    private val service = "by.vsdev.cpt.secrets"

    private val secClass = CFBridgingRelease(kSecClass) as String
    private val secAttrService = CFBridgingRelease(kSecAttrService) as String
    private val secAttrAccount = CFBridgingRelease(kSecAttrAccount) as String
    private val secValueData = CFBridgingRelease(kSecValueData) as String
    private val secReturnData = CFBridgingRelease(kSecReturnData) as String
    private val secMatchLimit = CFBridgingRelease(kSecMatchLimit) as String
    private val secClassGenericPassword = CFBridgingRelease(kSecClassGenericPassword) as String
    private val secMatchLimitOne = CFBridgingRelease(kSecMatchLimitOne) as String
    private val secAttrAccessible = CFBridgingRelease(kSecAttrAccessible) as String
    private val secAttrAccessibleWhenUnlockedThisDeviceOnly =
        CFBridgingRelease(kSecAttrAccessibleWhenUnlockedThisDeviceOnly) as String

    override suspend fun store(
        key: String,
        value: String,
    ) {
        remove(key)
        val query = baseQuery(key)
        query.setObject(value.toNSData(), forKey = secValueData.ns())
        // Pinned to this device only, so secrets never travel via an iCloud/device backup.
        query.setObject(secAttrAccessibleWhenUnlockedThisDeviceOnly, forKey = secAttrAccessible.ns())
        val status = query.asCFDictionary { ref -> SecItemAdd(ref, null) }
        check(status == errSecSuccess) { "Keychain write failed for key \"$key\" with OSStatus $status" }
    }

    override suspend fun retrieve(key: String): String? {
        val query = baseQuery(key)
        query.setObject(true, forKey = secReturnData.ns())
        query.setObject(secMatchLimitOne, forKey = secMatchLimit.ns())
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = query.asCFDictionary { ref -> SecItemCopyMatching(ref, result.ptr) }
            if (status != errSecSuccess) return@memScoped null
            val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
            data.toKString()
        }
    }

    override suspend fun remove(key: String) {
        val status = baseQuery(key).asCFDictionary { ref -> SecItemDelete(ref) }
        // errSecItemNotFound (e.g. store()'s delete-before-add on first write) isn't a failure.
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "Keychain delete failed for key \"$key\" with OSStatus $status"
        }
    }

    private fun baseQuery(key: String): NSMutableDictionary {
        val dict = NSMutableDictionary()
        dict.setObject(secClassGenericPassword, forKey = secClass.ns())
        dict.setObject(service, forKey = secAttrService.ns())
        dict.setObject(key, forKey = secAttrAccount.ns())
        return dict
    }

    private fun String.ns(): NSString = this as NSString

    private inline fun <T> NSMutableDictionary.asCFDictionary(block: (CFDictionaryRef) -> T): T {
        val ref = CFBridgingRetain(this) as CFDictionaryRef
        try {
            return block(ref)
        } finally {
            CFBridgingRelease(ref)
        }
    }

    private fun String.toNSData(): NSData {
        val bytes = encodeToByteArray()
        if (bytes.isEmpty()) return NSData()
        return bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
        }
    }

    private fun NSData.toKString(): String {
        val size = length.toInt()
        if (size == 0) return ""
        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, size.toULong())
        }
        return bytes.decodeToString()
    }
}
