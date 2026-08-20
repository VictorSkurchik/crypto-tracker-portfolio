package by.vsdev.cpt.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * One shared client for every provider. No explicit engine is passed — each platform's source set
 * has exactly one Ktor engine artifact on its classpath (OkHttp for android/jvm, Darwin for iOS),
 * so Ktor's engine auto-selection picks the right one.
 */
fun createHttpClient(): HttpClient =
    HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
        install(Logging) {
            level = LogLevel.INFO
            logger = RedactingLogger
        }
        install(HttpTimeout) {
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        }
    }

/**
 * Wraps [Logger.DEFAULT] and redacts known-sensitive query parameter *values* before the line is
 * emitted, rather than disabling request logging outright.
 *
 * At [LogLevel.INFO], Ktor's Logging plugin only logs the method + full URL (and response status)
 * — never headers or bodies — so every exchange connector that puts its API key/secret/signature
 * in a header (OKX, Bybit, Bitget, CoinMarketCap) is unaffected. Two providers put a secret in the
 * URL itself, which *is* logged at this level:
 *  - [by.vsdev.cpt.core.network.onchain.EtherscanV2Provider] sends the API key as `apikey=...`
 *    (mandated by Etherscan's API — there's no header-based alternative).
 *  - [by.vsdev.cpt.core.network.exchange.BinanceConnector] appends the HMAC `signature=...` for
 *    request signing (Binance's API design, not header-based).
 * Every other connector/provider in this module was checked and does not put a secret in the URL.
 */
private object RedactingLogger : Logger {
    override fun log(message: String) {
        Logger.DEFAULT.log(redact(message))
    }

    private fun redact(message: String): String =
        SENSITIVE_QUERY_PARAM_REGEX.replace(message) { match -> "${match.groupValues[1]}=REDACTED" }

    // Case-insensitive and covers a couple of plausible future param spellings in addition to the
    // two that are actually in use today (`apikey`, `signature`), since a missed name here would
    // silently leak a credential into logs again.
    private val SENSITIVE_QUERY_PARAM_REGEX =
        Regex("""(?i)\b(apikey|api_key|signature|sign|secret|passphrase)=[^&\s"']+""")
}

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L
