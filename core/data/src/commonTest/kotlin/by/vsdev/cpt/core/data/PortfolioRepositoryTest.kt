package by.vsdev.cpt.core.data

import by.vsdev.cpt.core.data.fakes.FakeBalanceDao
import by.vsdev.cpt.core.data.fakes.FakeCustomAssetDao
import by.vsdev.cpt.core.data.fakes.FakeExchangeAccountDao
import by.vsdev.cpt.core.data.fakes.FakeExchangeConnector
import by.vsdev.cpt.core.data.fakes.FakeExchangeConnectorRegistry
import by.vsdev.cpt.core.data.fakes.FakeOnChainProvider
import by.vsdev.cpt.core.data.fakes.FakeOnChainProviderRegistry
import by.vsdev.cpt.core.data.fakes.FakePriceProvider
import by.vsdev.cpt.core.data.fakes.FakeSecretStore
import by.vsdev.cpt.core.data.fakes.FakeWalletDao
import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.CustomAssetPricing
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PortfolioRepositoryTest {
    @Test
    fun `refresh aggregates wallets exchanges and custom assets into one total`() =
        runTest {
            val walletsRepository = WalletsRepository(FakeWalletDao())
            val exchangesRepository = ExchangesRepository(FakeExchangeAccountDao(), FakeSecretStore())
            val customAssetsRepository = CustomAssetsRepository(FakeCustomAssetDao())
            val balanceDao = FakeBalanceDao()

            val wallet = walletsRepository.addWallet("My ETH", ChainId.ETHEREUM, "0xabc")
            val exchangeAccount =
                exchangesRepository.addAccount("My Binance", ExchangeId.BINANCE, ExchangeCredentials.ApiKeySecret("k", "s"))
            customAssetsRepository.addAsset("Cold storage", "BTC", 2.0, CustomAssetPricing.Fixed(50_000.0))

            val repository =
                PortfolioRepository(
                    walletsRepository = walletsRepository,
                    exchangesRepository = exchangesRepository,
                    customAssetsRepository = customAssetsRepository,
                    balanceDao = balanceDao,
                    onChainProviderRegistry =
                        FakeOnChainProviderRegistry(
                            listOf(
                                FakeOnChainProvider(
                                    ChainId.ETHEREUM,
                                    balancesByAddress = mapOf("0xabc" to listOf(TokenBalance("ETH", 1.5))),
                                ),
                            ),
                        ),
                    exchangeConnectorRegistry =
                        FakeExchangeConnectorRegistry(
                            listOf(
                                FakeExchangeConnector(
                                    ExchangeId.BINANCE,
                                    ProviderResult.Success(listOf(TokenBalance("USDT", 1_000.0))),
                                ),
                            ),
                        ),
                    priceProvider = FakePriceProvider(prices = mapOf("ETH" to 2_000.0, "USDT" to 1.0)),
                )

            val errors = repository.refresh()

            // ETH: 1.5 * 2000 = 3000, USDT: 1000 * 1 = 1000, BTC custom: 2 * 50000 = 100000
            val snapshot = repository.observeSnapshot().first()
            assertEquals(3_000.0 + 1_000.0 + 100_000.0, snapshot.totalValueUsd)
            assertEquals(3, snapshot.byAccount.size)
            assertNull(errors[wallet.id.value])
            assertNull(errors[exchangeAccount.id.value])
        }

    @Test
    fun `one exchange failing does not blank out other accounts cached balances`() =
        runTest {
            val walletsRepository = WalletsRepository(FakeWalletDao())
            val exchangesRepository = ExchangesRepository(FakeExchangeAccountDao(), FakeSecretStore())
            val customAssetsRepository = CustomAssetsRepository(FakeCustomAssetDao())
            val balanceDao = FakeBalanceDao()

            val healthyAccount =
                exchangesRepository.addAccount("Healthy", ExchangeId.BINANCE, ExchangeCredentials.ApiKeySecret("k", "s"))
            val brokenAccount =
                exchangesRepository.addAccount("Broken", ExchangeId.OKX, ExchangeCredentials.ApiKeySecretPassphrase("k", "s", "p"))

            val repository =
                PortfolioRepository(
                    walletsRepository = walletsRepository,
                    exchangesRepository = exchangesRepository,
                    customAssetsRepository = customAssetsRepository,
                    balanceDao = balanceDao,
                    onChainProviderRegistry = FakeOnChainProviderRegistry(emptyList()),
                    exchangeConnectorRegistry =
                        FakeExchangeConnectorRegistry(
                            listOf(
                                FakeExchangeConnector(
                                    ExchangeId.BINANCE,
                                    ProviderResult.Success(listOf(TokenBalance("USDT", 500.0))),
                                ),
                                FakeExchangeConnector(
                                    ExchangeId.OKX,
                                    ProviderResult.Failure(ProviderError.AuthenticationFailed("bad key")),
                                ),
                            ),
                        ),
                    priceProvider = FakePriceProvider(prices = mapOf("USDT" to 1.0)),
                )

            // First a successful sync for the broken account, so it has cached history to potentially lose.
            val firstConnectorRegistry =
                FakeExchangeConnectorRegistry(
                    listOf(
                        FakeExchangeConnector(ExchangeId.BINANCE, ProviderResult.Success(listOf(TokenBalance("USDT", 500.0)))),
                        FakeExchangeConnector(ExchangeId.OKX, ProviderResult.Success(listOf(TokenBalance("BTC", 1.0)))),
                    ),
                )
            PortfolioRepository(
                walletsRepository,
                exchangesRepository,
                customAssetsRepository,
                balanceDao,
                FakeOnChainProviderRegistry(emptyList()),
                firstConnectorRegistry,
                FakePriceProvider(prices = mapOf("USDT" to 1.0, "BTC" to 60_000.0)),
            ).refresh()

            val errors = repository.refresh()

            assertNull(errors[healthyAccount.id.value])
            assertNotNull(errors[brokenAccount.id.value])

            val snapshot = repository.observeSnapshot().first()
            val brokenAccountBreakdown = snapshot.byAccount.single { it.accountId == brokenAccount.id }
            // The broken account's previous BTC balance must survive an account-level auth failure.
            assertEquals(60_000.0, brokenAccountBreakdown.valueUsd)
        }
}
