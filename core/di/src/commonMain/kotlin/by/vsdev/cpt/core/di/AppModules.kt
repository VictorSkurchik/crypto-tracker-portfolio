package by.vsdev.cpt.core.di

import by.vsdev.cpt.core.data.CustomAssetsRepository
import by.vsdev.cpt.core.data.ExchangesRepository
import by.vsdev.cpt.core.data.PortfolioRepository
import by.vsdev.cpt.core.data.WalletsRepository
import by.vsdev.cpt.core.database.AppDatabase
import by.vsdev.cpt.core.database.DatabaseProvider
import by.vsdev.cpt.core.datastore.AppPreferences
import by.vsdev.cpt.core.model.DefaultExchangeConnectorRegistry
import by.vsdev.cpt.core.model.DefaultOnChainProviderRegistry
import by.vsdev.cpt.core.model.ExchangeConnectorRegistry
import by.vsdev.cpt.core.model.OnChainProviderRegistry
import by.vsdev.cpt.core.model.PriceProvider
import by.vsdev.cpt.core.network.createHttpClient
import by.vsdev.cpt.core.network.exchange.BinanceConnector
import by.vsdev.cpt.core.network.exchange.BitgetConnector
import by.vsdev.cpt.core.network.exchange.BybitConnector
import by.vsdev.cpt.core.network.exchange.OkxConnector
import by.vsdev.cpt.core.network.onchain.EtherscanV2Provider
import by.vsdev.cpt.core.network.onchain.TonProvider
import by.vsdev.cpt.core.network.onchain.TronProvider
import by.vsdev.cpt.core.network.price.CoinMarketCapPriceProvider
import by.vsdev.cpt.core.secrets.SecretStore
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

val networkModule =
    module {
        single { createHttpClient() }
        single<OnChainProviderRegistry> {
            DefaultOnChainProviderRegistry(
                listOf(
                    EtherscanV2Provider(get()) { get<AppPreferences>().etherscanApiKey() },
                    TonProvider(get()),
                    TronProvider(get()),
                ),
            )
        }
        single<ExchangeConnectorRegistry> {
            DefaultExchangeConnectorRegistry(
                listOf(
                    BinanceConnector(get()),
                    OkxConnector(get()),
                    BybitConnector(get()),
                    BitgetConnector(get()),
                ),
            )
        }
        single<PriceProvider> {
            CoinMarketCapPriceProvider(get()) { get<AppPreferences>().coinMarketCapApiKey() }
        }
    }

val databaseModule =
    module {
        single { get<DatabaseProvider>().database() }
        single { get<AppDatabase>().walletDao() }
        single { get<AppDatabase>().exchangeAccountDao() }
        single { get<AppDatabase>().customAssetDao() }
        single { get<AppDatabase>().balanceDao() }
    }

val dataModule =
    module {
        single { AppPreferences(get<SecretStore>()) }
        single { WalletsRepository(get()) }
        single { ExchangesRepository(get(), get()) }
        single { CustomAssetsRepository(get()) }
        single { PortfolioRepository(get(), get(), get(), get(), get(), get(), get()) }
    }

fun initKoin(
    platformModule: Module,
    featureModules: List<Module> = emptyList(),
) {
    startKoin {
        modules(platformModule, networkModule, databaseModule, dataModule)
        modules(featureModules)
    }
}
