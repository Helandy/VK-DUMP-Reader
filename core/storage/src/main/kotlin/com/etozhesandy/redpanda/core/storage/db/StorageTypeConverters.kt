package com.etozhesandy.redpanda.core.storage.db

import androidx.room.TypeConverter
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.DialogKind
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.model.Sex
import com.etozhesandy.redpanda.core.model.SourceType

class StorageTypeConverters {

    @TypeConverter
    fun fromSourceType(value: SourceType): String = value.name

    @TypeConverter
    fun toSourceType(value: String): SourceType = SourceType.valueOf(value)

    @TypeConverter
    fun fromProfileStatus(value: ProfileStatus): String = value.name

    @TypeConverter
    fun toProfileStatus(value: String): ProfileStatus = ProfileStatus.valueOf(value)

    @TypeConverter
    fun fromDialogKind(value: DialogKind): String = value.name

    @TypeConverter
    fun toDialogKind(value: String): DialogKind = DialogKind.valueOf(value)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    /**
     * Falls back to [AttachmentType.OTHER] rather than throwing: a database written by a newer
     * build can hold a type name this one has never heard of, and one unknown row must not make
     * every attachment query crash.
     */
    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType =
        runCatching { AttachmentType.valueOf(value) }.getOrDefault(AttachmentType.OTHER)

    @TypeConverter
    fun fromSex(value: Sex): String = value.name

    @TypeConverter
    fun toSex(value: String): Sex = Sex.valueOf(value)
}
