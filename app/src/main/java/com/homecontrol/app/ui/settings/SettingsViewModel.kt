package com.homecontrol.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homecontrol.app.AppContainer
import com.homecontrol.app.data.AppSettings
import com.homecontrol.app.data.ThemeMode
import com.homecontrol.app.util.PhoneUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(private val c: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = c.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { c.settings.setTheme(mode) } }
    fun setSound(on: Boolean) { viewModelScope.launch { c.settings.setSound(on) } }

    fun setCountry(code: String) { viewModelScope.launch { c.settings.setCountryCode(code) } }

    /** Validates and stores the main destination number. */
    fun savePhone(raw: String, countryCode: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val normalized = PhoneUtils.normalize(raw, countryCode)
            if (normalized == null) {
                onResult(false)
            } else {
                c.settings.setCountryCode(countryCode)
                c.settings.setPhone(normalized)
                onResult(true)
                _messages.emit("شماره مقصد ذخیره شد")
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            c.repository.clearHistory()
            _messages.emit("تاریخچه پاک شد")
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val json = c.backup.export()
                withContext(Dispatchers.IO) {
                    val out = c.appContext.contentResolver.openOutputStream(uri, "wt")
                        ?: error("cannot open output stream")
                    out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            _messages.emit(if (ok) "فایل پشتیبان ذخیره شد" else "ذخیره فایل پشتیبان ناموفق بود")
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val ok = runCatching {
                val text = withContext(Dispatchers.IO) {
                    val input = c.appContext.contentResolver.openInputStream(uri)
                        ?: error("cannot open input stream")
                    input.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                c.backup.import(text)
            }.isSuccess
            _messages.emit(if (ok) "اطلاعات بازیابی شد" else "فایل پشتیبان معتبر نیست؛ چیزی تغییر نکرد")
        }
    }
}
