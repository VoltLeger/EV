package com.example.util

import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import java.util.Calendar

data class RangeForecastResult(
    val estimatedRangeKm: Double,
    val realConsumptionPer100Km: Double,
    val basisDistanceKm: Double,
    val remainingKwh: Double
)

object EVCalculator {

    /**
     * buffer_kwh = 1.5 + 0.025 * E
     * usable_kwh = E - buffer_kwh
     * where E in 20..240 kWh
     */
    fun calculateUsableCapacity(declaredKwh: Double): Double {
        val clamped = declaredKwh.coerceIn(20.0, 240.0)
        val buffer = 1.5 + (0.025 * clamped)
        return (clamped - buffer).coerceAtLeast(1.0)
    }

    fun calculateBuffer(declaredKwh: Double): Double {
        val clamped = declaredKwh.coerceIn(20.0, 240.0)
        return 1.5 + (0.025 * clamped)
    }

    /**
     * Checks if current time is within operator's night tariff window.
     * E.g. 23:00 to 06:00, or 21:00 to 08:30
     */
    fun isNightTariffTime(
        startHour: Int,
        endHour: Int,
        calendar: Calendar = Calendar.getInstance()
    ): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return if (startHour > endHour) {
            // Over midnight: e.g. 23 to 6, or 21 to 8
            hour >= startHour || hour < endHour
        } else if (startHour < endHour) {
            // Same day
            hour in startHour until endHour
        } else {
            false
        }
    }

    /**
     * Calculate suggested idle penalty based on operator rule and session duration
     */
    fun calculateSuggestedPenalty(
        operator: Operator?,
        startTime: Long,
        endTime: Long
    ): Double {
        if (operator == null || operator.penaltyIdlePerMin <= 0.0) return 0.0
        val durationMillis = (endTime - startTime).coerceAtLeast(0L)
        val durationMinutes = durationMillis / (1000 * 60)
        val chargeableMinutes = (durationMinutes - operator.penaltyFreeMinutes).coerceAtLeast(0L)
        return chargeableMinutes * operator.penaltyIdlePerMin
    }

    /**
     * Calculates real consumption over the last ~500 km of driving and forecasts remaining range.
     */
    fun calculateRangeForecast(
        completedSessions: List<ChargingSession>,
        usableCapacityKwh: Double,
        remainingSoc: Double
    ): RangeForecastResult {
        val sorted = completedSessions.filter { it.status == "completed" && it.kwhDeliveredByStation > 0 }
            .sortedByDescending { it.startTime }

        var accumulatedKm = 0.0
        var accumulatedKwh = 0.0

        for (i in 0 until sorted.size - 1) {
            val curr = sorted[i]
            val prev = sorted[i + 1]
            val deltaKm = curr.startOdometer - prev.startOdometer
            if (deltaKm > 0) {
                accumulatedKm += deltaKm
                accumulatedKwh += curr.kwhDeliveredByStation
                if (accumulatedKm >= 500.0) {
                    break
                }
            }
        }

        val consumptionPer100Km = if (accumulatedKm >= 30.0 && accumulatedKwh > 0.0) {
            (accumulatedKwh / accumulatedKm) * 100.0
        } else {
            // Default typical EV consumption if not enough driving history yet
            18.5
        }

        val clampedSoc = remainingSoc.coerceIn(0.0, 100.0)
        val remainingKwh = usableCapacityKwh * (clampedSoc / 100.0)
        val estimatedRange = if (consumptionPer100Km > 0.0) {
            (remainingKwh / consumptionPer100Km) * 100.0
        } else {
            0.0
        }

        return RangeForecastResult(
            estimatedRangeKm = estimatedRange,
            realConsumptionPer100Km = consumptionPer100Km,
            basisDistanceKm = accumulatedKm,
            remainingKwh = remainingKwh
        )
    }

    /**
     * Calculates average consumption for a specific month (kWh / 100 km)
     */
    fun calculateMonthConsumption(
        completedSessions: List<ChargingSession>,
        year: Int,
        month: Int // 0-based, Calendar.MONTH
    ): Double? {
        val cal = Calendar.getInstance()
        val monthSessions = completedSessions.filter { session ->
            if (session.status != "completed") return@filter false
            cal.timeInMillis = session.startTime
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }.sortedBy { it.startTime }

        if (monthSessions.size < 2) {
            // If we have single session, see if we can calculate from SOC delta and usable capacity
            return null
        }

        val minOdo = monthSessions.minOf { it.startOdometer }
        val maxOdo = monthSessions.maxOf { it.startOdometer }
        val distance = maxOdo - minOdo
        val totalKwh = monthSessions.sumOf { it.kwhDeliveredByStation }

        return if (distance > 10.0 && totalKwh > 0.0) {
            (totalKwh / distance) * 100.0
        } else {
            null
        }
    }
}
