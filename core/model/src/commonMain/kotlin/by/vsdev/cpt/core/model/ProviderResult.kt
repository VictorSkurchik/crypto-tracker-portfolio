package by.vsdev.cpt.core.model

sealed class ProviderResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ProviderResult<T>()

    data class Failure(
        val error: ProviderError,
    ) : ProviderResult<Nothing>()
}

inline fun <T, R> ProviderResult<T>.map(transform: (T) -> R): ProviderResult<R> =
    when (this) {
        is ProviderResult.Success -> ProviderResult.Success(transform(value))
        is ProviderResult.Failure -> this
    }

sealed class ProviderError {
    abstract val message: String

    data class AuthenticationFailed(
        override val message: String,
    ) : ProviderError()

    data class RateLimited(
        override val message: String,
    ) : ProviderError()

    data class Timeout(
        override val message: String,
    ) : ProviderError()

    data class Unavailable(
        override val message: String,
    ) : ProviderError()

    data class UnexpectedResponse(
        override val message: String,
    ) : ProviderError()
}
