package by.vsdev.cpt.core.network

import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

/**
 * Maps an [Exception] caught around an HTTP call to the right [ProviderError].
 *
 * Every connector/provider in this module wraps its call in a generic `catch (e: Exception)` as a
 * last resort, but a real network timeout was previously bucketed under [ProviderError.Unavailable]
 * indistinguishably from e.g. a 5xx response or a malformed body, even though [ProviderError.Timeout]
 * exists specifically to let callers retry/back off differently for "the network was slow" versus
 * "the server/response was actually broken". Ktor 3.5.1 surfaces a timeout as one of three distinct
 * exception types depending on which phase failed -- request, connect, or socket read/write -- so
 * all three are checked here.
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
