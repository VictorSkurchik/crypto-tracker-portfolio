package by.vsdev.cpt.feature.customassets.fakes

import by.vsdev.cpt.core.database.dao.CustomAssetDao
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory fake for the Room DAO `CustomAssetsRepository` depends on. `CustomAssetsRepository` is
 * a concrete class, not an interface, so testing `CustomAssetsViewModel` means faking the
 * repository's own dependency rather than the repository itself — mirroring the pattern
 * `:feature:portfolio` uses for `PortfolioRepository` one level up.
 */
class FakeCustomAssetDao : CustomAssetDao {
    private val state = MutableStateFlow<List<CustomAssetEntity>>(emptyList())

    override fun observeAll(): Flow<List<CustomAssetEntity>> = state

    override suspend fun upsert(asset: CustomAssetEntity) {
        state.update { it.filterNot { existing -> existing.id == asset.id } + asset }
    }

    override suspend fun delete(id: String) {
        state.update { it.filterNot { existing -> existing.id == id } }
    }
}
