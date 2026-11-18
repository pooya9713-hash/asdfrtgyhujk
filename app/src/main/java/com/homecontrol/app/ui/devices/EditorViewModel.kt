package com.homecontrol.app.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.CategoryEntity
import com.homecontrol.app.data.CommandEntity
import com.homecontrol.app.data.CommandKind
import com.homecontrol.app.data.DeviceEntity
import com.homecontrol.app.ui.components.DeviceColors
import com.homecontrol.app.ui.components.DeviceIcons
import com.homecontrol.app.util.PhoneUtils
import com.homecontrol.app.util.SmsUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CommandDraft(
    val key: Long,
    val label: String = "",
    val text: String = "",
    val kind: String = CommandKind.CUSTOM
)

data class EditorState(
    val loading: Boolean = true,
    val name: String = "",
    val iconKey: String = DeviceIcons.all.first().key,
    val color: Long = DeviceColors.first(),
    val categoryId: Long? = null,
    val useMainNumber: Boolean = true,
    val destNumber: String = "",
    val commands: List<CommandDraft> = emptyList(),
    val error: String? = null,
    val saved: Boolean = false
)

class EditorViewModel(private val c: AppContainer, private val deviceId: Long) : ViewModel() {

    val isNew: Boolean = deviceId == 0L

    private var nextKey = 1L
    private var sortOrder = 0
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = c.repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (deviceId == 0L) {
                _state.value = EditorState(
                    loading = false,
                    commands = listOf(
                        CommandDraft(nextKey++, "روشن", "", CommandKind.ON),
                        CommandDraft(nextKey++, "خاموش", "", CommandKind.OFF)
                    )
                )
            } else {
                val d = c.repository.getDevice(deviceId)
                if (d == null) {
                    _state.value = EditorState(loading = false, error = "وسیله پیدا نشد")
                } else {
                    sortOrder = d.device.sortOrder
                    _state.value = EditorState(
                        loading = false,
                        name = d.device.name,
                        iconKey = d.device.iconKey,
                        color = d.device.colorHex,
                        categoryId = d.device.categoryId,
                        useMainNumber = d.device.destNumber.isNullOrBlank(),
                        destNumber = d.device.destNumber ?: "",
                        commands = d.commands.sortedBy { it.sortOrder }
                            .map { CommandDraft(nextKey++, it.label, it.smsText, it.kind) }
                    )
                }
            }
        }
    }

    private fun edit(block: (EditorState) -> EditorState) {
        _state.update { block(it).copy(error = null) }
    }

    fun setName(v: String) = edit { it.copy(name = v) }
    fun setIcon(v: String) = edit { it.copy(iconKey = v) }
    fun setColor(v: Long) = edit { it.copy(color = v) }
    fun setCategory(v: Long?) = edit { it.copy(categoryId = v) }
    fun setUseMainNumber(v: Boolean) = edit { it.copy(useMainNumber = v) }
    fun setDest(v: String) = edit { it.copy(destNumber = v) }

    fun updateCommand(draft: CommandDraft) = edit { s ->
        s.copy(commands = s.commands.map { if (it.key == draft.key) draft else it })
    }

    fun addCommand() = edit { s ->
        s.copy(commands = s.commands + CommandDraft(nextKey++, "", "", CommandKind.CUSTOM))
    }

    fun removeCommand(key: Long) = edit { s -> s.copy(commands = s.commands.filterNot { it.key == key }) }

    private fun fail(message: String) { _state.update { it.copy(error = message) } }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            val name = s.name.trim()
            if (name.isEmpty()) { fail("نام وسیله را وارد کنید"); return@launch }
            if (s.commands.isEmpty()) { fail("حداقل یک فرمان لازم است"); return@launch }

            val settings = c.settings.settings.first()
            val dest: String? = if (s.useMainNumber) {
                null
            } else {
                PhoneUtils.normalize(s.destNumber, settings.countryCode)
                    ?: run { fail("شماره مقصد اختصاصی نامعتبر است"); return@launch }
            }

            val commands = mutableListOf<CommandEntity>()
            for ((index, d) in s.commands.withIndex()) {
                val text = d.text.trim()
                if (text.isEmpty()) { fail("متن پیامک فرمان ${index + 1} خالی است"); return@launch }
                if (!SmsUtils.fitsOneSms(text)) {
                    fail("متن فرمان ${index + 1} بیشتر از یک پیامک است؛ کوتاه‌ترش کنید")
                    return@launch
                }
                val label = d.label.trim().ifEmpty {
                    when (d.kind) {
                        CommandKind.ON -> "روشن"
                        CommandKind.OFF -> "خاموش"
                        else -> "ارسال"
                    }
                }
                commands += CommandEntity(deviceId = deviceId, label = label, smsText = text, kind = d.kind)
            }

            c.repository.saveDevice(
                DeviceEntity(
                    id = deviceId,
                    name = name,
                    iconKey = s.iconKey,
                    colorHex = s.color,
                    categoryId = s.categoryId,
                    destNumber = dest,
                    sortOrder = sortOrder
                ),
                commands
            )
            _state.update { it.copy(saved = true) }
        }
    }
}
