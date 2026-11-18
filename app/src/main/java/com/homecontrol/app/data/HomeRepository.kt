package com.homecontrol.app.data

import kotlinx.coroutines.flow.Flow

class HomeRepository(db: AppDatabase) {
    private val deviceDao = db.deviceDao()
    private val categoryDao = db.categoryDao()
    private val historyDao = db.historyDao()

    val devices: Flow<List<DeviceWithCommands>> = deviceDao.observeAll()
    val categories: Flow<List<CategoryEntity>> = categoryDao.observeAll()
    val history: Flow<List<HistoryEntity>> = historyDao.observeAll()

    suspend fun getDevice(id: Long): DeviceWithCommands? = deviceDao.get(id)

    suspend fun saveDevice(device: DeviceEntity, commands: List<CommandEntity>): Long =
        deviceDao.save(device, commands)

    suspend fun deleteDevice(id: Long) = deviceDao.delete(id)

    suspend fun moveDevice(id: Long, up: Boolean) {
        val list = deviceDao.allDevices().toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        java.util.Collections.swap(list, i, j)
        deviceDao.updateDevices(list.mapIndexed { index, d -> d.copy(sortOrder = index) })
    }

    suspend fun saveCategory(category: CategoryEntity) {
        if (category.id == 0L) {
            categoryDao.insert(category.copy(sortOrder = categoryDao.maxOrder() + 1))
        } else {
            categoryDao.update(category)
        }
    }

    suspend fun deleteCategory(id: Long) = categoryDao.remove(id)

    suspend fun addHistory(item: HistoryEntity): Long = historyDao.insert(item)
    suspend fun markSendResult(id: Long, ok: Boolean, error: String?) =
        historyDao.updateSendStatus(id, if (ok) SendStatus.SENT else SendStatus.FAILED, error)
    suspend fun markDelivery(id: Long, delivery: String) = historyDao.updateDelivery(id, delivery)
    suspend fun deleteHistory(id: Long) = historyDao.delete(id)
    suspend fun clearHistory() = historyDao.clear()

    /** Adds a few example devices so a fresh install is not empty. */
    suspend fun seedSamples() {
        val cats = categoryDao.all().associateBy { it.name }
        fun cmd(label: String, text: String, kind: String) =
            CommandEntity(deviceId = 0, label = label, smsText = text, kind = kind)

        deviceDao.save(
            DeviceEntity(name = "چراغ پذیرایی", iconKey = "light", colorHex = 0xFFFFB84D,
                categoryId = cats["روشنایی"]?.id),
            listOf(cmd("روشن", "LIGHT1_ON", CommandKind.ON), cmd("خاموش", "LIGHT1_OFF", CommandKind.OFF))
        )
        deviceDao.save(
            DeviceEntity(name = "کولر", iconKey = "ac", colorHex = 0xFF40C4FF,
                categoryId = cats["سرمایش و گرمایش"]?.id),
            listOf(cmd("روشن", "AC_ON", CommandKind.ON), cmd("خاموش", "AC_OFF", CommandKind.OFF))
        )
        deviceDao.save(
            DeviceEntity(name = "درب ورودی", iconKey = "door", colorHex = 0xFF9B6CFF,
                categoryId = cats["امنیت"]?.id),
            listOf(cmd("باز کردن", "DOOR_OPEN", CommandKind.CUSTOM))
        )
    }
}
