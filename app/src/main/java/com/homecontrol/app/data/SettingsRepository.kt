package com.homecontrol.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "homecontrol_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val phone: String = "",
    val countryCode: String = "+98",
    val theme: ThemeMode = ThemeMode.DARK,
    val sound: Boolean = true,
    val smsExplained: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val PHONE = stringPreferencesKey("phone")
        val COUNTRY = stringPreferencesKey("country_code")
        val THEME = stringPreferencesKey("theme")
        val SOUND = booleanPreferencesKey("sound")
        val EXPLAINED = booleanPreferencesKey("sms_explained")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p: Preferences ->
            AppSettings(
                phone = p[Keys.PHONE] ?: "",
                countryCode = p[Keys.COUNTRY] ?: "+98",
                theme = runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "DARK") }
                    .getOrDefault(ThemeMode.DARK),
                sound = p[Keys.SOUND] ?: true,
                smsExplained = p[Keys.EXPLAINED] ?: false
            )
        }

    suspend fun setPhone(value: String) { context.dataStore.edit { it[Keys.PHONE] = value } }
    suspend fun setCountryCode(value: String) { context.dataStore.edit { it[Keys.COUNTRY] = value } }
    suspend fun setTheme(value: ThemeMode) { context.dataStore.edit { it[Keys.THEME] = value.name } }
    suspend fun setSound(value: Boolean) { context.dataStore.edit { it[Keys.SOUND] = value } }
    suspend fun setSmsExplained(value: Boolean) { context.dataStore.edit { it[Keys.EXPLAINED] = value } }
}
