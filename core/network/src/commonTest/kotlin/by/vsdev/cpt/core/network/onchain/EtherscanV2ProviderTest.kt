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
import kotlin.test.assertTrue

class EtherscanV2ProviderTest {
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
    fun `supports only the three EVM chains it was built for`() {
        val provider = EtherscanV2Provider(clientRespondingWith("{}")) { "key" }

        assertTrue(provider.supports(ChainId.ETHEREUM))
        assertTrue(provider.supports(ChainId.OPTIMISM))
        assertTrue(provider.supports(ChainId.ARBITRUM))
        assertTrue(!provider.supports(ChainId.TON))
    }

    @Test
    fun `converts wei to a native ETH balance`() =
        runTest {
            val provider = EtherscanV2Provider(clientRespondingWith("""{"status":"1","result":"1500000000000000000"}""")) { "key" }

            val result = provider.fetchBalances(ChainId.ETHEREUM, "0xabc")

            check(result is ProviderResult.Success)
            assertEquals(1, result.value.size)
            assertEquals("ETH", result.value.single().assetSymbol)
            assertEquals(1.5, result.value.single().quantity)
        }

    @Test
    fun `a zero balance yields no token entries`() =
        runTest {
            val provider = EtherscanV2Provider(clientRespondingWith("""{"status":"1","result":"0"}""")) { "key" }

            val result = provider.fetchBalances(ChainId.ARBITRUM, "0xabc")

            check(result is ProviderResult.Success)
            assertTrue(result.value.isEmpty())
        }

    @Test
    fun `an Etherscan status of 0 surfaces as UnexpectedResponse not a crash`() =
        runTest {
            val provider =
                EtherscanV2Provider(clientRespondingWith("""{"status":"0","message":"NOTOK","result":"Invalid address"}""")) { "key" }

            val result = provider.fetchBalances(ChainId.ETHEREUM, "not-an-address")

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.UnexpectedResponse)
        }

    @Test
    fun `a missing API key surfaces as AuthenticationFailed without making a request`() =
        runTest {
            val provider = EtherscanV2Provider(clientRespondingWith("{}")) { null }

            val result = provider.fetchBalances(ChainId.ETHEREUM, "0xabc")

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.AuthenticationFailed)
        }
}
