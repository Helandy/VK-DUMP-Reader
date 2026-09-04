package com.etozhesandy.redpanda.core.storage.db.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.etozhesandy.redpanda.core.model.AttachmentType
import kotlinx.coroutines.flow.Flow

/**
 * Type filters are bound as parameters rather than written as SQL literals: a literal `'PHOTO'`
 * silently stops matching if the constant is ever renamed, whereas passing [AttachmentType] values
 * makes that a compile error. Both filtered queries are covered by the composite `(scope, type,
 * timestampEpoch)` indices on [AttachmentEntity], so the database does the merging and ordering.
 */
@Dao
interface AttachmentDao {

    /**
     * Read once rather than observed: a page of messages maps its attachments while it is being
     * built, and an archive is read-only once imported, so there is nothing to watch for.
     */
    @Query("SELECT * FROM attachments WHERE messageId = :messageId ORDER BY orderInMessage ASC")
    suspend fun getAttachmentsForMessage(messageId: String): List<AttachmentEntity>

    @Query(
        "SELECT * FROM attachments WHERE dialogId = :dialogId AND type IN (:types) " +
            "ORDER BY timestampEpoch ASC, orderInMessage ASC",
    )
    fun observeByTypesForDialog(dialogId: String, types: List<AttachmentType>): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE profileId = :profileId AND type IN (:types) ORDER BY timestampEpoch DESC")
    fun observeByTypesForProfile(profileId: String, types: List<AttachmentType>): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE profileId = :profileId AND sourceFolder IS NOT NULL ORDER BY sourceFolder ASC, timestampEpoch DESC")
    fun observeArchiveFilesForProfile(profileId: String): Flow<List<AttachmentEntity>>

    @Query(
        "SELECT * FROM attachments WHERE profileId = :profileId AND sourceFolder = :folder " +
            "ORDER BY timestampEpoch DESC",
    )
    fun observeArchiveFilesInFolder(profileId: String, folder: String): Flow<List<AttachmentEntity>>

    /**
     * Per-folder counts and a preview item, so the folder list doesn't have to load every file in
     * the profile just to render a grid of summaries.
     */
    // `path` is a bare column beside a single MAX(): SQLite guarantees it comes from the row that
    // MAX picked, which makes the newest file in each folder its preview without a subquery.
    @Query(
        "SELECT sourceFolder AS folder, COUNT(*) AS fileCount, path AS previewPath, type AS previewType, " +
            "MAX(timestampEpoch) AS latestTimestampEpoch FROM attachments " +
            "WHERE profileId = :profileId AND sourceFolder IS NOT NULL " +
            "GROUP BY sourceFolder ORDER BY sourceFolder ASC",
    )
    fun observeArchiveFolders(profileId: String): Flow<List<ArchiveFolderSummary>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
