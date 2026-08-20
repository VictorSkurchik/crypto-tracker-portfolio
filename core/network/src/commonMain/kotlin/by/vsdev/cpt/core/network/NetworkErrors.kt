package by.vsdev.cpt.core.network

import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * Maps an [Exception] caught around an HTTP call to the right [ProviderError], distinguishing a
 * timeout (retriable) from an actually-broken response ([ProviderError.Unavailable]). Ktor surfaces
 * a timeout as one of three distinct exception types depending on which phase failed.
 */
fun networkExceptionToFailure(
    exception: Exception,
    fallbackMessage: String,
): ProviderResult.Failure =
    when (exception) {
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException ->
            ProviderResult.Failure(ProviderError.Timeout(exception.message ?: fallbackMessage))
        else -> ProviderResult.Failure(ProviderError.Unavailable(exception.message ?: fallbackMessage))
    }
