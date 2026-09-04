package com.etozhesandy.redpanda.features.dialogs.domain.repository

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Profile
import kotlinx.coroutines.flow.Flow

interface DialogsRepository {
    fun observeDialogs(profileId: String, query: String?, category: String?): Flow<List<ChatDialog>>
    fun observeCategories(profileId: String): Flow<List<String>>
    fun observeProfile(profileId: String): Flow<Profile?>
}
