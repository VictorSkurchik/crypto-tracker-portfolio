package by.vsdev.cpt.core.network.price

import by.vsdev.cpt.core.model.PriceProvider
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class CoinMarketCapPriceProvider(
    private val httpClient: HttpClient,
    private val apiKey: suspend () -> String?,
) : PriceProvider {
    override suspend fun getPrices(symbols: Set<String>): ProviderResult<Map<String, Double>> {
        if (symbols.isEmpty()) return ProviderResult.Success(emptyMap())
        val key =
            apiKey()
                ?: return ProviderResult.Failure(ProviderError.AuthenticationFailed("No CoinMarketCap API key configured"))

        return try {
            val response =
                httpClient.get("$BASE_URL/v2/cryptocurrency/quotes/latest") {
                    header("X-CMC_PRO_API_KEY", key)
                    parameter("symbol", symbols.joinToString(","))
                }
            if (response.status == HttpStatusCode.TooManyRequests) {
                return ProviderResult.Failure(ProviderError.RateLimited("CoinMarketCap rate limit hit"))
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failure(ProviderError.Unavailable("CoinMarketCap returned ${response.status}"))
            }
            val body = response.body<CmcQuotesResponse>()
            val prices =
                body.data
                    .mapNotNull { (symbol, entries) ->
                        val price =
                            entries
                                .firstOrNull()
                                ?.quote
                                ?.usd
                                ?.price ?: return@mapNotNull null
                        symbol to price
                    }.toMap()
            ProviderResult.Success(prices)
        } catch (e: Exception) {
            ProviderResult.Failure(ProviderError.Unavailable(e.message ?: "CoinMarketCap request failed"))
        }
    }

    private companion object {
        const val BASE_URL = "https://pro-api.coinmarketcap.com"
    }
}

@Serializable
private data class CmcQuotesResponse(
    val data: Map<String, List<CmcQuoteEntry>> = emptyMap(),
)

@Serializable
private data class CmcQuoteEntry(
    val quote: CmcQuote = CmcQuote(),
)

@Serializable
private data class CmcQuote(
    @SerialName("USD") val usd: CmcUsdQuote? = null,
)

@Serializable
private data class CmcUsdQuote(
    val price: Double? = null,
)
