package com.homecontrol.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.CategoryEntity
import com.homecontrol.app.data.CommandEntity
import com.homecontrol.app.data.DeviceEntity
import com.homecontrol.app.data.DeviceWithCommands
import com.homecontrol.app.data.HistoryEntity
import com.homecontrol.app.data.SendStatus
import com.homecontrol.app.util.PhoneUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ReadyState { READY, NO_NUMBER, NO_SIM }

enum class EventAction(val label: String) {
    OPEN_SETTINGS("تنظیمات"),
    OPEN_APP_SETTINGS("تنظیمات")
}

data class UiEvent(val text: String, val action: EventAction? = null)

data class PendingSend(val device: DeviceEntity, val command: CommandEntity, val number: String)

data class HomeUiState(
    val devices: List<DeviceWithCommands> = emptyList(),
    val totalDevices: Int = 0,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategory: Long? = null,
    val destination: String = "",
    val ready: ReadyState = ReadyState.NO_NUMBER,
    val sound: Boolean = true,
    val smsExplained: Boolean = false
)

class HomeViewModel(private val c: AppContainer) : ViewModel() {

    private val selected = MutableStateFlow<Long?>(null)
    private val simReady = MutableStateFlow(c.smsSender.isSimReady())
    private val lastSent = mutableMapOf<Long, Long>()

    /** The command waiting for the user's confirmation. */
    val pending = MutableStateFlow<PendingSend?>(null)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    val state: StateFlow<HomeUiState> = combine(
        c.repository.devices,
        c.repository.categories,
        c.settings.settings,
        selected,
        simReady
    ) { devices, categories, settings, sel, sim ->
        val validSel = sel?.takeIf { id -> categories.any { it.id == id } }
        val dest = PhoneUtils.normalize(settings.phone, settings.countryCode)
        HomeUiState(
            devices = if (validSel == null) devices else devices.filter { it.device.categoryId == validSel },
            totalDevices = devices.size,
            categories = categories,
            selectedCategory = validSel,
            destination = dest ?: "",
            ready = when {
                dest == null -> ReadyState.NO_NUMBER
                !sim -> ReadyState.NO_SIM
                else -> ReadyState.READY
            },
            sound = settings.sound,
            smsExplained = settings.smsExplained
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectCategory(id: Long?) { selected.value = id }

    fun refreshSim() { simReady.value = c.smsSender.isSimReady() }

    /** Step 1: the user pressed a button. Resolve the destination and ask for confirmation. */
    fun prepare(device: DeviceEntity, command: CommandEntity) {
        viewModelScope.launch {
            val s = c.settings.settings.first()
            val raw = device.destNumber?.takeIf { it.isNotBlank() } ?: s.phone
            val number = PhoneUtils.normalize(raw, s.countryCode)
            if (number == null) {
                _events.emit(UiEvent("شماره مقصد ثبت نشده یا نامعتبر است", EventAction.OPEN_SETTINGS))
            } else {
                pending.value = PendingSend(device, command, number)
            }
        }
    }

    fun cancelPending() { pending.value = null }

    fun markExplained() {
        viewModelScope.launch { c.settings.setSmsExplained(true) }
    }

    fun denyPermission() {
        pending.value = null
        _events.tryEmit(UiEvent("بدون مجوز ارسال پیامک، برنامه نمی‌تواند فرمان بفرستد", EventAction.OPEN_APP_SETTINGS))
    }

    /** Step 2: the user confirmed (and the SMS permission is granted). */
    fun confirmSend() {
        val p = pending.value ?: return
        pending.value = null
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val last = lastSent[p.command.id] ?: 0L
            if (now - last < DEBOUNCE_MS) {
                _events.emit(UiEvent("این فرمان همین الان ارسال شد؛ چند ثانیه صبر کنید"))
                return@launch
            }
            lastSent[p.command.id] = now

            val historyId = c.repository.addHistory(
                HistoryEntity(
                    deviceName = p.device.name,
                    commandLabel = p.command.label,
                    smsText = p.command.smsText,
                    number = p.number,
                    timestamp = now,
                    status = SendStatus.PENDING
                )
            )

            if (!c.smsSender.isSimReady()) {
                simReady.value = false
                c.repository.markSendResult(historyId, false, "سیم‌کارت یافت نشد")
                _events.emit(UiEvent("سیم‌کارت یافت نشد یا آماده نیست؛ پیامک ارسال نشد"))
                return@launch
            }
            try {
                c.smsSender.send(p.number, p.command.smsText, historyId)
                _events.emit(UiEvent("پیامک به سرویس ارسال سپرده شد؛ نتیجه در تاریخچه ثبت می‌شود"))
            } catch (e: Exception) {
                c.repository.markSendResult(historyId, false, e.message ?: "خطای ناشناخته")
                _events.emit(UiEvent("ارسال پیامک ناموفق بود"))
            }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 3_000L
    }
}
