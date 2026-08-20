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
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * TON Center v3 — chosen over TonAPI because it works without an API key for occasional,
 * low-frequency polling (1 req/sec unauthenticated is plenty for a portfolio refresh; 10 req/sec
 * with a free key if that ever becomes a bottleneck). Response field names below are best-effort
 * from the public OpenAPI schema, not a live-verified sample — spot-check against a real wallet
 * address during implementation.
 */
class TonProvider(
    private val httpClient: HttpClient,
) : OnChainProvider {
    override fun supports(chainId: ChainId): Boolean = chainId == ChainId.TON

    override suspend fun fetchBalances(
        chainId: ChainId,
        address: String,
    ): ProviderResult<List<TokenBalance>> =
        try {
            ProviderResult.Success(fetchNativeBalance(address) + fetchJettonBalances(address))
        } catch (e: Exception) {
            ProviderResult.Failure(ProviderError.Unavailable(e.message ?: "TON request failed"))
        }

    private suspend fun fetchNativeBalance(address: String): List<TokenBalance> {
        val response =
            httpClient.get("$BASE_URL/accountStates") {
                parameter("address", address)
            }
        if (!response.status.isSuccess()) return emptyList()
        val body = response.body<TonAccountStatesResponse>()
        val nanotons =
            body.accounts
                .firstOrNull()
                ?.balance
                ?.toDoubleOrNull() ?: return emptyList()
        val ton = nanotons / 1e9
        return if (ton > 0.0) listOf(TokenBalance(assetSymbol = "TON", quantity = ton, chain = ChainId.TON)) else emptyList()
    }

    private suspend fun fetchJettonBalances(address: String): List<TokenBalance> {
        val response =
            httpClient.get("$BASE_URL/jetton/wallets") {
                parameter("owner_address", address)
                parameter("exclude_zero_balance", true)
            }
        if (!response.status.isSuccess()) return emptyList()
        val body = response.body<TonJettonWalletsResponse>()
        return body.jettonWallets.mapNotNull { wallet ->
            val symbol = wallet.jetton?.symbol ?: return@mapNotNull null
            val decimals = wallet.jetton.decimals ?: 9
            val quantity = (wallet.balance.toDoubleOrNull() ?: 0.0) / 10.0.pow(decimals)
            if (quantity <= 0.0) null else TokenBalance(assetSymbol = symbol, quantity = quantity, chain = ChainId.TON)
        }
    }

    private companion object {
        const val BASE_URL = "https://toncenter.com/api/v3"
    }
}

@Serializable
private data class TonAccountStatesResponse(
    val accounts: List<TonAccountState> = emptyList(),
)

@Serializable
private data class TonAccountState(
    val balance: String? = null,
)

@Serializable
private data class TonJettonWalletsResponse(
    @SerialName("jetton_wallets") val jettonWallets: List<TonJettonWallet> = emptyList(),
)

@Serializable
private data class TonJettonWallet(
    val balance: String = "0",
    val jetton: TonJettonMeta? = null,
)

@Serializable
private data class TonJettonMeta(
    val symbol: String? = null,
    val decimals: Int? = null,
)
