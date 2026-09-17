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
    val selectedCarId: Long = 0L,
    val onboardingCompleted: Boolean = false,
    val pinEnabled: Boolean = false,
    val pinCode: String = "",
    val biometricEnabled: Boolean = false
)
