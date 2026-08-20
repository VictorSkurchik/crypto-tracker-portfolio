package by.vsdev.cpt.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}

/**
 * [io] deliberately uses [Dispatchers.Default] on every platform, not [Dispatchers.IO] — the
 * latter is Android/JVM-only (internal on Kotlin/Native), and Room/Ktor already manage their own
 * threading internally, so a single shared background dispatcher is enough here.
 */
class DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher = Dispatchers.Default
    override val default: CoroutineDispatcher = Dispatchers.Default
}
