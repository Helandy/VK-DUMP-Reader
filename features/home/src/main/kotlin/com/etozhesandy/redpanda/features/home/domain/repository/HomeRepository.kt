package com.etozhesandy.redpanda.features.home.domain.repository

import com.etozhesandy.redpanda.core.model.Profile
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeProfiles(): Flow<List<Profile>>
    fun observeImportRunning(): Flow<Boolean>
    suspend fun deleteProfile(profileId: String)
}
