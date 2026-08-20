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

class BitgetConnector(
    private val httpClient: HttpClient,
) : ExchangeConnector {
    override val id: ExchangeId = ExchangeId.BITGET

    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> {
        val creds =
            credentials as? ExchangeCredentials.ApiKeySecretPassphrase
                ?: return ProviderResult.Failure(ProviderError.UnexpectedResponse("Bitget requires an API key + secret + passphrase"))

        return try {
            val timestamp =
                Clock.System
                    .now()
                    .toEpochMilliseconds()
                    .toString()
            val prehash = timestamp + "GET" + REQUEST_PATH
            val signature = HmacSigner.base64(creds.apiSecret, prehash)
            val response =
                httpClient.get("$BASE_URL$REQUEST_PATH") {
                    header("ACCESS-KEY", creds.apiKey)
                    header("ACCESS-SIGN", signature)
                    header("ACCESS-TIMESTAMP", timestamp)
                    header("ACCESS-PASSPHRASE", creds.passphrase)
                }
            mapResponse(response)
        } catch (e: Exception) {
            networkExceptionToFailure(e, "Bitget request failed")
        }
    }

    private suspend fun mapResponse(response: HttpResponse): ProviderResult<List<TokenBalance>> {
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            return ProviderResult.Failure(ProviderError.AuthenticationFailed("Bitget rejected the API credentials"))
        }
        if (response.status == HttpStatusCode.TooManyRequests) {
            return ProviderResult.Failure(ProviderError.RateLimited("Bitget rate limit hit"))
        }
        if (!response.status.isSuccess()) {
            return ProviderResult.Failure(ProviderError.Unavailable("Bitget returned ${response.status}"))
        }
        val body = response.body<BitgetAssetsResponse>()
        if (body.code != "00000") {
            return ProviderResult.Failure(ProviderError.UnexpectedResponse(body.msg ?: "Bitget error ${body.code}"))
        }
        val balances =
            body.data.mapNotNull { asset ->
                val qty =
                    (asset.available.toDoubleOrNull() ?: 0.0) + (asset.frozen.toDoubleOrNull() ?: 0.0) +
                        (asset.locked.toDoubleOrNull() ?: 0.0)
                if (qty <= 0.0) null else TokenBalance(assetSymbol = asset.coin.uppercase(), quantity = qty)
            }
        return ProviderResult.Success(balances)
    }

    private companion object {
        const val BASE_URL = "https://api.bitget.com"
        const val REQUEST_PATH = "/api/v2/spot/account/assets"
    }
}

@Serializable
private data class BitgetAssetsResponse(
    val code: String,
    val msg: String? = null,
    val data: List<BitgetAsset> = emptyList(),
)

@Serializable
private data class BitgetAsset(
    val coin: String,
    val available: String = "0",
    val frozen: String = "0",
    val locked: String = "0",
)
