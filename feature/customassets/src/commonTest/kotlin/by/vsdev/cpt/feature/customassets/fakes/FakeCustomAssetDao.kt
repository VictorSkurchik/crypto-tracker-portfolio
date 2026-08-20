package by.vsdev.cpt.feature.customassets.fakes

import by.vsdev.cpt.core.database.dao.CustomAssetDao
import by.vsdev.cpt.core.database.entity.CustomAssetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

// CustomAssetsRepository is a concrete class, so its Room DAO is faked here instead of itself.
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
