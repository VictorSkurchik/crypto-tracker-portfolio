package by.vsdev.cpt.core.network.exchange

import by.vsdev.cpt.core.model.ExchangeConnector
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import by.vsdev.cpt.core.network.HmacSigner
import by.vsdev.cpt.core.network.networkExceptionToFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlin.time.Clock

class BinanceConnector(
    private val httpClient: HttpClient,
) : ExchangeConnector {
    override val id: ExchangeId = ExchangeId.BINANCE

    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> {
        val creds =
            credentials as? ExchangeCredentials.ApiKeySecret
                ?: return ProviderResult.Failure(ProviderError.UnexpectedResponse("Binance requires an API key + secret"))

        return try {
            val query = "timestamp=${Clock.System.now().toEpochMilliseconds()}&recvWindow=10000"
            val signature = HmacSigner.hex(creds.apiSecret, query)
            val response =
                httpClient.get("$BASE_URL/api/v3/account?$query&signature=$signature") {
                    header("X-MBX-APIKEY", creds.apiKey)
                }
            mapResponse(response)
        } catch (e: Exception) {
            networkExceptionToFailure(e, "Binance request failed")
        }
    }

    private suspend fun mapResponse(response: HttpResponse): ProviderResult<List<TokenBalance>> =
        when {
            response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden ->
                ProviderResult.Failure(ProviderError.AuthenticationFailed("Binance rejected the API key/secret"))
            response.status == HttpStatusCode.TooManyRequests ->
                ProviderResult.Failure(ProviderError.RateLimited("Binance rate limit hit"))
            !response.status.isSuccess() ->
                ProviderResult.Failure(ProviderError.Unavailable("Binance returned ${response.status}"))
            else -> {
                val body = response.body<BinanceAccountResponse>()
                val balances =
                    body.balances.mapNotNull { balance ->
                        val total = (balance.free.toDoubleOrNull() ?: 0.0) + (balance.locked.toDoubleOrNull() ?: 0.0)
                        if (total <= 0.0) null else TokenBalance(assetSymbol = balance.asset, quantity = total)
                    }
                ProviderResult.Success(balances)
            }
        }

    private companion object {
        const val BASE_URL = "https://api.binance.com"
    }
}

@Serializable
private data class BinanceAccountResponse(
    val balances: List<BinanceBalance> = emptyList(),
)

@Serializable
private data class BinanceBalance(
    val asset: String,
    val free: String,
    val locked: String,
)
