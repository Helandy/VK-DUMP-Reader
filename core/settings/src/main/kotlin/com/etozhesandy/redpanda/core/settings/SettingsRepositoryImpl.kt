package com.etozhesandy.redpanda.core.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.MessageSort
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppSettings(
                coilCacheSizeMb = preferences[COIL_CACHE_SIZE_MB_KEY] ?: AppSettings.DEFAULT_COIL_CACHE_SIZE_MB,
                mediaImageWidthDp = preferences[MEDIA_IMAGE_WIDTH_DP_KEY] ?: AppSettings.DEFAULT_MEDIA_IMAGE_WIDTH_DP,
                defaultDialogSort = preferences[DIALOG_SORT_KEY].toEnum(AppSettings.DEFAULT_DIALOG_SORT),
                defaultDialogSortAscending = preferences[DIALOG_SORT_ASCENDING_KEY]
                    ?: AppSettings.DEFAULT_DIALOG_SORT_ASCENDING,
                defaultChatReversed = preferences[CHAT_REVERSED_KEY] ?: AppSettings.DEFAULT_CHAT_REVERSED,
                defaultMediaSort = preferences[MEDIA_SORT_KEY].toEnum(AppSettings.DEFAULT_MEDIA_SORT),
                defaultMediaSortAscending = preferences[MEDIA_SORT_ASCENDING_KEY]
                    ?: AppSettings.DEFAULT_MEDIA_SORT_ASCENDING,
                defaultSearchSort = preferences[SEARCH_SORT_KEY].toEnum(AppSettings.DEFAULT_SEARCH_SORT),
                defaultSearchSortAscending = preferences[SEARCH_SORT_ASCENDING_KEY]
                    ?: AppSettings.DEFAULT_SEARCH_SORT_ASCENDING,
            )
        }

    override suspend fun setCoilCacheSizeMb(value: Int) {
        dataStore.edit { preferences -> preferences[COIL_CACHE_SIZE_MB_KEY] = value }
    }

    override suspend fun setMediaImageWidthDp(value: Int) {
        dataStore.edit { preferences -> preferences[MEDIA_IMAGE_WIDTH_DP_KEY] = value }
    }

    override suspend fun setDefaultDialogSort(sort: DialogSort, ascending: Boolean) {
        dataStore.edit { preferences ->
            preferences[DIALOG_SORT_KEY] = sort.name
            preferences[DIALOG_SORT_ASCENDING_KEY] = ascending
        }
    }

    override suspend fun setDefaultChatReversed(value: Boolean) {
        dataStore.edit { preferences -> preferences[CHAT_REVERSED_KEY] = value }
    }

    override suspend fun setDefaultMediaSort(sort: MediaSort, ascending: Boolean) {
        dataStore.edit { preferences ->
            preferences[MEDIA_SORT_KEY] = sort.name
            preferences[MEDIA_SORT_ASCENDING_KEY] = ascending
        }
    }

    override suspend fun setDefaultSearchSort(sort: MessageSort, ascending: Boolean) {
        dataStore.edit { preferences ->
            preferences[SEARCH_SORT_KEY] = sort.name
            preferences[SEARCH_SORT_ASCENDING_KEY] = ascending
        }
    }

    /** Falls back to [default] for a name no longer in the enum (an option renamed between builds). */
    private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default

    private companion object {
        val COIL_CACHE_SIZE_MB_KEY = intPreferencesKey("coil_cache_size_mb")
        val MEDIA_IMAGE_WIDTH_DP_KEY = intPreferencesKey("media_image_width_dp")
        val DIALOG_SORT_KEY = stringPreferencesKey("default_dialog_sort")
        val DIALOG_SORT_ASCENDING_KEY = booleanPreferencesKey("default_dialog_sort_ascending")
        val CHAT_REVERSED_KEY = booleanPreferencesKey("default_chat_reversed")
        val MEDIA_SORT_KEY = stringPreferencesKey("default_media_sort")
        val MEDIA_SORT_ASCENDING_KEY = booleanPreferencesKey("default_media_sort_ascending")
        val SEARCH_SORT_KEY = stringPreferencesKey("default_search_sort")
        val SEARCH_SORT_ASCENDING_KEY = booleanPreferencesKey("default_search_sort_ascending")
    }
}
