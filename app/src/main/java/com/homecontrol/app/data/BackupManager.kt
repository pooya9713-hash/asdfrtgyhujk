package com.homecontrol.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Exports/imports devices, categories and settings as a JSON document. History is not included. */
class BackupManager(
    private val db: AppDatabase,
    private val settings: SettingsRepository
) {
    suspend fun export(): String {
        val s = settings.settings.first()
        val root = JSONObject()
            .put("app", "HomeControl")
            .put("version", 1)
            .put(
                "settings",
                JSONObject()
                    .put("phone", s.phone)
                    .put("countryCode", s.countryCode)
                    .put("theme", s.theme.name)
                    .put("sound", s.sound)
            )

        val cats = JSONArray()
        db.categoryDao().all().forEach {
            cats.put(JSONObject().put("id", it.id).put("name", it.name).put("sortOrder", it.sortOrder))
        }
        root.put("categories", cats)

        val devices = JSONArray()
        db.deviceDao().allWithCommands().forEach { dw ->
            val cmds = JSONArray()
            dw.commands.sortedBy { it.sortOrder }.forEach { c ->
                cmds.put(
                    JSONObject().put("label", c.label).put("smsText", c.smsText)
                        .put("kind", c.kind).put("sortOrder", c.sortOrder)
                )
            }
            devices.put(
                JSONObject()
                    .put("id", dw.device.id)
                    .put("name", dw.device.name)
                    .put("iconKey", dw.device.iconKey)
                    .put("colorHex", dw.device.colorHex)
                    .put("categoryId", dw.device.categoryId ?: JSONObject.NULL)
                    .put("destNumber", dw.device.destNumber ?: JSONObject.NULL)
                    .put("sortOrder", dw.device.sortOrder)
                    .put("commands", cmds)
            )
        }
        root.put("devices", devices)
        return root.toString(2)
    }

    /** Throws if the file is not a valid HomeControl backup; nothing is changed in that case. */
    suspend fun import(json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "HomeControl") { "not a HomeControl backup" }

        val categories = mutableListOf<CategoryEntity>()
        val catArr = root.getJSONArray("categories")
        for (i in 0 until catArr.length()) {
            val o = catArr.getJSONObject(i)
            categories += CategoryEntity(o.getLong("id"), o.getString("name"), o.optInt("sortOrder", i))
        }

        val devices = mutableListOf<Pair<DeviceEntity, List<CommandEntity>>>()
        val devArr = root.getJSONArray("devices")
        for (i in 0 until devArr.length()) {
            val o = devArr.getJSONObject(i)
            val id = o.getLong("id")
            val device = DeviceEntity(
                id = id,
                name = o.getString("name"),
                iconKey = o.getString("iconKey"),
                colorHex = o.getLong("colorHex"),
                categoryId = if (o.isNull("categoryId")) null else o.getLong("categoryId"),
                destNumber = if (o.isNull("destNumber")) null else o.getString("destNumber"),
                sortOrder = o.optInt("sortOrder", i)
            )
            val cmdArr = o.getJSONArray("commands")
            val cmds = (0 until cmdArr.length()).map { k ->
                val c = cmdArr.getJSONObject(k)
                CommandEntity(
                    deviceId = id,
                    label = c.getString("label"),
                    smsText = c.getString("smsText"),
                    kind = c.optString("kind", CommandKind.CUSTOM),
                    sortOrder = c.optInt("sortOrder", k)
                )
            }
            devices += device to cmds
        }

        db.withTransaction {
            db.deviceDao().deleteAll()
            db.categoryDao().deleteAll()
            categories.forEach { db.categoryDao().insert(it) }
            devices.forEach { (device, cmds) ->
                db.deviceDao().insertDevice(device)
                db.deviceDao().insertCommands(cmds)
            }
        }

        root.optJSONObject("settings")?.let { st ->
            settings.setPhone(st.optString("phone", ""))
            settings.setCountryCode(st.optString("countryCode", "+98"))
            runCatching { ThemeMode.valueOf(st.optString("theme", "DARK")) }
                .getOrNull()?.let { settings.setTheme(it) }
            settings.setSound(st.optBoolean("sound", true))
        }
    }
}
