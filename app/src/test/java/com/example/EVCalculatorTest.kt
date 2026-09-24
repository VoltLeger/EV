package com.example

import com.example.data.model.AppSettings
import com.example.util.EVCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EVCalculatorTest {

    @Test
    fun testDetermineHomeTariff_standard() {
        val settings = AppSettings(
            homeChargeConfigured = true,
            homeStandardPrice = 0.36,
            homeNightTariffEnabled = false,
            homeThreeTariffEnabled = false
        )
        val tariff = EVCalculator.determineHomeTariff(settings, testHour = 14)
        assertEquals(0.36, tariff.pricePerKwh, 0.0001)
        assertFalse(tariff.isNight)
    }

    @Test
    fun testDetermineHomeTariff_twoTariff_dayAndNight() {
        val settings = AppSettings(
            homeChargeConfigured = true,
            homeStandardPrice = 0.36,
            homeNightTariffEnabled = true,
            homeNightPrice = 0.1822,
            homeNightStartHour = 23,
            homeNightEndHour = 6,
            homeThreeTariffEnabled = false
        )
        // At 02:00 (night)
        val nightTariff = EVCalculator.determineHomeTariff(settings, testHour = 2)
        assertEquals(0.1822, nightTariff.pricePerKwh, 0.0001)
        assertTrue(nightTariff.isNight)

        // At 14:00 (day)
        val dayTariff = EVCalculator.determineHomeTariff(settings, testHour = 14)
        assertEquals(0.36, dayTariff.pricePerKwh, 0.0001)
        assertFalse(dayTariff.isNight)
    }

    @Test
    fun testDetermineHomeTariff_threeTariff() {
        val settings = AppSettings(
            homeChargeConfigured = true,
            homeThreeTariffEnabled = true,
            homeNightPrice = 0.1822,
            homeNightStartHour = 23,
            homeNightEndHour = 6,
            homePeakPrice = 0.5467,
            homePeakStartHour = 17,
            homePeakEndHour = 23,
            homeSemiPeakPrice = 0.2126,
            homeSemiPeakStartHour = 6,
            homeSemiPeakEndHour = 17
        )

        // Peak (17:00 - 23:00)
        val peakTariff = EVCalculator.determineHomeTariff(settings, testHour = 19)
        assertEquals(0.5467, peakTariff.pricePerKwh, 0.0001)
        assertEquals("Пиковый (17:00-23:00)", peakTariff.tariffName)

        // Night (23:00 - 06:00)
        val nightTariff = EVCalculator.determineHomeTariff(settings, testHour = 23)
        assertEquals(0.1822, nightTariff.pricePerKwh, 0.0001)
        assertTrue(nightTariff.isNight)

        // Semi-peak (06:00 - 17:00)
        val semiPeakTariff = EVCalculator.determineHomeTariff(settings, testHour = 10)
        assertEquals(0.2126, semiPeakTariff.pricePerKwh, 0.0001)
        assertEquals("Полупиковый (06:00-17:00)", semiPeakTariff.tariffName)
    }
}
