package by.vsdev.cpt.core.network.exchange

import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BinanceConnectorTest {
    private fun clientRespondingWith(
        status: HttpStatusCode,
        body: String,
    ): HttpClient {
        val engine =
            MockEngine { _ ->
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `maps balances summing free and locked dropping zero-quantity assets`() =
        runTest {
            val body =
                """
                {"balances":[
                    {"asset":"BTC","free":"0.5","locked":"0.25"},
                    {"asset":"USDT","free":"0","locked":"0"}
                ]}
                """.trimIndent()
            val connector = BinanceConnector(clientRespondingWith(HttpStatusCode.OK, body))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("BTC", result.value.single().assetSymbol)
            assertEquals(0.75, result.value.single().quantity)
        }

    @Test
    fun `maps 401 to AuthenticationFailed`() =
        runTest {
            val connector = BinanceConnector(clientRespondingWith(HttpStatusCode.Unauthorized, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.AuthenticationFailed)
        }

    @Test
    fun `maps 429 to RateLimited`() =
        runTest {
            val connector = BinanceConnector(clientRespondingWith(HttpStatusCode.TooManyRequests, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.RateLimited)
        }

    @Test
    fun `rejects the wrong credential type instead of crashing`() =
        runTest {
            val connector = BinanceConnector(clientRespondingWith(HttpStatusCode.OK, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecretPassphrase("key", "secret", "passphrase"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }
}
