package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Moves favorites out of `messages` and into `favorite_messages`.
 *
 * The stars already set are not carried over. They were a flag on the table a chat pages out of,
 * which is the whole reason this migration exists, and re-marking a handful of messages costs the
 * reader less than the code to move them would cost here.
 *
 * `messages` is rebuilt rather than altered: `ALTER TABLE ... DROP COLUMN` needs SQLite 3.35 and
 * `minSdk 24` ships far older than that. Row ids are copied verbatim, because `messages_fts` is an
 * external-content table keyed by them; Room drops the FTS sync triggers around a migration, so
 * neither dropping the old table nor filling the new one disturbs the search index.
 */
internal val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_messages` (`messageId` TEXT NOT NULL, " +
                "`profileId` TEXT NOT NULL, `dialogId` TEXT NOT NULL, PRIMARY KEY(`messageId`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_favorite_messages_profileId` ON `favorite_messages` (`profileId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_favorite_messages_dialogId` ON `favorite_messages` (`dialogId`)",
        )

        connection.execSQL(
            "CREATE TABLE `_new_messages` (`rowId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`messageId` TEXT NOT NULL, `dialogId` TEXT NOT NULL, `profileId` TEXT NOT NULL, " +
                "`senderId` TEXT NOT NULL, `senderName` TEXT NOT NULL, `timestampEpoch` INTEGER NOT NULL, " +
                "`text` TEXT NOT NULL, `isOutgoing` INTEGER NOT NULL, `hasAttachments` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "INSERT INTO `_new_messages` (`rowId`, `messageId`, `dialogId`, `profileId`, `senderId`, " +
                "`senderName`, `timestampEpoch`, `text`, `isOutgoing`, `hasAttachments`) " +
                "SELECT `rowId`, `messageId`, `dialogId`, `profileId`, `senderId`, `senderName`, " +
                "`timestampEpoch`, `text`, `isOutgoing`, `hasAttachments` FROM `messages`",
        )
        connection.execSQL("DROP TABLE `messages`")
        connection.execSQL("ALTER TABLE `_new_messages` RENAME TO `messages`")

        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_dialogId` ON `messages` (`dialogId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_messages_profileId` ON `messages` (`profileId`)")
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_messages_messageId` ON `messages` (`messageId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_messages_dialogId_timestampEpoch` ON `messages` " +
                "(`dialogId`, `timestampEpoch`)",
        )
    }
}
