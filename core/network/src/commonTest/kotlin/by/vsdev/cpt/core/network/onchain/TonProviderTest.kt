package by.vsdev.cpt.core.network.onchain

import by.vsdev.cpt.core.model.ChainId
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TonProviderTest {
    private fun clientDispatching(responses: Map<String, String>): HttpClient {
        val engine =
            MockEngine { request ->
                val body =
                    responses[request.url.encodedPath]
                        ?: error("no mock response for ${request.url.encodedPath}")
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

    private fun clientRespondingWith(status: HttpStatusCode): HttpClient {
        val engine = MockEngine { respond(content = "", status = status) }
        return HttpClient(engine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `converts nanotons to TON and resolves jetton symbol and decimals via the masters lookup`() =
        runTest {
            val provider =
                TonProvider(
                    clientDispatching(
                        mapOf(
                            "/api/v3/accountStates" to """{"accounts":[{"balance":"2500000000"}]}""",
                            "/api/v3/jetton/wallets" to
                                """{"jetton_wallets":[{"balance":"5000000","jetton":"master-address"}]}""",
                            "/api/v3/jetton/masters" to
                                """
                                {"jetton_masters":[{"address":"master-address",
                                    "jetton_content":{"symbol":"USDT","decimals":"6"}}]}
                                """.trimIndent(),
                        ),
                    ),
                )

            val result = provider.fetchBalances(ChainId.TON, "ton-address")

            check(result is ProviderResult.Success)
            val bySymbol = result.value.associateBy { it.assetSymbol }
            assertEquals(2, bySymbol.size)
            assertEquals(2.5, bySymbol.getValue("TON").quantity)
            assertEquals(5.0, bySymbol.getValue("USDT").quantity)
        }

    @Test
    fun `a jetton wallet whose master never resolves is dropped rather than shown unlabeled`() =
        runTest {
            val provider =
                TonProvider(
                    clientDispatching(
                        mapOf(
                            "/api/v3/accountStates" to """{"accounts":[{"balance":"0"}]}""",
                            "/api/v3/jetton/wallets" to
                                """{"jetton_wallets":[{"balance":"5000000","jetton":"unknown-master"}]}""",
                            "/api/v3/jetton/masters" to """{"jetton_masters":[]}""",
                        ),
                    ),
                )

            val result = provider.fetchBalances(ChainId.TON, "ton-address")

            check(result is ProviderResult.Success)
            assertTrue(result.value.isEmpty())
        }

    @Test
    fun `no jetton wallets skips the masters lookup entirely`() =
        runTest {
            val provider =
                TonProvider(
                    clientDispatching(
                        mapOf(
                            "/api/v3/accountStates" to """{"accounts":[{"balance":"1000000000"}]}""",
                            "/api/v3/jetton/wallets" to """{"jetton_wallets":[]}""",
                        ),
                    ),
                )

            val result = provider.fetchBalances(ChainId.TON, "ton-address")

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("TON", result.value.single().assetSymbol)
        }

    @Test
    fun `a non-2xx response is reported as a failure rather than a silent zero balance`() =
        runTest {
            val provider = TonProvider(clientRespondingWith(HttpStatusCode.InternalServerError))

            val result = provider.fetchBalances(ChainId.TON, "ton-address")

            val failure = assertIs<ProviderResult.Failure>(result)
            assertIs<ProviderError.Unavailable>(failure.error)
        }

    @Test
    fun `a rate-limited response maps to ProviderError RateLimited`() =
        runTest {
            val provider = TonProvider(clientRespondingWith(HttpStatusCode.TooManyRequests))

            val result = provider.fetchBalances(ChainId.TON, "ton-address")

            val failure = assertIs<ProviderResult.Failure>(result)
            assertIs<ProviderError.RateLimited>(failure.error)
        }
}
