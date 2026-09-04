package com.etozhesandy.redpanda.core.navigation

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations shared by every feature module. */
object Routes {

    @Serializable
    data object Home

    @Serializable
    data object Import

    @Serializable
    data object Settings

    @Serializable
    data class Profile(val profileId: String)

    @Serializable
    data class ProfileFriends(val profileId: String)

    @Serializable
    data class ProfileGroups(val profileId: String)

    @Serializable
    data class ProfileSavedPhotos(val profileId: String)

    @Serializable
    data class ProfileAttachments(val profileId: String)

    @Serializable
    data class ProfileSavedPhotoViewer(val profileId: String, val startPhotoId: String)

    @Serializable
    data class ProfileAttachmentViewer(val profileId: String, val startAttachmentId: String)

    @Serializable
    data class ProfileMedia(val profileId: String)

    @Serializable
    data class ProfileMediaFolder(val profileId: String, val folder: String)

    @Serializable
    data class ProfileMediaViewer(val profileId: String, val folder: String, val startAttachmentId: String)

    @Serializable
    data class WebView(val url: String)

    @Serializable
    data class Dialogs(val profileId: String)

    @Serializable
    data class Chat(
        val dialogId: String,
        val profileId: String,
        val scrollToMessageId: String? = null,
        /**
         * "asc" / "desc" when the user explicitly picked an order for this screen; null means the
         * default from the settings applies. A nullable String rather than a Boolean? because
         * type-safe navigation has no NavType for a nullable Boolean.
         */
        val orderOverride: String? = null,
    ) {
        companion object {
            const val ORDER_ASCENDING = "asc"
            const val ORDER_DESCENDING = "desc"
        }
    }

    /**
     * Searching a dialog is its own destination rather than a mode of [Chat]: opening a result
     * rebuilds the chat around that message, so the two screens can never share one state.
     *
     * [orderOverride] is carried through untouched so a result opens the chat in the order the
     * user was already reading it in — the search screen never interprets it.
     */
    @Serializable
    data class ChatSearch(
        val dialogId: String,
        val profileId: String,
        val orderOverride: String? = null,
    )

    @Serializable
    data class Favorites(val profileId: String)

    @Serializable
    data class PhotoViewer(val dialogId: String, val startAttachmentId: String)
}
