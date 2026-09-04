package com.etozhesandy.redpanda.core.archive.parse

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.ProfileDetails
import com.etozhesandy.redpanda.core.model.SavedPhoto

/**
 * Streaming destination for parsed archive data. A single dialog's history can run into the
 * hundreds of thousands of messages, so parsers flush in bounded batches instead of building one
 * giant in-memory list — keeping peak memory roughly constant regardless of archive size.
 *
 * [onProfileDetails]/[onFriends]/[onGroups]/[onSavedPhotos] default to a no-op since only richer
 * export formats (currently the VK API-dump layout) carry this data.
 */
interface ParseSink {
    suspend fun onDisplayName(name: String)

    /**
     * Number of dialogs the parser found in the archive, reported once before any of them is
     * parsed so progress can be shown as "N of M" — [onDialog] then marks each one finished.
     */
    suspend fun onDialogsDiscovered(total: Int) {}

    suspend fun onDialog(dialog: ChatDialog)
    suspend fun onMessages(batch: List<Message>)
    suspend fun onAttachments(batch: List<Attachment>)
    suspend fun onProfileDetails(details: ProfileDetails) {}
    suspend fun onFriends(batch: List<Friend>) {}
    suspend fun onGroups(batch: List<Group>) {}
    suspend fun onSavedPhotos(batch: List<SavedPhoto>) {}
}
