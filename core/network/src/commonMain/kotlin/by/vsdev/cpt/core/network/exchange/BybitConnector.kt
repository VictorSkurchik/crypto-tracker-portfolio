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

class BybitConnector(
    private val httpClient: HttpClient,
) : ExchangeConnector {
    override val id: ExchangeId = ExchangeId.BYBIT

    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> {
        val creds =
            credentials as? ExchangeCredentials.ApiKeySecret
                ?: return ProviderResult.Failure(ProviderError.UnexpectedResponse("Bybit requires an API key + secret"))

        return try {
            val timestamp =
                Clock.System
                    .now()
                    .toEpochMilliseconds()
                    .toString()
            val query = "accountType=UNIFIED"
            val prehash = timestamp + creds.apiKey + RECV_WINDOW + query
            val signature = HmacSigner.hex(creds.apiSecret, prehash)
            val response =
                httpClient.get("$BASE_URL/v5/account/wallet-balance?$query") {
                    header("X-BAPI-API-KEY", creds.apiKey)
                    header("X-BAPI-SIGN", signature)
                    header("X-BAPI-TIMESTAMP", timestamp)
                    header("X-BAPI-RECV-WINDOW", RECV_WINDOW)
                }
            mapResponse(response)
        } catch (e: Exception) {
            networkExceptionToFailure(e, "Bybit request failed")
        }
    }

    private suspend fun mapResponse(response: HttpResponse): ProviderResult<List<TokenBalance>> {
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            return ProviderResult.Failure(ProviderError.AuthenticationFailed("Bybit rejected the API credentials"))
        }
        if (response.status == HttpStatusCode.TooManyRequests) {
            return ProviderResult.Failure(ProviderError.RateLimited("Bybit rate limit hit"))
        }
        if (!response.status.isSuccess()) {
            return ProviderResult.Failure(ProviderError.Unavailable("Bybit returned ${response.status}"))
        }
        val body = response.body<BybitWalletBalanceResponse>()
        if (body.retCode != 0) {
            return ProviderResult.Failure(ProviderError.UnexpectedResponse(body.retMsg ?: "Bybit error ${body.retCode}"))
        }
        val balances =
            body.result.list.firstOrNull()?.coin.orEmpty().mapNotNull { coin ->
                val qty = coin.walletBalance.toDoubleOrNull() ?: 0.0
                if (qty <= 0.0) null else TokenBalance(assetSymbol = coin.coin, quantity = qty)
            }
        return ProviderResult.Success(balances)
    }

    private companion object {
        const val BASE_URL = "https://api.bybit.com"
        const val RECV_WINDOW = "10000"
    }
}

@Serializable
private data class BybitWalletBalanceResponse(
    val retCode: Int,
    val retMsg: String? = null,
    val result: BybitResult = BybitResult(),
)

@Serializable
private data class BybitResult(
    val list: List<BybitAccountBalance> = emptyList(),
)

@Serializable
private data class BybitAccountBalance(
    val accountType: String = "",
    val coin: List<BybitCoinBalance> = emptyList(),
)

@Serializable
private data class BybitCoinBalance(
    val coin: String,
    val walletBalance: String = "0",
)
