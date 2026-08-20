package by.vsdev.cpt.feature.settings

import app.cash.turbine.test
import by.vsdev.cpt.core.datastore.AppPreferences
import by.vsdev.cpt.feature.settings.fakes.FakeSecretStore
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

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads persisted keys`() =
        runTest(testDispatcher) {
            val secretStore = FakeSecretStore()
            val appPreferences = AppPreferences(secretStore)
            appPreferences.setCoinMarketCapApiKey("cmc-key")
            appPreferences.setEtherscanApiKey("etherscan-key")

            val viewModel = SettingsViewModel(appPreferences)
            advanceUntilIdle()

            assertEquals(SettingsUiState("cmc-key", "etherscan-key"), viewModel.uiState.value)
        }

    @Test
    fun `setCoinMarketCapApiKey updates state and persists`() =
        runTest(testDispatcher) {
            val appPreferences = AppPreferences(FakeSecretStore())
            val viewModel = SettingsViewModel(appPreferences)
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(SettingsUiState(), awaitItem())

                viewModel.setCoinMarketCapApiKey("new-cmc-key")

                assertEquals(SettingsUiState(coinMarketCapApiKey = "new-cmc-key"), awaitItem())
            }
            advanceUntilIdle()
            assertEquals("new-cmc-key", appPreferences.coinMarketCapApiKey())
        }

    @Test
    fun `setEtherscanApiKey updates state and persists`() =
        runTest(testDispatcher) {
            val appPreferences = AppPreferences(FakeSecretStore())
            val viewModel = SettingsViewModel(appPreferences)
            advanceUntilIdle()

            viewModel.uiState.test {
                assertEquals(SettingsUiState(), awaitItem())

                viewModel.setEtherscanApiKey("new-etherscan-key")

                assertEquals(SettingsUiState(etherscanApiKey = "new-etherscan-key"), awaitItem())
            }
            advanceUntilIdle()
            assertEquals("new-etherscan-key", appPreferences.etherscanApiKey())
        }
}
