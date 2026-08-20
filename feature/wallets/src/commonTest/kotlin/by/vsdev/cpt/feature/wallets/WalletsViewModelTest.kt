package by.vsdev.cpt.feature.wallets

import app.cash.turbine.test
import by.vsdev.cpt.core.data.WalletsRepository
import by.vsdev.cpt.core.model.ChainId
import by.vsdev.cpt.feature.wallets.fakes.FakeWalletDao
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class WalletsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(walletDao: FakeWalletDao = FakeWalletDao()) = WalletsViewModel(WalletsRepository(walletDao))

    @Test
    fun `adding a wallet with a valid address adds it and clears the address error`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()

            viewModel.wallets.test {
                assertEquals(emptyList(), awaitItem())

                viewModel.addWallet("My ETH", ChainId.ETHEREUM, "0x1111111111111111111111111111111111111111")

                val wallets = awaitItem()
                assertEquals(1, wallets.size)
                assertEquals("My ETH", wallets.single().displayName)
                assertEquals(ChainId.ETHEREUM, wallets.single().chain)
            }
            assertNull(viewModel.addressError.value)
        }

    @Test
    fun `adding a wallet with an invalid address sets an address error and does not add it`() =
        runTest(testDispatcher) {
            val walletDao = FakeWalletDao()
            val viewModel = buildViewModel(walletDao)

            viewModel.addWallet("My ETH", ChainId.ETHEREUM, "not-a-valid-address")

            assertNotNull(viewModel.addressError.value)
            viewModel.wallets.test {
                assertEquals(emptyList(), awaitItem())
            }
        }

    @Test
    fun `clearAddressError clears a previously set address error`() =
        runTest(testDispatcher) {
            val viewModel = buildViewModel()
            viewModel.addWallet("My ETH", ChainId.ETHEREUM, "not-a-valid-address")
            assertNotNull(viewModel.addressError.value)

            viewModel.clearAddressError()

            assertNull(viewModel.addressError.value)
        }
}
