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

class OkxConnector(
    private val httpClient: HttpClient,
) : ExchangeConnector {
    override val id: ExchangeId = ExchangeId.OKX

    override suspend fun fetchBalances(credentials: ExchangeCredentials): ProviderResult<List<TokenBalance>> {
        val creds =
            credentials as? ExchangeCredentials.ApiKeySecretPassphrase
                ?: return ProviderResult.Failure(ProviderError.UnexpectedResponse("OKX requires an API key + secret + passphrase"))

        return try {
            val timestamp = Clock.System.now().toString()
            val prehash = timestamp + "GET" + REQUEST_PATH
            val signature = HmacSigner.base64(creds.apiSecret, prehash)
            val response =
                httpClient.get("$BASE_URL$REQUEST_PATH") {
                    header("OK-ACCESS-KEY", creds.apiKey)
                    header("OK-ACCESS-SIGN", signature)
                    header("OK-ACCESS-TIMESTAMP", timestamp)
                    header("OK-ACCESS-PASSPHRASE", creds.passphrase)
                }
            mapResponse(response)
        } catch (e: Exception) {
            networkExceptionToFailure(e, "OKX request failed")
        }
    }

    private suspend fun mapResponse(response: HttpResponse): ProviderResult<List<TokenBalance>> {
        if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
            return ProviderResult.Failure(ProviderError.AuthenticationFailed("OKX rejected the API credentials"))
        }
        if (response.status == HttpStatusCode.TooManyRequests) {
            return ProviderResult.Failure(ProviderError.RateLimited("OKX rate limit hit"))
        }
        if (!response.status.isSuccess()) {
            return ProviderResult.Failure(ProviderError.Unavailable("OKX returned ${response.status}"))
        }
        val body = response.body<OkxBalanceResponse>()
        if (body.code != "0") {
            return ProviderResult.Failure(ProviderError.UnexpectedResponse(body.msg ?: "OKX error ${body.code}"))
        }
        val balances =
            body.data.firstOrNull()?.details.orEmpty().mapNotNull { detail ->
                val qty = detail.eq.toDoubleOrNull() ?: detail.cashBal.toDoubleOrNull() ?: 0.0
                if (qty <= 0.0) null else TokenBalance(assetSymbol = detail.ccy, quantity = qty)
            }
        return ProviderResult.Success(balances)
    }

    private companion object {
        const val BASE_URL = "https://www.okx.com"
        const val REQUEST_PATH = "/api/v5/account/balance"
    }
}

@Serializable
private data class OkxBalanceResponse(
    val code: String,
    val msg: String? = null,
    val data: List<OkxBalanceData> = emptyList(),
)

@Serializable
private data class OkxBalanceData(
    val details: List<OkxBalanceDetail> = emptyList(),
)

@Serializable
private data class OkxBalanceDetail(
    val ccy: String,
    val cashBal: String = "0",
    val eq: String = "0",
)
