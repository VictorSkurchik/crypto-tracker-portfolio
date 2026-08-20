package by.vsdev.cpt.feature.customassets

import app.cash.turbine.test
import by.vsdev.cpt.core.data.CustomAssetsRepository
import by.vsdev.cpt.core.model.CustomAssetPricing
import by.vsdev.cpt.feature.customassets.fakes.FakeCustomAssetDao
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
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CustomAssetsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(dao: FakeCustomAssetDao = FakeCustomAssetDao()) = CustomAssetsViewModel(CustomAssetsRepository(dao))

    @Test
    fun `addFixedPriceAsset adds an asset with fixed pricing`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.assets.test {
                assertEquals(emptyList(), awaitItem())

                viewModel.addFixedPriceAsset("My Bitcoin", "btc", 2.5, 60000.0)
                advanceUntilIdle()

                val assets = awaitItem()
                assertEquals(1, assets.size)
                val asset = assets.single()
                assertEquals("My Bitcoin", asset.displayName)
                assertEquals("BTC", asset.assetSymbol)
                assertEquals(2.5, asset.quantity)
                val pricing = assertIs<CustomAssetPricing.Fixed>(asset.pricing)
                assertEquals(60000.0, pricing.unitPriceUsd)
            }
            assertNull(viewModel.validationError.value)
        }

    @Test
    fun `addLivePricedAsset adds an asset with live CoinMarketCap pricing`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.assets.test {
                assertEquals(emptyList(), awaitItem())

                viewModel.addLivePricedAsset("My Ether", "eth", 1.0, "eth")
                advanceUntilIdle()

                val asset = awaitItem().single()
                assertEquals("ETH", asset.assetSymbol)
                val pricing = assertIs<CustomAssetPricing.LiveFromCoinMarketCap>(asset.pricing)
                assertEquals("ETH", pricing.cmcSymbol)
            }
        }

    @Test
    fun `addFixedPriceAsset with unparsable quantity reports a distinct error from zero quantity`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.addFixedPriceAsset("Label", "BTC", null, 100.0)
            val notANumberError = viewModel.validationError.value
            assertEquals("Quantity must be a valid number", notANumberError)

            viewModel.clearValidationError()
            viewModel.addFixedPriceAsset("Label", "BTC", 0.0, 100.0)
            val zeroError = viewModel.validationError.value
            assertEquals("Quantity must be greater than zero", zeroError)

            assertTrue(notANumberError != zeroError)
        }

    @Test
    fun `validation error is surfaced then cleared`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.validationError.test {
                assertNull(awaitItem())

                viewModel.addFixedPriceAsset("Label", "", 1.0, 1.0)
                assertEquals("Symbol is required", awaitItem())

                viewModel.clearValidationError()
                assertNull(awaitItem())
            }
        }
}
