package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "voltledger_settings")

class SettingsManager(private val context: Context) {
    companion object {
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_CURRENCY = stringPreferencesKey("currency")
        val KEY_AUTO_NIGHT_TARIFF = booleanPreferencesKey("auto_night_tariff")
        val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_NOTIFY_UNFINISHED = booleanPreferencesKey("notify_unfinished")
        val KEY_UNFINISHED_HOURS = intPreferencesKey("unfinished_hours")
        val KEY_NOTIFY_WEEKLY = booleanPreferencesKey("notify_weekly")
        val KEY_NOTIFY_MONTHLY = booleanPreferencesKey("notify_monthly")
        val KEY_SELECTED_CAR_ID = longPreferencesKey("selected_car_id")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val KEY_PIN_CODE = stringPreferencesKey("pin_code")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            language = prefs[KEY_LANGUAGE] ?: "ru",
            theme = prefs[KEY_THEME] ?: "dark",
            currency = prefs[KEY_CURRENCY] ?: "BYN",
            autoNightTariff = prefs[KEY_AUTO_NIGHT_TARIFF] ?: true,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            notifyUnfinished = prefs[KEY_NOTIFY_UNFINISHED] ?: true,
            unfinishedHoursThreshold = prefs[KEY_UNFINISHED_HOURS] ?: 4,
            notifyWeekly = prefs[KEY_NOTIFY_WEEKLY] ?: true,
            notifyMonthly = prefs[KEY_NOTIFY_MONTHLY] ?: true,
            selectedCarId = prefs[KEY_SELECTED_CAR_ID] ?: 0L,
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            pinEnabled = prefs[KEY_PIN_ENABLED] ?: false,
            pinCode = prefs[KEY_PIN_CODE] ?: ""
        )
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun updateTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun updateCurrency(currency: String) {
        context.dataStore.edit { it[KEY_CURRENCY] = currency }
    }

    suspend fun updateAutoNightTariff(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_NIGHT_TARIFF] = enabled }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun updateNotifyUnfinished(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_UNFINISHED] = enabled }
    }

    suspend fun updateUnfinishedHours(hours: Int) {
        context.dataStore.edit { it[KEY_UNFINISHED_HOURS] = hours }
    }

    suspend fun updateNotifyWeekly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_WEEKLY] = enabled }
    }

    suspend fun updateNotifyMonthly(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_MONTHLY] = enabled }
    }

    suspend fun updateSelectedCarId(carId: Long) {
        context.dataStore.edit { it[KEY_SELECTED_CAR_ID] = carId }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun updatePinSettings(enabled: Boolean, pin: String) {
        context.dataStore.edit {
            it[KEY_PIN_ENABLED] = enabled
            it[KEY_PIN_CODE] = pin
        }
    }
}
