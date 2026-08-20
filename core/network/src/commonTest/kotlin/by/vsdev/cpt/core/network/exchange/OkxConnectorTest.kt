package by.vsdev.cpt.core.network.exchange

import by.vsdev.cpt.core.model.ExchangeCredentials
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

class OkxConnectorTest {
    private fun clientRespondingWith(body: String): HttpClient {
        val engine =
            MockEngine { _ ->
                respond(
                    content = body,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `maps trading-account balances preferring eq over cashBal`() =
        runTest {
            val body =
                """
                {"code":"0","data":[{"details":[
                    {"ccy":"USDT","cashBal":"100","eq":"105"},
                    {"ccy":"DOGE","cashBal":"0","eq":"0"}
                ]}]}
                """.trimIndent()
            val connector = OkxConnector(clientRespondingWith(body))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecretPassphrase("key", "secret", "phrase"))

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("USDT", result.value.single().assetSymbol)
            assertEquals(105.0, result.value.single().quantity)
        }

    @Test
    fun `a non-zero OKX error code surfaces as UnexpectedResponse not a crash`() =
        runTest {
            val connector = OkxConnector(clientRespondingWith("""{"code":"50001","msg":"System error"}"""))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecretPassphrase("key", "secret", "phrase"))

            assertTrue(result is ProviderResult.Failure)
        }

    @Test
    fun `requires a passphrase rejecting a plain ApiKeySecret`() =
        runTest {
            val connector = OkxConnector(clientRespondingWith("{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            assertTrue(result is ProviderResult.Failure)
        }
}
