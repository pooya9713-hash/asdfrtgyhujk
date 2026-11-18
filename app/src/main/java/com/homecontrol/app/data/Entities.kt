package com.homecontrol.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

object CommandKind {
    const val ON = "ON"
    const val OFF = "OFF"
    const val CUSTOM = "CUSTOM"
}

object SendStatus {
    const val PENDING = "PENDING"
    const val SENT = "SENT"
    const val FAILED = "FAILED"
}

object DeliveryStatus {
    const val NONE = "NONE"
    const val DELIVERED = "DELIVERED"
    const val DELIVERY_FAILED = "DELIVERY_FAILED"
}

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "devices", indices = [Index("categoryId")])
data class DeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val categoryId: Long? = null,
    val destNumber: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "commands",
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["id"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("deviceId")]
)
data class CommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: Long,
    val label: String,
    val smsText: String,
    val kind: String = CommandKind.CUSTOM,
    val sortOrder: Int = 0
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceName: String,
    val commandLabel: String,
    val smsText: String,
    val number: String,
    val timestamp: Long,
    val status: String = SendStatus.PENDING,
    val error: String? = null,
    val delivery: String = DeliveryStatus.NONE
)

data class DeviceWithCommands(
    @Embedded val device: DeviceEntity,
    @Relation(parentColumn = "id", entityColumn = "deviceId")
    val commands: List<CommandEntity>
)
