package com.etozhesandy.redpanda.features.profile.domain.repository

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.features.profile.domain.model.ArchiveFolder
import kotlinx.coroutines.flow.Flow

interface ProfileInfoRepository {
    fun observeProfile(profileId: String): Flow<Profile?>
    fun observeFriends(profileId: String): Flow<List<Friend>>
    fun observeGroups(profileId: String): Flow<List<Group>>
    fun observeSavedPhotos(profileId: String): Flow<List<SavedPhoto>>
    fun observeAttachments(profileId: String): Flow<List<Attachment>>
    fun observeArchiveFiles(profileId: String): Flow<List<Attachment>>
    fun observeArchiveFolders(profileId: String): Flow<List<ArchiveFolder>>
    fun observeArchiveFilesInFolder(profileId: String, folder: String): Flow<List<Attachment>>
}
