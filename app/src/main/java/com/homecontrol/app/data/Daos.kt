package com.homecontrol.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class DeviceDao {
    @Transaction
    @Query("SELECT * FROM devices ORDER BY sortOrder, id")
    abstract fun observeAll(): Flow<List<DeviceWithCommands>>

    @Transaction
    @Query("SELECT * FROM devices ORDER BY sortOrder, id")
    abstract suspend fun allWithCommands(): List<DeviceWithCommands>

    @Query("SELECT * FROM devices ORDER BY sortOrder, id")
    abstract suspend fun allDevices(): List<DeviceEntity>

    @Transaction
    @Query("SELECT * FROM devices WHERE id = :id")
    abstract suspend fun get(id: Long): DeviceWithCommands?

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM devices")
    abstract suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDevice(device: DeviceEntity): Long

    @Update
    abstract suspend fun updateDevice(device: DeviceEntity)

    @Update
    abstract suspend fun updateDevices(devices: List<DeviceEntity>)

    @Insert
    abstract suspend fun insertCommands(commands: List<CommandEntity>)

    @Query("DELETE FROM commands WHERE deviceId = :deviceId")
    abstract suspend fun deleteCommandsOf(deviceId: Long)

    @Query("DELETE FROM devices WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM devices")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun save(device: DeviceEntity, commands: List<CommandEntity>): Long {
        val id = if (device.id == 0L) {
            insertDevice(device.copy(sortOrder = maxOrder() + 1))
        } else {
            updateDevice(device)
            deleteCommandsOf(device.id)
            device.id
        }
        insertCommands(commands.mapIndexed { i, c -> c.copy(id = 0, deviceId = id, sortOrder = i) })
        return id
    }
}

@Dao
abstract class CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    abstract fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder, id")
    abstract suspend fun all(): List<CategoryEntity>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    abstract suspend fun maxOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(category: CategoryEntity): Long

    @Update
    abstract suspend fun update(category: CategoryEntity)

    @Query("UPDATE devices SET categoryId = NULL WHERE categoryId = :id")
    abstract suspend fun detachDevices(id: Long)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    @Query("DELETE FROM categories")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun remove(id: Long) {
        detachDevices(id)
        deleteById(id)
    }
}

@Dao
abstract class HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC, id DESC")
    abstract fun observeAll(): Flow<List<HistoryEntity>>

    @Insert
    abstract suspend fun insert(item: HistoryEntity): Long

    // Only a PENDING record can change; a late callback never overwrites a final status.
    @Query("UPDATE history SET status = :status, error = :error WHERE id = :id AND status = 'PENDING'")
    abstract suspend fun updateSendStatus(id: Long, status: String, error: String?)

    @Query("UPDATE history SET delivery = :delivery WHERE id = :id")
    abstract suspend fun updateDelivery(id: Long, delivery: String)

    @Query("DELETE FROM history WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("DELETE FROM history")
    abstract suspend fun clear()
}
