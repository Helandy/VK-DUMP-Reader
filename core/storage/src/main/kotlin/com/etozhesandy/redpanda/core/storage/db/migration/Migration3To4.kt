package com.etozhesandy.redpanda.core.storage.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/** Adds the `(profileId, sourceFolder, timestampEpoch)` index the archive-folder screens read by. */
internal val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_attachments_profileId_sourceFolder_timestampEpoch` " +
                "ON `attachments` (`profileId`, `sourceFolder`, `timestampEpoch`)",
        )
    }
}
