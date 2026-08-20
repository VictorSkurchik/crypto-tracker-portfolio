package by.vsdev.cpt.core.network.onchain

import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.OnChainProvider
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import by.vsdev.cpt.core.network.networkExceptionToFailure
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.pow

/**
 * TON Center v3 — chosen over TonAPI because it works without an API key for occasional,
 * low-frequency polling (1 req/sec unauthenticated is plenty for a portfolio refresh; 10 req/sec
 * with a free key if that ever becomes a bottleneck). `/jetton/wallets` only returns each jetton's
 * master contract address, not its symbol/decimals, so a second `/jetton/masters` call resolves
 * metadata from `jetton_content` (whose `decimals` is itself a string, not a number).
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
        } catch (e: TonRequestFailedException) {
            ProviderResult.Failure(e.error)
        } catch (e: Exception) {
            networkExceptionToFailure(e, "TON request failed")
        }

    private suspend fun fetchNativeBalance(address: String): List<TokenBalance> {
        val response =
            httpClient.get("$BASE_URL/accountStates") {
                parameter("address", address)
            }
        response.requireSuccessOrThrow("accountStates")
        val body = response.body<TonAccountStatesResponse>()
        val nanotons =
            body.accounts
                .firstOrNull()
                ?.balance
                ?.toDoubleOrNull() ?: return emptyList()
        val ton = nanotons / NANOTONS_PER_TON
        return if (ton > 0.0) listOf(TokenBalance(assetSymbol = "TON", quantity = ton, chain = ChainId.TON)) else emptyList()
    }

    private suspend fun fetchJettonBalances(address: String): List<TokenBalance> {
        val walletsResponse =
            httpClient.get("$BASE_URL/jetton/wallets") {
                parameter("owner_address", address)
                parameter("exclude_zero_balance", true)
            }
        walletsResponse.requireSuccessOrThrow("jetton/wallets")
        val wallets = walletsResponse.body<TonJettonWalletsResponse>().jettonWallets
        if (wallets.isEmpty()) return emptyList()

        val masterAddresses = wallets.map { it.jetton }.distinct()
        val mastersResponse =
            httpClient.get("$BASE_URL/jetton/masters") {
                masterAddresses.forEach { parameter("address", it) }
            }
        mastersResponse.requireSuccessOrThrow("jetton/masters")
        val metaByAddress =
            mastersResponse
                .body<TonJettonMastersResponse>()
                .jettonMasters
                .associateBy { it.address }

        return wallets.mapNotNull { wallet ->
            val meta = metaByAddress[wallet.jetton]?.jettonContent ?: return@mapNotNull null
            val symbol = meta.symbol ?: return@mapNotNull null
            val decimals = meta.decimals?.toIntOrNull() ?: DEFAULT_JETTON_DECIMALS
            val quantity = (wallet.balance.toDoubleOrNull() ?: 0.0) / 10.0.pow(decimals)
            if (quantity <= 0.0) null else TokenBalance(assetSymbol = symbol, quantity = quantity, chain = ChainId.TON)
        }
    }

    /**
     * Throws [TonRequestFailedException] on a non-2xx response instead of the caller silently
     * treating it as "this wallet holds nothing" (as `return emptyList()` used to do here in three
     * places) -- unlike every other provider in this module, which distinguishes rate-limiting/auth
     * failures from a generic outage. A real API error must never be reported as a zero balance.
     */
    private fun HttpResponse.requireSuccessOrThrow(context: String) {
        if (status.isSuccess()) return
        val error =
            when (status) {
                HttpStatusCode.TooManyRequests -> ProviderError.RateLimited("TON Center rate limit hit ($context)")
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                    ProviderError.AuthenticationFailed("TON Center rejected the request ($context)")
                else -> ProviderError.Unavailable("TON Center returned $status ($context)")
            }
        throw TonRequestFailedException(error)
    }

    private class TonRequestFailedException(
        val error: ProviderError,
    ) : Exception(error.message)

    private companion object {
        const val BASE_URL = "https://toncenter.com/api/v3"
        const val NANOTONS_PER_TON = 1e9
        const val DEFAULT_JETTON_DECIMALS = 9
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
    val jetton: String,
)

@Serializable
private data class TonJettonMastersResponse(
    @SerialName("jetton_masters") val jettonMasters: List<TonJettonMaster> = emptyList(),
)

@Serializable
private data class TonJettonMaster(
    val address: String,
    @SerialName("jetton_content") val jettonContent: TonJettonContent? = null,
)

@Serializable
private data class TonJettonContent(
    val symbol: String? = null,
    val decimals: String? = null,
)
