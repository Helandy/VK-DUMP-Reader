package com.etozhesandy.redpanda.core.settings

import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.MessageSort
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {

    val settings: Flow<AppSettings>

    suspend fun setCoilCacheSizeMb(value: Int)

    suspend fun setMediaImageWidthDp(value: Int)

    // Key and direction are written together: a screen reading them mid-write would otherwise
    // briefly see a new key paired with the previous key's direction.
    suspend fun setDefaultDialogSort(sort: DialogSort, ascending: Boolean)

    suspend fun setDefaultChatReversed(value: Boolean)

    suspend fun setDefaultMediaSort(sort: MediaSort, ascending: Boolean)

    suspend fun setDefaultSearchSort(sort: MessageSort, ascending: Boolean)
}
