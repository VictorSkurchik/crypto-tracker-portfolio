package by.vsdev.cpt.core.data

import by.vsdev.cpt.core.database.dao.BalanceDao
import by.vsdev.cpt.core.database.entity.CachedBalanceEntity
import by.vsdev.cpt.core.database.entity.RefreshStateEntity
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountBreakdown
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.AssetBreakdown
import by.vsdev.cpt.core.model.CustomAssetPricing
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.OnChainProviderRegistry
import by.vsdev.cpt.core.model.PortfolioSnapshot
import by.vsdev.cpt.core.model.PriceProvider
import by.vsdev.cpt.core.model.PricedBalance
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Batches ONE [PriceProvider] call per distinct symbol across the whole refresh (never per
 * account), and persists per-account results independently so one account's failure never blanks
 * out the others' cached data.
 */
class PortfolioRepository(
    private val walletsRepository: WalletsRepository,
    private val exchangesRepository: ExchangesRepository,
    private val customAssetsRepository: CustomAssetsRepository,
    private val balanceDao: BalanceDao,
    private val onChainProviderRegistry: OnChainProviderRegistry,
    private val exchangeConnectorRegistry: ExchangeConnectorRegistry,
    private val priceProvider: PriceProvider,
) {
    /** Guards [refresh] against two overlapping triggers racing the same account lists/DB rows. */
    private val refreshMutex = Mutex()

    fun observeSnapshot(): Flow<PortfolioSnapshot> =
        combine(
            walletsRepository.observeWallets(),
            exchangesRepository.observeAccounts(),
            customAssetsRepository.observeAssets(),
            balanceDao.observeAll(),
            balanceDao.observeRefreshState(),
        ) { wallets, exchanges, customAssets, cachedBalances, refreshState ->
            buildSnapshot(wallets, exchanges, customAssets, cachedBalances, refreshState?.lastRefreshedEpochMillis)
        }

    /** No-ops (empty error map) if a refresh is already in flight. */
    suspend fun refresh(): Map<String, ProviderError?> {
        if (!refreshMutex.tryLock()) return emptyMap()
        try {
            return coroutineScope {
                val wallets = walletsRepository.observeWallets().first()
                val exchanges = exchangesRepository.observeAccounts().first()
                val customAssets = customAssetsRepository.observeAssets().first()

                val rawByAccount =
                    (fetchWalletResults(wallets) + fetchExchangeResults(exchanges)).awaitAll().toMap()
                val prices = fetchPrices(rawByAccount, customAssets)
                val now = Clock.System.now().toEpochMilliseconds()

                val newBalances = mutableListOf<CachedBalanceEntity>()
                val errorsByAccount =
                    persistAccountBalances(wallets.map { it.id } + exchanges.map { it.id }, rawByAccount, prices, now, newBalances)
                persistCustomAssetBalances(customAssets, prices, now, newBalances)

                balanceDao.upsertAll(newBalances)
                balanceDao.upsertRefreshState(RefreshStateEntity(lastRefreshedEpochMillis = now))
                errorsByAccount
            }
        } finally {
            refreshMutex.unlock()
        }
    }

    private fun CoroutineScope.fetchWalletResults(
        wallets: List<Account.OnChainWallet>,
    ): List<Deferred<Pair<String, ProviderResult<List<TokenBalance>>>>> =
        wallets.map { wallet ->
            async {
                wallet.id.value to
                    fetchAccountBalances {
                        onChainProviderRegistry.resolve(wallet.chain)?.fetchBalances(wallet.chain, wallet.address)
                            ?: ProviderResult.Failure(ProviderError.UnexpectedResponse("No provider registered for ${wallet.chain}"))
                    }
            }
        }

    private fun CoroutineScope.fetchExchangeResults(
        exchanges: List<Account.ExchangeAccount>,
    ): List<Deferred<Pair<String, ProviderResult<List<TokenBalance>>>>> =
        exchanges.map { exchange ->
            async {
                exchange.id.value to
                    fetchAccountBalances {
                        val credentials = exchangesRepository.resolveCredentials(exchange.credentialsRef)
                        val connector = exchangeConnectorRegistry.resolve(exchange.exchange)
                        when {
                            connector == null ->
                                ProviderResult.Failure(
                                    ProviderError.UnexpectedResponse("No connector registered for ${exchange.exchange}"),
                                )
                            credentials == null ->
                                ProviderResult.Failure(ProviderError.AuthenticationFailed("Missing stored credentials"))
                            else -> connector.fetchBalances(credentials)
                        }
                    }
            }
        }

    /**
     * Bounds [fetch] to [ACCOUNT_FETCH_TIMEOUT_MILLIS] (defense-in-depth on top of the network
     * layer's own HTTP timeouts, so one hung call can't hold up the rest of [refresh]), and retries
     * a transient [ProviderError.RateLimited] up to [MAX_RATE_LIMIT_RETRIES] times.
     */
    private suspend fun fetchAccountBalances(fetch: suspend () -> ProviderResult<List<TokenBalance>>): ProviderResult<List<TokenBalance>> {
        var result = fetchWithTimeout(fetch)
        var retries = 0
        while (result is ProviderResult.Failure && result.error is ProviderError.RateLimited && retries < MAX_RATE_LIMIT_RETRIES) {
            delay(RATE_LIMIT_RETRY_DELAY_MILLIS)
            result = fetchWithTimeout(fetch)
            retries++
        }
        return result
    }

    private suspend fun fetchWithTimeout(fetch: suspend () -> ProviderResult<List<TokenBalance>>): ProviderResult<List<TokenBalance>> =
        withTimeoutOrNull(ACCOUNT_FETCH_TIMEOUT_MILLIS) { fetch() }
            ?: ProviderResult.Failure(ProviderError.Timeout("Account fetch timed out after ${ACCOUNT_FETCH_TIMEOUT_MILLIS}ms"))

    private suspend fun fetchPrices(
        rawByAccount: Map<String, ProviderResult<List<TokenBalance>>>,
        customAssets: List<Account.CustomAsset>,
    ): Map<String, Double> {
        val symbolsNeedingPrice = mutableSetOf<String>()
        rawByAccount.values.forEach { result ->
            if (result is ProviderResult.Success) symbolsNeedingPrice += result.value.map { it.assetSymbol }
        }
        customAssets.forEach { asset ->
            (asset.pricing as? CustomAssetPricing.LiveFromCoinMarketCap)?.let { symbolsNeedingPrice += it.cmcSymbol }
        }
        return (priceProvider.getPrices(symbolsNeedingPrice) as? ProviderResult.Success)?.value.orEmpty()
    }

    private suspend fun persistAccountBalances(
        accountIds: List<AccountId>,
        rawByAccount: Map<String, ProviderResult<List<TokenBalance>>>,
        prices: Map<String, Double>,
        now: Long,
        newBalances: MutableList<CachedBalanceEntity>,
    ): MutableMap<String, ProviderError?> {
        val errorsByAccount = mutableMapOf<String, ProviderError?>()
        accountIds.forEach { accountId ->
            when (val result = rawByAccount[accountId.value]) {
                is ProviderResult.Success -> {
                    balanceDao.clearForAccount(accountId.value)
                    result.value.forEach { balance ->
                        val price = prices[balance.assetSymbol] ?: 0.0
                        newBalances +=
                            CachedBalanceEntity(
                                accountId = accountId.value,
                                assetSymbol = balance.assetSymbol,
                                quantity = balance.quantity,
                                priceUsd = price,
                                valueUsd = balance.quantity * price,
                                chain = balance.chain?.name,
                                lastUpdatedEpochMillis = now,
                            )
                    }
                    errorsByAccount[accountId.value] = null
                }
                is ProviderResult.Failure -> errorsByAccount[accountId.value] = result.error
                null -> Unit
            }
        }
        return errorsByAccount
    }

    private suspend fun persistCustomAssetBalances(
        customAssets: List<Account.CustomAsset>,
        prices: Map<String, Double>,
        now: Long,
        newBalances: MutableList<CachedBalanceEntity>,
    ) {
        customAssets.forEach { asset ->
            balanceDao.clearForAccount(asset.id.value)
            val price =
                when (val pricing = asset.pricing) {
                    is CustomAssetPricing.Fixed -> pricing.unitPriceUsd
                    is CustomAssetPricing.LiveFromCoinMarketCap -> prices[pricing.cmcSymbol] ?: 0.0
                }
            newBalances +=
                CachedBalanceEntity(
                    accountId = asset.id.value,
                    assetSymbol = asset.assetSymbol,
                    quantity = asset.quantity,
                    priceUsd = price,
                    valueUsd = asset.quantity * price,
                    chain = null,
                    lastUpdatedEpochMillis = now,
                )
        }
    }

    private fun buildSnapshot(
        wallets: List<Account.OnChainWallet>,
        exchanges: List<Account.ExchangeAccount>,
        customAssets: List<Account.CustomAsset>,
        cachedBalances: List<CachedBalanceEntity>,
        lastRefreshedEpochMillis: Long?,
    ): PortfolioSnapshot {
        val byAccountId = cachedBalances.groupBy { it.accountId }
        val allAccounts: List<Triple<String, String, String>> =
            wallets.map { Triple(it.id.value, it.displayName, it.chain.name) } +
                exchanges.map { Triple(it.id.value, it.displayName, "EXCHANGE") } +
                customAssets.map { Triple(it.id.value, it.displayName, "CUSTOM") }

        val accountBreakdowns =
            allAccounts.map { (accountId, displayName, badge) ->
                val balances =
                    byAccountId[accountId].orEmpty().map {
                        PricedBalance(it.assetSymbol, it.quantity, it.priceUsd, it.valueUsd)
                    }
                AccountBreakdown(
                    accountId = AccountId(accountId),
                    displayName = displayName,
                    badge = badge,
                    valueUsd = balances.sumOf { it.valueUsd },
                    balances = balances,
                )
            }

        val assetBreakdowns =
            cachedBalances
                .groupBy { it.assetSymbol }
                .map { (symbol, entries) ->
                    AssetBreakdown(assetSymbol = symbol, quantity = entries.sumOf { it.quantity }, valueUsd = entries.sumOf { it.valueUsd })
                }.sortedByDescending { it.valueUsd }

        return PortfolioSnapshot(
            totalValueUsd = accountBreakdowns.sumOf { it.valueUsd },
            byAccount = accountBreakdowns,
            byAsset = assetBreakdowns,
            lastUpdated = lastRefreshedEpochMillis?.let { Instant.fromEpochMilliseconds(it) },
        )
    }

    private companion object {
        // Larger than the network layer's own timeouts (15s request + 10s connect) — a backstop only.
        const val ACCOUNT_FETCH_TIMEOUT_MILLIS = 30_000L
        const val RATE_LIMIT_RETRY_DELAY_MILLIS = 2_000L
        const val MAX_RATE_LIMIT_RETRIES = 1
    }
}
