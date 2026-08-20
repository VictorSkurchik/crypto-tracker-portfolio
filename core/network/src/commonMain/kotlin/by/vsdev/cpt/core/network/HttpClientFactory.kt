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
 * Wraps [Logger.DEFAULT], redacting sensitive query-param values before the line is emitted.
 * [LogLevel.INFO] logs the full URL but never headers, so only Etherscan's `apikey=` and
 * Binance's `signature=` (both mandated by those APIs, no header alternative) would otherwise
 * leak — every other connector keeps secrets in headers only.
 */
private object RedactingLogger : Logger {
    override fun log(message: String) {
        Logger.DEFAULT.log(redact(message))
    }

    private fun redact(message: String): String =
        SENSITIVE_QUERY_PARAM_REGEX.replace(message) { match -> "${match.groupValues[1]}=REDACTED" }

    // Broader than today's two actual params (apikey/signature) as a guard against future leaks.
    private val SENSITIVE_QUERY_PARAM_REGEX =
        Regex("""(?i)\b(apikey|api_key|signature|sign|secret|passphrase)=[^&\s"']+""")
}

private const val REQUEST_TIMEOUT_MILLIS = 15_000L
private const val CONNECT_TIMEOUT_MILLIS = 10_000L
