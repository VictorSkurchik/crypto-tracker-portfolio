package by.vsdev.cpt.core.network.onchain

import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.OnChainProvider
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Etherscan's V2 unified API — one API key + a `chainid` param covers Ethereum, Optimism, and
 * Arbitrum One. Native-coin balance only for v1: the free tier only offers per-contract ERC-20
 * lookups (no bulk "all tokens held by this wallet" endpoint), so tracking specific ERC-20 tokens
 * on these chains is a natural fast-follow, not MVP scope.
 */
class EtherscanV2Provider(
    private val httpClient: HttpClient,
    private val apiKey: suspend () -> String?,
) : OnChainProvider {
    override fun supports(chainId: ChainId): Boolean = chainId in CHAIN_IDS

    override suspend fun fetchBalances(
        chainId: ChainId,
        address: String,
    ): ProviderResult<List<TokenBalance>> {
        val key =
            apiKey()
                ?: return ProviderResult.Failure(ProviderError.AuthenticationFailed("No Etherscan API key configured"))
        val chainIdNumber =
            CHAIN_IDS[chainId]
                ?: return ProviderResult.Failure(ProviderError.UnexpectedResponse("Unsupported chain $chainId"))

        return try {
            val response =
                httpClient.get(BASE_URL) {
                    parameter("chainid", chainIdNumber)
                    parameter("module", "account")
                    parameter("action", "balance")
                    parameter("address", address)
                    parameter("tag", "latest")
                    parameter("apikey", key)
                }
            if (response.status == HttpStatusCode.TooManyRequests) {
                return ProviderResult.Failure(ProviderError.RateLimited("Etherscan rate limit hit"))
            }
            if (!response.status.isSuccess()) {
                return ProviderResult.Failure(ProviderError.Unavailable("Etherscan returned ${response.status}"))
            }
            val body = response.body<EtherscanBalanceResponse>()
            if (body.status != "1") {
                return ProviderResult.Failure(ProviderError.UnexpectedResponse(body.message ?: "Etherscan error"))
            }
            val weiBalance = body.result.toDoubleOrNull() ?: 0.0
            val nativeBalance = weiBalance / 1e18
            val balances =
                if (nativeBalance > 0.0) {
                    listOf(TokenBalance(assetSymbol = "ETH", quantity = nativeBalance, chain = chainId))
                } else {
                    emptyList()
                }
            ProviderResult.Success(balances)
        } catch (e: Exception) {
            ProviderResult.Failure(ProviderError.Unavailable(e.message ?: "Etherscan request failed"))
        }
    }

    private companion object {
        const val BASE_URL = "https://api.etherscan.io/v2/api"
        val CHAIN_IDS =
            mapOf(
                ChainId.ETHEREUM to 1,
                ChainId.OPTIMISM to 10,
                ChainId.ARBITRUM to 42161,
            )
    }
}

@Serializable
private data class EtherscanBalanceResponse(
    val status: String,
    val message: String? = null,
    val result: String = "0",
)
