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

data class MonthComparisonStats(
    val currentMonthCost: Double,
    val prevMonthCost: Double,
    val costDiffPercent: Double?, // positive means spent more
    val currentMonthDistance: Double,
    val prevMonthDistance: Double,
    val distanceDiffPercent: Double?,
    val currentMonthConsumption: Double?,
    val prevMonthConsumption: Double?,
    val consumptionDiffPercent: Double?
)

data class TopStationRank(
    val name: String,
    val metricValue: Double,
    val formattedValue: String,
    val subtitle: String
)

data class OperatorStatSummary(
    val name: String,
    val count: Int,
    val totalKwh: Double,
    val totalCost: Double,
    val avgPricePerKwh: Double,
    val avgPowerKw: Double?,
    val avgDurationMinutes: Long?
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
        return isHourInRange(hour, startHour, endHour)
    }

    fun isHourInRange(hour: Int, startHour: Int, endHour: Int): Boolean {
        return if (startHour > endHour) {
            // Over midnight: e.g. 23 to 6, or 17 to 23
            hour >= startHour || hour < endHour
        } else if (startHour < endHour) {
            // Same day
            hour in startHour until endHour
        } else {
            false
        }
    }

    data class HomeTariffEstimate(
        val pricePerKwh: Double,
        val tariffName: String,
        val isNight: Boolean
    )

    fun determineHomeTariff(
        settings: com.example.data.model.AppSettings,
        calendar: Calendar = Calendar.getInstance(),
        testHour: Int? = null
    ): HomeTariffEstimate {
        val hour = testHour ?: calendar.get(Calendar.HOUR_OF_DAY)
        if (settings.homeThreeTariffEnabled) {
            // 1. Peak period: 17:00 to 23:00
            if (isHourInRange(hour, settings.homePeakStartHour, settings.homePeakEndHour)) {
                return HomeTariffEstimate(
                    pricePerKwh = settings.homePeakPrice,
                    tariffName = "Пиковый (17:00-23:00)",
                    isNight = false
                )
            }
            // 2. Night / Minimal load: 23:00 to 06:00
            if (isHourInRange(hour, settings.homeNightStartHour, settings.homeNightEndHour)) {
                return HomeTariffEstimate(
                    pricePerKwh = settings.homeNightPrice,
                    tariffName = "Ночной (23:00-06:00)",
                    isNight = true
                )
            }
            // 3. Semi-peak: remaining period (06:00 to 17:00)
            return HomeTariffEstimate(
                pricePerKwh = settings.homeSemiPeakPrice,
                tariffName = "Полупиковый (06:00-17:00)",
                isNight = false
            )
        } else if (settings.homeNightTariffEnabled) {
            // Two-tariff (Day / Night)
            if (isHourInRange(hour, settings.homeNightStartHour, settings.homeNightEndHour)) {
                return HomeTariffEstimate(
                    pricePerKwh = settings.homeNightPrice,
                    tariffName = "Ночной (23:00-06:00)",
                    isNight = true
                )
            } else {
                return HomeTariffEstimate(
                    pricePerKwh = settings.homeStandardPrice,
                    tariffName = "Дневной (стандартный)",
                    isNight = false
                )
            }
        } else {
            // Single standard tariff
            return HomeTariffEstimate(
                pricePerKwh = settings.homeStandardPrice,
                tariffName = "Стандартный",
                isNight = false
            )
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
     * Takes into account actual energy discharged from battery between charges when SoC is available.
     */
    fun calculateRangeForecast(
        completedSessions: List<ChargingSession>,
        usableCapacityKwh: Double,
        remainingSoc: Double
    ): RangeForecastResult {
        val sorted = completedSessions.filter { it.status == "completed" }
            .sortedByDescending { it.endTime ?: it.startTime }

        var accumulatedKm = 0.0
        var accumulatedKwh = 0.0

        for (i in 0 until sorted.size - 1) {
            val curr = sorted[i]
            val prev = sorted[i + 1]
            val deltaKm = curr.startOdometer - prev.startOdometer
            if (deltaKm > 0) {
                accumulatedKm += deltaKm

                // Check physical battery discharge during trip
                val tripDischargePercent = if (prev.endSoc > 0 && curr.startSoc > 0 && prev.endSoc >= curr.startSoc) {
                    prev.endSoc - curr.startSoc
                } else null

                val effectiveKwh = if (tripDischargePercent != null && tripDischargePercent > 0.0 && usableCapacityKwh > 0.0) {
                    (tripDischargePercent / 100.0) * usableCapacityKwh
                } else {
                    // Fallback to delivered kWh with endSoc delta adjustment
                    val endSocDelta = if (curr.endSoc > 0 && prev.endSoc > 0) curr.endSoc - prev.endSoc else 0.0
                    val netAdj = (endSocDelta / 100.0) * usableCapacityKwh
                    val adjKwh = curr.kwhDeliveredByStation - netAdj
                    if (adjKwh > 0.0) adjKwh else curr.kwhDeliveredByStation
                }

                accumulatedKwh += effectiveKwh
                if (accumulatedKm >= 500.0) {
                    break
                }
            }
        }

        val consumptionPer100Km = if (accumulatedKm >= 20.0 && accumulatedKwh > 0.0) {
            (accumulatedKwh / accumulatedKm) * 100.0
        } else {
            // Default typical EV consumption if not enough driving history yet
            16.0
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
     * Incorporates real battery SoC discharge when available.
     */
    fun calculateMonthConsumption(
        completedSessions: List<ChargingSession>,
        year: Int,
        month: Int, // 0-based, Calendar.MONTH
        usableCapacityKwh: Double = 57.0
    ): Double? {
        val cal = Calendar.getInstance()
        val monthSessions = completedSessions.filter { session ->
            if (session.status != "completed") return@filter false
            cal.timeInMillis = session.startTime
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }.sortedBy { it.startTime }

        if (monthSessions.size < 2) {
            return null
        }

        var accumulatedKm = 0.0
        var accumulatedKwh = 0.0

        for (i in 0 until monthSessions.size - 1) {
            val prev = monthSessions[i]
            val curr = monthSessions[i + 1]
            val deltaKm = curr.startOdometer - prev.startOdometer
            if (deltaKm > 0) {
                accumulatedKm += deltaKm

                val tripDischargePercent = if (prev.endSoc > 0 && curr.startSoc > 0 && prev.endSoc >= curr.startSoc) {
                    prev.endSoc - curr.startSoc
                } else null

                val effectiveKwh = if (tripDischargePercent != null && tripDischargePercent > 0.0 && usableCapacityKwh > 0.0) {
                    (tripDischargePercent / 100.0) * usableCapacityKwh
                } else {
                    val endSocDelta = if (curr.endSoc > 0 && prev.endSoc > 0) curr.endSoc - prev.endSoc else 0.0
                    val netAdj = (endSocDelta / 100.0) * usableCapacityKwh
                    val adjKwh = curr.kwhDeliveredByStation - netAdj
                    if (adjKwh > 0.0) adjKwh else curr.kwhDeliveredByStation
                }

                accumulatedKwh += effectiveKwh
            }
        }

        return if (accumulatedKm >= 10.0 && accumulatedKwh > 0.0) {
            (accumulatedKwh / accumulatedKm) * 100.0
        } else {
            null
        }
    }

    /**
     * Interactive calculation of estimated range for a given battery SoC percentage
     */
    fun calculateRangeForSoc(
        usableCapacityKwh: Double,
        socPercent: Double,
        consumptionPer100Km: Double
    ): Double {
        if (consumptionPer100Km <= 0.0) return 0.0
        val clampedSoc = socPercent.coerceIn(0.0, 100.0)
        val kwh = usableCapacityKwh * (clampedSoc / 100.0)
        return (kwh / consumptionPer100Km) * 100.0
    }

    /**
     * Compares real consumption to official passport / WLTP consumption.
     * Returns: (deltaKwhPer100Km, deltaPercent)
     * e.g. real 18.0 vs passport 16.0 -> (+2.0 kWh, +12.5%)
     */
    fun comparePassportConsumption(
        actualConsumption: Double,
        passportConsumption: Double
    ): Pair<Double, Double> {
        if (passportConsumption <= 0.0) return Pair(0.0, 0.0)
        val deltaKwh = actualConsumption - passportConsumption
        val deltaPercent = (deltaKwh / passportConsumption) * 100.0
        return Pair(deltaKwh, deltaPercent)
    }

    /**
     * Calculates Month-to-Month comparison (current calendar month vs previous calendar month)
     * All costs are converted to the selected [mainCurrency] using CurrencyConverter.
     */
    fun calculateMonthToMonthStats(
        completedSessions: List<ChargingSession>,
        mainCurrency: String = "BYN"
    ): MonthComparisonStats {
        val cal = Calendar.getInstance()
        val curYear = cal.get(Calendar.YEAR)
        val curMonth = cal.get(Calendar.MONTH)

        cal.add(Calendar.MONTH, -1)
        val prevYear = cal.get(Calendar.YEAR)
        val prevMonth = cal.get(Calendar.MONTH)

        val tempCal = Calendar.getInstance()
        val curSessions = completedSessions.filter {
            tempCal.timeInMillis = it.startTime
            tempCal.get(Calendar.YEAR) == curYear && tempCal.get(Calendar.MONTH) == curMonth
        }

        val prevSessions = completedSessions.filter {
            tempCal.timeInMillis = it.startTime
            tempCal.get(Calendar.YEAR) == prevYear && tempCal.get(Calendar.MONTH) == prevMonth
        }

        val curCost = curSessions.sumOf { CurrencyConverter.convert(it.totalCost, it.currency, mainCurrency) }
        val prevCost = prevSessions.sumOf { CurrencyConverter.convert(it.totalCost, it.currency, mainCurrency) }
        val costDiff = if (prevCost > 0.0) ((curCost - prevCost) / prevCost) * 100.0 else null

        val curDist = if (curSessions.isNotEmpty()) {
            (curSessions.maxOf { it.startOdometer } - curSessions.minOf { it.startOdometer }).coerceAtLeast(0.0)
        } else 0.0

        val prevDist = if (prevSessions.isNotEmpty()) {
            (prevSessions.maxOf { it.startOdometer } - prevSessions.minOf { it.startOdometer }).coerceAtLeast(0.0)
        } else 0.0

        val distDiff = if (prevDist > 0.0) ((curDist - prevDist) / prevDist) * 100.0 else null

        val curCons = calculateMonthConsumption(completedSessions, curYear, curMonth)
        val prevCons = calculateMonthConsumption(completedSessions, prevYear, prevMonth)
        val consDiff = if (curCons != null && prevCons != null && prevCons > 0.0) {
            ((curCons - prevCons) / prevCons) * 100.0
        } else null

        return MonthComparisonStats(
            currentMonthCost = curCost,
            prevMonthCost = prevCost,
            costDiffPercent = costDiff,
            currentMonthDistance = curDist,
            prevMonthDistance = prevDist,
            distanceDiffPercent = distDiff,
            currentMonthConsumption = curCons,
            prevMonthConsumption = prevCons,
            consumptionDiffPercent = consDiff
        )
    }

    /**
     * Top-3 Cheapest Stations (lowest effective cost per delivered kWh in [mainCurrency])
     */
    fun getTopCheapestStations(sessions: List<ChargingSession>, mainCurrency: String): List<TopStationRank> {
        val grouped = sessions.filter { it.kwhDeliveredByStation > 0 && it.operatorName.isNotBlank() }
            .groupBy { it.operatorName }

        return grouped.map { (opName, list) ->
            val totalKwh = list.sumOf { it.kwhDeliveredByStation }
            val totalCostMain = list.sumOf { CurrencyConverter.convert(it.totalCost, it.currency, mainCurrency) }
            val avgPrice = if (totalKwh > 0) totalCostMain / totalKwh else 0.0
            TopStationRank(
                name = opName,
                metricValue = avgPrice,
                formattedValue = String.format(java.util.Locale.US, "%.2f %s/кВт·ч", avgPrice, mainCurrency),
                subtitle = "${list.size} зарядок • ${String.format(java.util.Locale.US, "%.1f", totalKwh)} кВт·ч"
            )
        }.sortedBy { it.metricValue }.take(3)
    }

    /**
     * Top-3 Fastest Stations (highest recorded or average power in kW)
     */
    fun getTopFastestStations(sessions: List<ChargingSession>): List<TopStationRank> {
        val grouped = sessions.filter { (it.avgPowerKw ?: 0.0) > 0 && it.operatorName.isNotBlank() }
            .groupBy { it.operatorName }

        return grouped.mapNotNull { (opName, list) ->
            val powers = list.mapNotNull { it.avgPowerKw }
            if (powers.isEmpty()) return@mapNotNull null
            val avgPower = powers.average()
            TopStationRank(
                name = opName,
                metricValue = avgPower,
                formattedValue = String.format(java.util.Locale.US, "%.0f кВт", avgPower),
                subtitle = "макс: ${String.format(java.util.Locale.US, "%.0f", powers.maxOrNull() ?: avgPower)} кВт • ${list.size} сессий"
            )
        }.sortedByDescending { it.metricValue }.take(3)
    }

    /**
     * Top-3 Stations with Least Losses (lowest difference between station delivered and car received)
     */
    fun getTopLeastLossesStations(sessions: List<ChargingSession>): List<TopStationRank> {
        val withLosses = sessions.filter {
            it.kwhReceivedByCar != null &&
            it.kwhReceivedByCar!! > 0 &&
            it.kwhDeliveredByStation > it.kwhReceivedByCar!! &&
            it.operatorName.isNotBlank()
        }.groupBy { it.operatorName }

        return withLosses.mapNotNull { (opName, list) ->
            val lossPercents = list.map { s ->
                val delivered = s.kwhDeliveredByStation
                val received = s.kwhReceivedByCar ?: delivered
                ((delivered - received) / delivered) * 100.0
            }
            if (lossPercents.isEmpty()) return@mapNotNull null
            val avgLoss = lossPercents.average()
            TopStationRank(
                name = opName,
                metricValue = avgLoss,
                formattedValue = String.format(java.util.Locale.US, "%.1f%% потерь", avgLoss),
                subtitle = "по ${list.size} замерам"
            )
        }.sortedBy { it.metricValue }.take(3)
    }

    /**
     * Detailed Operator Aggregated Statistics (with avg power & duration)
     */
    fun getOperatorDetailedStats(
        sessions: List<ChargingSession>,
        mainCurrency: String = "BYN"
    ): List<OperatorStatSummary> {
        val grouped = sessions.filter { it.status == "completed" && it.operatorName.isNotBlank() }
            .groupBy { it.operatorName }

        return grouped.map { (name, list) ->
            val count = list.size
            val totalKwh = list.sumOf { it.kwhDeliveredByStation }
            val totalCost = list.sumOf { CurrencyConverter.convert(it.totalCost, it.currency, mainCurrency) }
            val avgPrice = if (totalKwh > 0) totalCost / totalKwh else 0.0

            val powers = list.mapNotNull { it.avgPowerKw }.filter { it > 0 }
            val avgPower = if (powers.isNotEmpty()) powers.average() else null

            val durations = list.mapNotNull { s ->
                if (s.endTime != null && s.endTime > s.startTime) {
                    (s.endTime - s.startTime) / (1000 * 60)
                } else null
            }
            val avgDuration = if (durations.isNotEmpty()) durations.average().toLong() else null

            OperatorStatSummary(
                name = name,
                count = count,
                totalKwh = totalKwh,
                totalCost = totalCost,
                avgPricePerKwh = avgPrice,
                avgPowerKw = avgPower,
                avgDurationMinutes = avgDuration
            )
        }.sortedByDescending { it.totalCost }
    }

    /**
     * Calculates average consumption across the last three charging sessions (two distance intervals).
     * Takes into account real battery SoC discharge when available.
     */
    fun calculateLastThreeChargesConsumption(
        completedSessions: List<ChargingSession>,
        usableCapacityKwh: Double = 57.0
    ): LastThreeChargesConsumption {
        val sorted = completedSessions.filter { it.status == "completed" }
            .sortedByDescending { it.endTime ?: it.startTime }

        if (sorted.size < 2) {
            val single = sorted.firstOrNull()
            return LastThreeChargesConsumption(
                avgConsumption = null,
                distanceKm = 0.0,
                totalKwh = single?.kwhDeliveredByStation ?: 0.0,
                chargesCount = sorted.size,
                hasEnoughData = false
            )
        }

        val takeCount = if (sorted.size >= 3) 3 else 2
        val recentList = sorted.take(takeCount)

        val latest = recentList.first()
        val oldest = recentList.last()
        val distance = (latest.startOdometer - oldest.startOdometer).coerceAtLeast(0.0)

        // Sum consumed energy across the intervals in the window
        var totalConsumedKwh = 0.0
        for (i in 0 until recentList.size - 1) {
            val curr = recentList[i]
            val prev = recentList[i + 1]

            val tripDischargePercent = if (prev.endSoc > 0 && curr.startSoc > 0 && prev.endSoc >= curr.startSoc) {
                prev.endSoc - curr.startSoc
            } else null

            val intervalKwh = if (tripDischargePercent != null && tripDischargePercent > 0.0 && usableCapacityKwh > 0.0) {
                (tripDischargePercent / 100.0) * usableCapacityKwh
            } else {
                val endSocDelta = if (curr.endSoc > 0 && prev.endSoc > 0) curr.endSoc - prev.endSoc else 0.0
                val netAdj = (endSocDelta / 100.0) * usableCapacityKwh
                val adjKwh = curr.kwhDeliveredByStation - netAdj
                if (adjKwh > 0.0) adjKwh else curr.kwhDeliveredByStation
            }
            totalConsumedKwh += intervalKwh
        }

        val consumption = if (distance >= 5.0 && totalConsumedKwh > 0.0) {
            (totalConsumedKwh / distance) * 100.0
        } else null

        return LastThreeChargesConsumption(
            avgConsumption = consumption,
            distanceKm = distance,
            totalKwh = totalConsumedKwh,
            chargesCount = takeCount,
            hasEnoughData = consumption != null
        )
    }

    /**
     * Calculates real energy consumption between consecutive charging sessions.
     * Takes into account:
     * 1. Distance between sessions (startOdometer delta)
     * 2. Battery SoC change: energy discharged between end of previous charge and start of current charge
     *    OR energy delivered by station adjusted for net SoC delta (endSoc of latest - endSoc of previous).
     *
     * Example:
     * 1st session: charged to 99% at 14081 km.
     * 2nd session: arrived with 66% at 14193 km (delta = 112 km, battery discharged 99% -> 66% = 33%).
     * Charged 10 kWh to 83%.
     * Real energy consumed on the 112 km trip = 33% of usable battery (~17 kWh).
     * Consumption = (17 kWh / 112 km) * 100 = 15.2 kWh/100 km (matches vehicle dashboard perfectly).
     */
    fun calculateLastTwoChargesConsumption(
        completedSessions: List<ChargingSession>,
        usableCapacityKwh: Double = 57.0
    ): LastTwoChargesConsumption {
        val sorted = completedSessions.filter { it.status == "completed" }
            .sortedByDescending { it.endTime ?: it.startTime }
        if (sorted.size < 2) {
            val single = sorted.firstOrNull()
            return LastTwoChargesConsumption(
                avgConsumption = null,
                distanceKm = 0.0,
                totalKwh = single?.kwhDeliveredByStation ?: 0.0,
                lastChargeKwh = single?.kwhDeliveredByStation,
                hasEnoughData = false
            )
        }
        val latest = sorted[0]
        val previous = sorted[1]
        val distance = (latest.startOdometer - previous.startOdometer).coerceAtLeast(0.0)

        // Effective energy consumed during the trip between the two charges
        val tripDischargePercent = if (previous.endSoc > 0 && latest.startSoc > 0 && previous.endSoc >= latest.startSoc) {
            previous.endSoc - latest.startSoc
        } else null

        val consumedKwh: Double = if (tripDischargePercent != null && tripDischargePercent > 0.0 && usableCapacityKwh > 0.0) {
            // Physical energy taken from battery: delta SoC * usable capacity
            (tripDischargePercent / 100.0) * usableCapacityKwh
        } else {
            // Fallback: If SoC at arrival is missing, adjust delivered kWh by endSoc difference
            val endSocDelta = if (latest.endSoc > 0 && previous.endSoc > 0) {
                latest.endSoc - previous.endSoc
            } else 0.0
            val netAdjustmentKwh = (endSocDelta / 100.0) * usableCapacityKwh
            val estimated = (latest.kwhDeliveredByStation - netAdjustmentKwh).coerceAtLeast(0.0)
            if (estimated > 0.0) estimated else latest.kwhDeliveredByStation
        }

        val consumption = if (distance >= 5.0 && consumedKwh > 0.0) {
            (consumedKwh / distance) * 100.0
        } else null

        return LastTwoChargesConsumption(
            avgConsumption = consumption,
            distanceKm = distance,
            totalKwh = consumedKwh,
            lastChargeKwh = latest.kwhDeliveredByStation,
            hasEnoughData = consumption != null
        )
    }

    /**
     * Estimates prospective odometer when user types in remaining battery SoC.
     */
    fun estimateOdometerFromSoc(
        currentSoc: Double,
        carOdometer: Double,
        carSoc: Double,
        usableCapacityKwh: Double,
        avgConsumption: Double?
    ): Double {
        val socDiff = (carSoc - currentSoc).coerceAtLeast(0.0)
        if (socDiff <= 0.0) return carOdometer
        val kwhUsed = (socDiff / 100.0) * usableCapacityKwh
        val consumption = if (avgConsumption != null && avgConsumption > 5.0) avgConsumption else 16.5
        val estimatedDistance = (kwhUsed / consumption) * 100.0
        return Math.round((carOdometer + estimatedDistance) * 10.0) / 10.0
    }
}

data class LastTwoChargesConsumption(
    val avgConsumption: Double?,
    val distanceKm: Double,
    val totalKwh: Double,
    val lastChargeKwh: Double?,
    val hasEnoughData: Boolean
)

data class LastThreeChargesConsumption(
    val avgConsumption: Double?,
    val distanceKm: Double,
    val totalKwh: Double,
    val chargesCount: Int,
    val hasEnoughData: Boolean
)

