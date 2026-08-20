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

class BybitConnectorTest {
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
    fun `maps unified wallet balances dropping zero-quantity coins`() =
        runTest {
            val body =
                """
                {"retCode":0,"retMsg":"OK","result":{"list":[{"accountType":"UNIFIED","coin":[
                    {"coin":"USDT","walletBalance":"250.5"},
                    {"coin":"DOGE","walletBalance":"0"}
                ]}]}}
                """.trimIndent()
            val connector = BybitConnector(clientRespondingWith(HttpStatusCode.OK, body))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("USDT", result.value.single().assetSymbol)
            assertEquals(250.5, result.value.single().quantity)
        }

    @Test
    fun `a non-zero Bybit retCode surfaces as UnexpectedResponse not a crash`() =
        runTest {
            val connector = BybitConnector(clientRespondingWith(HttpStatusCode.OK, """{"retCode":10001,"retMsg":"bad request"}"""))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }

    @Test
    fun `maps 401 to AuthenticationFailed`() =
        runTest {
            val connector = BybitConnector(clientRespondingWith(HttpStatusCode.Unauthorized, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.AuthenticationFailed)
        }

    @Test
    fun `rejects the wrong credential type instead of crashing`() =
        runTest {
            val connector = BybitConnector(clientRespondingWith(HttpStatusCode.OK, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecretPassphrase("key", "secret", "passphrase"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }
}
