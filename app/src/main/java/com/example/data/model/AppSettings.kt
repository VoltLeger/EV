package com.example.data.model

data class AppSettings(
    val language: String = "ru", // "ru", "en"
    val theme: String = "dark", // "dark", "light", "system", "amoled"
    val currency: String = "BYN", // "BYN", "RUB", "PLN", "USD", "EUR"
    val autoNightTariff: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val notifyUnfinished: Boolean = true,
    val unfinishedHoursThreshold: Int = 4,
    val notifyWeekly: Boolean = true,
    val notifyMonthly: Boolean = true,
    val notifyAchievements: Boolean = true,
    val selectedCarId: Long = 0L,
    val onboardingCompleted: Boolean = false,
    // Home Charging Settings
    val homeChargeConfigured: Boolean = false,
    val homeStandardPrice: Double = 0.36,
    val homeNightTariffEnabled: Boolean = false,
    val homeNightPrice: Double = 0.1822,
    val homeNightStartHour: Int = 23,
    val homeNightEndHour: Int = 6,
    val homeThreeTariffEnabled: Boolean = false,
    val homePeakPrice: Double = 0.5467,
    val homePeakStartHour: Int = 17,
    val homePeakEndHour: Int = 23,
    val homeSemiPeakPrice: Double = 0.2126,
    val homeSemiPeakStartHour: Int = 6,
    val homeSemiPeakEndHour: Int = 17,
    val homeLastMeterKwh: Double? = null
)
