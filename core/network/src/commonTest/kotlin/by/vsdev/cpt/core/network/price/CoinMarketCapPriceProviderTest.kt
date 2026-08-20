package by.vsdev.cpt.core.network.price

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

class CoinMarketCapPriceProviderTest {
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
    fun `maps the uppercase USD quote field to a price per symbol`() =
        runTest {
            val body = """{"data":{"BTC":[{"quote":{"USD":{"price":65000.5}}}]}}"""
            val provider = CoinMarketCapPriceProvider(clientRespondingWith(body)) { "key" }

            val result = provider.getPrices(setOf("BTC"))

            check(result is ProviderResult.Success)
            assertEquals(65000.5, result.value.getValue("BTC"))
        }

    @Test
    fun `an empty symbol set short-circuits without making a request`() =
        runTest {
            val provider = CoinMarketCapPriceProvider(clientRespondingWith("{}")) { error("should not be called") }

            val result = provider.getPrices(emptySet())

            check(result is ProviderResult.Success)
            assertTrue(result.value.isEmpty())
        }

    @Test
    fun `a missing API key surfaces as AuthenticationFailed without making a request`() =
        runTest {
            val provider = CoinMarketCapPriceProvider(clientRespondingWith("{}")) { null }

            val result = provider.getPrices(setOf("BTC"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.AuthenticationFailed)
        }

    @Test
    fun `maps 429 to RateLimited`() =
        runTest {
            val engine =
                MockEngine { _ ->
                    respond(
                        content = "{}",
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
            val provider = CoinMarketCapPriceProvider(client) { "key" }

            val result = provider.getPrices(setOf("BTC"))

            check(result is ProviderResult.Failure)
            assertTrue(result.error is ProviderError.RateLimited)
        }
}
