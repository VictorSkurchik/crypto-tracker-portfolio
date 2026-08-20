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

class BitgetConnectorTest {
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

    private fun credentials() = ExchangeCredentials.ApiKeySecretPassphrase("key", "secret", "passphrase")

    @Test
    fun `sums available frozen and locked into a single uppercased balance`() =
        runTest {
            val body =
                """
                {"code":"00000","data":[
                    {"coin":"usdt","available":"10","frozen":"2","locked":"3"},
                    {"coin":"doge","available":"0","frozen":"0","locked":"0"}
                ]}
                """.trimIndent()
            val connector = BitgetConnector(clientRespondingWith(HttpStatusCode.OK, body))

            val result = connector.fetchBalances(credentials())

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("USDT", result.value.single().assetSymbol)
            assertEquals(15.0, result.value.single().quantity)
        }

    @Test
    fun `a non-success Bitget code surfaces as UnexpectedResponse not a crash`() =
        runTest {
            val connector = BitgetConnector(clientRespondingWith(HttpStatusCode.OK, """{"code":"40001","msg":"bad sign"}"""))

            val result = connector.fetchBalances(credentials())

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }

    @Test
    fun `maps 403 to AuthenticationFailed`() =
        runTest {
            val connector = BitgetConnector(clientRespondingWith(HttpStatusCode.Forbidden, "{}"))

            val result = connector.fetchBalances(credentials())

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.AuthenticationFailed)
        }

    @Test
    fun `rejects the wrong credential type instead of crashing`() =
        runTest {
            val connector = BitgetConnector(clientRespondingWith(HttpStatusCode.OK, "{}"))

            val result = connector.fetchBalances(ExchangeCredentials.ApiKeySecret("key", "secret"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }
}
