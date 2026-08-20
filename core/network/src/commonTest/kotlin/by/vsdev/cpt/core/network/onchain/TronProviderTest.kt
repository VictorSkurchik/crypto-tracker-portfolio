package by.vsdev.cpt.core.network.onchain

import by.vsdev.cpt.core.model.ChainId
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

class TronProviderTest {
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
    fun `converts sun to TRX and recognizes known TRC20 contracts`() =
        runTest {
            val body =
                """
                {"data":[{
                    "balance":5000000,
                    "trc20":[{"TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t":"2500000"}]
                }]}
                """.trimIndent()
            val provider = TronProvider(clientRespondingWith(body))

            val result = provider.fetchBalances(ChainId.TRON, "T-address")

            check(result is ProviderResult.Success)
            val bySymbol = result.value.associateBy { it.assetSymbol }
            assertEquals(2, bySymbol.size)
            assertEquals(5.0, bySymbol.getValue("TRX").quantity)
            assertEquals(2.5, bySymbol.getValue("USDT").quantity)
        }

    @Test
    fun `a wallet with no balance field at all yields no TRX entry`() =
        runTest {
            val provider = TronProvider(clientRespondingWith("""{"data":[{"trc20":[]}]}"""))

            val result = provider.fetchBalances(ChainId.TRON, "T-address")

            check(result is ProviderResult.Success)
            assertTrue(result.value.isEmpty())
        }

    @Test
    fun `an unrecognized TRC20 contract is skipped rather than shown as a raw address`() =
        runTest {
            val body = """{"data":[{"trc20":[{"TUnknownContractAddress00000000000":"1000000"}]}]}"""
            val provider = TronProvider(clientRespondingWith(body))

            val result = provider.fetchBalances(ChainId.TRON, "T-address")

            check(result is ProviderResult.Success)
            assertTrue(result.value.isEmpty())
        }
}
