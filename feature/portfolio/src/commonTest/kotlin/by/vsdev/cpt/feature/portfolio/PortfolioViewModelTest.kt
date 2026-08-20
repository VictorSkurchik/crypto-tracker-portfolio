package by.vsdev.cpt.feature.portfolio

import by.vsdev.cpt.core.data.CustomAssetsRepository
import by.vsdev.cpt.core.data.ExchangesRepository
import by.vsdev.cpt.core.data.PortfolioRepository
import by.vsdev.cpt.core.data.WalletsRepository
import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.core.model.DefaultExchangeConnectorRegistry
import by.vsdev.cpt.core.model.DefaultOnChainProviderRegistry
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.ExchangeCredentials
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.feature.portfolio.fakes.FakeBalanceDao
import by.vsdev.cpt.feature.portfolio.fakes.FakeCustomAssetDao
import by.vsdev.cpt.feature.portfolio.fakes.FakeExchangeAccountDao
import by.vsdev.cpt.feature.portfolio.fakes.FakeExchangeConnector
import by.vsdev.cpt.feature.portfolio.fakes.FakePriceProvider
import by.vsdev.cpt.feature.portfolio.fakes.FakeSecretStore
import by.vsdev.cpt.feature.portfolio.fakes.FakeWalletDao
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildRepository(
        priceProvider: FakePriceProvider = FakePriceProvider(),
        exchangeConnectorRegistry: ExchangeConnectorRegistry = DefaultExchangeConnectorRegistry(emptyList()),
        walletDao: FakeWalletDao = FakeWalletDao(),
        exchangeAccountDao: FakeExchangeAccountDao = FakeExchangeAccountDao(),
        customAssetDao: FakeCustomAssetDao = FakeCustomAssetDao(),
        balanceDao: FakeBalanceDao = FakeBalanceDao(),
    ): PortfolioRepository =
        PortfolioRepository(
            walletsRepository = WalletsRepository(walletDao),
            exchangesRepository = ExchangesRepository(exchangeAccountDao, FakeSecretStore()),
            customAssetsRepository = CustomAssetsRepository(customAssetDao),
            balanceDao = balanceDao,
            onChainProviderRegistry = DefaultOnChainProviderRegistry(emptyList()),
            exchangeConnectorRegistry = exchangeConnectorRegistry,
            priceProvider = priceProvider,
        )

    @Test
    fun `initial load auto-refreshes exactly once when nothing is cached yet`() =
        runTest(testDispatcher) {
            val priceProvider = FakePriceProvider()
            val repository = buildRepository(priceProvider = priceProvider)

            val viewModel = PortfolioViewModel(repository)
            advanceUntilIdle()

            // Exactly once: `observeSnapshot()` re-emits (still empty) after `refresh()` itself
            // writes the refresh timestamp, and that must not re-trigger another auto-refresh.
            assertEquals(1, priceProvider.callCount)
            val snapshot = viewModel.uiState.value.snapshot
            assertNotNull(snapshot?.lastUpdated)
            assertTrue(snapshot.byAccount.isEmpty())
        }

    @Test
    fun `initial load does not auto-refresh when an account is already cached`() =
        runTest(testDispatcher) {
            val walletDao = FakeWalletDao()
            WalletsRepository(walletDao).addWallet("My ETH", ChainId.ETHEREUM, "0xabc")
            val priceProvider = FakePriceProvider()
            val repository = buildRepository(priceProvider = priceProvider, walletDao = walletDao)

            val viewModel = PortfolioViewModel(repository)
            advanceUntilIdle()

            assertEquals(0, priceProvider.callCount)
            val snapshot = viewModel.uiState.value.snapshot
            assertEquals(1, snapshot?.byAccount?.size)
        }

    @Test
    fun `refresh sets isRefreshing while in flight and clears it once done`() =
        runTest(testDispatcher) {
            val walletDao = FakeWalletDao()
            WalletsRepository(walletDao).addWallet("My ETH", ChainId.ETHEREUM, "0xabc")
            val gate = CompletableDeferred<Unit>()
            val repository = buildRepository(priceProvider = FakePriceProvider(gate = gate), walletDao = walletDao)
            val viewModel = PortfolioViewModel(repository)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isRefreshing)

            viewModel.refresh()
            advanceUntilIdle()
            assertEquals(true, viewModel.uiState.value.isRefreshing)

            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.isRefreshing)
        }

    @Test
    fun `errors from a failing exchange connector surface in lastErrors`() =
        runTest(testDispatcher) {
            val exchangeAccountDao = FakeExchangeAccountDao()
            val exchangeAccount =
                ExchangesRepository(exchangeAccountDao, FakeSecretStore())
                    .addAccount("Broken", ExchangeId.BINANCE, ExchangeCredentials.ApiKeySecret("k", "s"))
            val exchangeConnectorRegistry =
                DefaultExchangeConnectorRegistry(
                    listOf(
                        FakeExchangeConnector(
                            ExchangeId.BINANCE,
                            ProviderResult.Failure(ProviderError.AuthenticationFailed("bad key")),
                        ),
                    ),
                )
            val repository =
                buildRepository(exchangeAccountDao = exchangeAccountDao, exchangeConnectorRegistry = exchangeConnectorRegistry)
            val viewModel = PortfolioViewModel(repository)
            advanceUntilIdle()

            viewModel.refresh()
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.lastErrors[exchangeAccount.id.value])
        }
}
