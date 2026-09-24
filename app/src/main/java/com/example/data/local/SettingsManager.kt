package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
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
        val KEY_NOTIFY_ACHIEVEMENTS = booleanPreferencesKey("notify_achievements")
        val KEY_SELECTED_CAR_ID = longPreferencesKey("selected_car_id")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        // Home Charging Tariff Preferences
        val KEY_HOME_CHARGE_CONFIGURED = booleanPreferencesKey("home_charge_configured")
        val KEY_HOME_STANDARD_PRICE = doublePreferencesKey("home_standard_price")
        val KEY_HOME_NIGHT_TARIFF_ENABLED = booleanPreferencesKey("home_night_tariff_enabled")
        val KEY_HOME_NIGHT_PRICE = doublePreferencesKey("home_night_price")
        val KEY_HOME_NIGHT_START_HOUR = intPreferencesKey("home_night_start_hour")
        val KEY_HOME_NIGHT_END_HOUR = intPreferencesKey("home_night_end_hour")
        val KEY_HOME_THREE_TARIFF_ENABLED = booleanPreferencesKey("home_three_tariff_enabled")
        val KEY_HOME_PEAK_PRICE = doublePreferencesKey("home_peak_price")
        val KEY_HOME_PEAK_START_HOUR = intPreferencesKey("home_peak_start_hour")
        val KEY_HOME_PEAK_END_HOUR = intPreferencesKey("home_peak_end_hour")
        val KEY_HOME_SEMI_PEAK_PRICE = doublePreferencesKey("home_semi_peak_price")
        val KEY_HOME_SEMI_PEAK_START_HOUR = intPreferencesKey("home_semi_peak_start_hour")
        val KEY_HOME_SEMI_PEAK_END_HOUR = intPreferencesKey("home_semi_peak_end_hour")
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
            notifyAchievements = prefs[KEY_NOTIFY_ACHIEVEMENTS] ?: true,
            selectedCarId = prefs[KEY_SELECTED_CAR_ID] ?: 0L,
            onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false,
            homeChargeConfigured = prefs[KEY_HOME_CHARGE_CONFIGURED] ?: false,
            homeStandardPrice = prefs[KEY_HOME_STANDARD_PRICE] ?: 0.36,
            homeNightTariffEnabled = prefs[KEY_HOME_NIGHT_TARIFF_ENABLED] ?: false,
            homeNightPrice = prefs[KEY_HOME_NIGHT_PRICE] ?: 0.1822,
            homeNightStartHour = prefs[KEY_HOME_NIGHT_START_HOUR] ?: 23,
            homeNightEndHour = prefs[KEY_HOME_NIGHT_END_HOUR] ?: 6,
            homeThreeTariffEnabled = prefs[KEY_HOME_THREE_TARIFF_ENABLED] ?: false,
            homePeakPrice = prefs[KEY_HOME_PEAK_PRICE] ?: 0.5467,
            homePeakStartHour = prefs[KEY_HOME_PEAK_START_HOUR] ?: 17,
            homePeakEndHour = prefs[KEY_HOME_PEAK_END_HOUR] ?: 23,
            homeSemiPeakPrice = prefs[KEY_HOME_SEMI_PEAK_PRICE] ?: 0.2126,
            homeSemiPeakStartHour = prefs[KEY_HOME_SEMI_PEAK_START_HOUR] ?: 6,
            homeSemiPeakEndHour = prefs[KEY_HOME_SEMI_PEAK_END_HOUR] ?: 17
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

    suspend fun updateNotifyAchievements(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFY_ACHIEVEMENTS] = enabled }
    }

    suspend fun updateSelectedCarId(carId: Long) {
        context.dataStore.edit { it[KEY_SELECTED_CAR_ID] = carId }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    suspend fun saveHomeChargingSettings(
        standardPrice: Double,
        nightTariffEnabled: Boolean,
        nightPrice: Double,
        nightStartHour: Int,
        nightEndHour: Int,
        threeTariffEnabled: Boolean,
        peakPrice: Double,
        peakStartHour: Int,
        peakEndHour: Int,
        semiPeakPrice: Double,
        semiPeakStartHour: Int,
        semiPeakEndHour: Int
    ) {
        context.dataStore.edit {
            it[KEY_HOME_CHARGE_CONFIGURED] = true
            it[KEY_HOME_STANDARD_PRICE] = standardPrice
            it[KEY_HOME_NIGHT_TARIFF_ENABLED] = nightTariffEnabled
            it[KEY_HOME_NIGHT_PRICE] = nightPrice
            it[KEY_HOME_NIGHT_START_HOUR] = nightStartHour
            it[KEY_HOME_NIGHT_END_HOUR] = nightEndHour
            it[KEY_HOME_THREE_TARIFF_ENABLED] = threeTariffEnabled
            it[KEY_HOME_PEAK_PRICE] = peakPrice
            it[KEY_HOME_PEAK_START_HOUR] = peakStartHour
            it[KEY_HOME_PEAK_END_HOUR] = peakEndHour
            it[KEY_HOME_SEMI_PEAK_PRICE] = semiPeakPrice
            it[KEY_HOME_SEMI_PEAK_START_HOUR] = semiPeakStartHour
            it[KEY_HOME_SEMI_PEAK_END_HOUR] = semiPeakEndHour
        }
    }
}
