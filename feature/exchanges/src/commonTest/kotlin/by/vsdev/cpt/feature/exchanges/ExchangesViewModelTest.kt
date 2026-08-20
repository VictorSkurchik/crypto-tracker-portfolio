package by.vsdev.cpt.feature.exchanges

import app.cash.turbine.test
import by.vsdev.cpt.core.data.ExchangesRepository
import by.vsdev.cpt.core.model.DefaultExchangeConnectorRegistry
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.ExchangeId
import by.vsdev.cpt.core.model.ProviderError
import by.vsdev.cpt.core.model.ProviderResult
import by.vsdev.cpt.core.model.TokenBalance
import by.vsdev.cpt.feature.exchanges.fakes.FakeExchangeAccountDao
import by.vsdev.cpt.feature.exchanges.fakes.FakeExchangeConnector
import by.vsdev.cpt.feature.exchanges.fakes.FakeSecretStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ExchangesViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(
        exchangeConnectorRegistry: ExchangeConnectorRegistry = DefaultExchangeConnectorRegistry(emptyList()),
        exchangeAccountDao: FakeExchangeAccountDao = FakeExchangeAccountDao(),
    ): ExchangesViewModel =
        ExchangesViewModel(
            exchangesRepository = ExchangesRepository(exchangeAccountDao, FakeSecretStore()),
            exchangeConnectorRegistry = exchangeConnectorRegistry,
        )

    @Test
    fun `addAccount sets isVerifying while in flight and clears it once verification succeeds`() =
        runTest(testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val registry =
                DefaultExchangeConnectorRegistry(
                    listOf(
                        FakeExchangeConnector(
                            ExchangeId.BINANCE,
                            ProviderResult.Success(listOf(TokenBalance("USDT", 100.0))),
                            gate = gate,
                        ),
                    ),
                )
            val viewModel = buildViewModel(exchangeConnectorRegistry = registry)

            viewModel.connectState.test {
                assertEquals(ConnectAccountUiState(), awaitItem())

                viewModel.addAccount("My Binance", ExchangeId.BINANCE, "key", "secret", "")
                assertEquals(ConnectAccountUiState(isVerifying = true), awaitItem())

                gate.complete(Unit)
                assertEquals(ConnectAccountUiState(isVerifying = false, connectionError = null), awaitItem())
            }
        }

    @Test
    fun `addAccount surfaces the connector's failure message and stops verifying`() =
        runTest(testDispatcher) {
            val registry =
                DefaultExchangeConnectorRegistry(
                    listOf(
                        FakeExchangeConnector(
                            ExchangeId.BINANCE,
                            ProviderResult.Failure(ProviderError.AuthenticationFailed("bad key")),
                        ),
                    ),
                )
            val viewModel = buildViewModel(exchangeConnectorRegistry = registry)

            viewModel.connectState.test {
                assertEquals(ConnectAccountUiState(), awaitItem())

                viewModel.addAccount("My Binance", ExchangeId.BINANCE, "key", "secret", "")
                assertEquals(ConnectAccountUiState(isVerifying = true), awaitItem())
                assertEquals(ConnectAccountUiState(connectionError = "bad key"), awaitItem())
            }
        }

    @Test
    fun `addAccount fails fast without verifying when the API key or secret is blank`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.connectState.test {
                assertEquals(ConnectAccountUiState(), awaitItem())

                viewModel.addAccount("My Binance", ExchangeId.BINANCE, "", "secret", "")
                assertEquals(ConnectAccountUiState(connectionError = "API key and secret are required"), awaitItem())
            }
        }

    @Test
    fun `clearConnectionError clears a previously set connection error`() =
        runTest(testDispatcher) {
            val registry =
                DefaultExchangeConnectorRegistry(
                    listOf(
                        FakeExchangeConnector(
                            ExchangeId.BINANCE,
                            ProviderResult.Failure(ProviderError.AuthenticationFailed("bad key")),
                        ),
                    ),
                )
            val viewModel = buildViewModel(exchangeConnectorRegistry = registry)

            viewModel.connectState.test {
                assertEquals(ConnectAccountUiState(), awaitItem())

                viewModel.addAccount("My Binance", ExchangeId.BINANCE, "key", "secret", "")
                assertEquals(ConnectAccountUiState(isVerifying = true), awaitItem())
                assertEquals(ConnectAccountUiState(connectionError = "bad key"), awaitItem())

                viewModel.clearConnectionError()
                assertEquals(ConnectAccountUiState(), awaitItem())
            }
        }
}
