package by.vsdev.cpt.core.network.onchain

import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.OnChainProvider
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * TronGrid `/v1/accounts/{address}` — one call gives both native TRX and TRC20 balances.
 * TRC20 entries only carry a contract address, not a symbol/decimals, so only a small hardcoded
 * set of well-known contracts (starting with USDT) are recognized for v1 — an unrecognized
 * contract is skipped rather than shown as a raw address, since that's not actionable for a user.
 */
class TronProvider(
    private val httpClient: HttpClient,
) : OnChainProvider {
    override fun supports(chainId: ChainId): Boolean = chainId == ChainId.TRON

    override suspend fun fetchBalances(
        chainId: ChainId,
        address: String,
    ): ProviderResult<List<TokenBalance>> =
        try {
            val response = httpClient.get("$BASE_URL/v1/accounts/$address")
            if (!response.status.isSuccess()) {
                ProviderResult.Failure(ProviderError.Unavailable("TronGrid returned ${response.status}"))
            } else {
                val body = response.body<TronAccountResponse>()
                val account = body.data.firstOrNull()
                val balances =
                    buildList {
                        account?.balance?.let { sun ->
                            val trx = sun / 1_000_000.0
                            if (trx > 0.0) add(TokenBalance(assetSymbol = "TRX", quantity = trx, chain = ChainId.TRON))
                        }
                        account?.trc20.orEmpty().forEach { entry ->
                            entry.entries.forEach { (contract, rawBalance) ->
                                val known = KNOWN_TRC20[contract] ?: return@forEach
                                val quantity = (rawBalance.jsonPrimitive.content.toDoubleOrNull() ?: 0.0) / known.scale
                                if (quantity > 0.0) add(TokenBalance(assetSymbol = known.symbol, quantity = quantity, chain = ChainId.TRON))
                            }
                        }
                    }
                ProviderResult.Success(balances)
            }
        } catch (e: Exception) {
            ProviderResult.Failure(ProviderError.Unavailable(e.message ?: "TronGrid request failed"))
        }

    private data class KnownToken(
        val symbol: String,
        val scale: Double,
    )

    private companion object {
        const val BASE_URL = "https://api.trongrid.io"
        val KNOWN_TRC20 =
            mapOf(
                "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t" to KnownToken("USDT", 1_000_000.0),
            )
    }
}

@Serializable
private data class TronAccountResponse(
    val data: List<TronAccountData> = emptyList(),
)

@Serializable
private data class TronAccountData(
    val balance: Long? = null,
    val trc20: List<JsonObject>? = null,
)
