package by.vsdev.cpt.core.data

import by.vsdev.cpt.core.database.dao.CustomAssetDao
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import by.vsdev.cpt.core.model.Account
import by.vsdev.cpt.core.model.AccountId
import by.vsdev.cpt.core.model.CustomAssetPricing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CustomAssetsRepository(
    private val customAssetDao: CustomAssetDao,
) {
    fun observeAssets(): Flow<List<Account.CustomAsset>> =
        customAssetDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addAsset(
        displayName: String,
        assetSymbol: String,
        quantity: Double,
        pricing: CustomAssetPricing,
    ): Account.CustomAsset {
        val id = AccountId(Uuid.random().toString())
        customAssetDao.upsert(id.value.toEntity(displayName, assetSymbol, quantity, pricing))
        return Account.CustomAsset(id = id, displayName = displayName, assetSymbol = assetSymbol, quantity = quantity, pricing = pricing)
    }

    suspend fun removeAsset(id: AccountId) {
        customAssetDao.delete(id.value)
    }
}

private fun String.toEntity(
    displayName: String,
    assetSymbol: String,
    quantity: Double,
    pricing: CustomAssetPricing,
) = CustomAssetEntity(
    id = this,
    displayName = displayName,
    assetSymbol = assetSymbol,
    quantity = quantity,
    pricingType = if (pricing is CustomAssetPricing.LiveFromCoinMarketCap) "CMC" else "FIXED",
    fixedPriceUsd = (pricing as? CustomAssetPricing.Fixed)?.unitPriceUsd,
    cmcSymbol = (pricing as? CustomAssetPricing.LiveFromCoinMarketCap)?.cmcSymbol,
)

private fun CustomAssetEntity.toDomain() =
    Account.CustomAsset(
        id = AccountId(id),
        displayName = displayName,
        assetSymbol = assetSymbol,
        quantity = quantity,
        pricing =
            cmcSymbol.let { symbol ->
                if (pricingType == "CMC" && symbol != null) {
                    CustomAssetPricing.LiveFromCoinMarketCap(symbol)
                } else {
                    CustomAssetPricing.Fixed(fixedPriceUsd ?: 0.0)
                }
            },
    )
