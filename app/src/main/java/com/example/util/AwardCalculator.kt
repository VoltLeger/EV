package com.example.util

import com.example.data.model.Award
import com.example.data.model.AwardCategory
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import kotlin.math.roundToInt

object AwardCalculator {

    fun calculateAwards(
        sessions: List<ChargingSession>,
        cars: List<Car>,
        activeCar: Car?
    ): List<Award> {
        val completed = sessions.filter { it.status == "completed" }

        // 1. Total energy delivered (kWh)
        val totalKwh = completed.sumOf { it.kwhDeliveredByStation }

        // 2. Total distance driven
        val totalDistance = if (completed.size >= 2) {
            val sorted = completed.sortedBy { it.startTime }
            val first = sorted.first().startOdometer
            val last = sorted.last().startOdometer
            (last - first).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // 3. Night charges count
        val nightChargesCount = completed.count { it.nightTariffApplied }.toDouble()

        // 4. DC fast sessions with power >= 30 kW or fast DC
        val dcFastCount = completed.count {
            it.stationType.equals("DC", ignoreCase = true) && (it.avgPowerKw ?: 40.0) >= 30.0
        }.toDouble()

        // 5. Distinct operators
        val distinctOperatorsCount = completed.mapNotNull {
            it.operatorName.ifBlank { null }
        }.distinct().size.toDouble()

        // 6. Real consumption
        val avgConsumption = if (totalDistance > 100 && totalKwh > 10) {
            (totalKwh / totalDistance) * 100.0
        } else {
            activeCar?.passportConsumption ?: 16.0
        }

        // 7. Completed sessions count
        val sessionsCount = completed.size.toDouble()

        // 8. BatteryFly sessions count ("Бабочка")
        val batteryFlyCount = completed.count {
            it.operatorName.contains("BatteryFly", ignoreCase = true) ||
            it.operatorName.contains("Бабочк", ignoreCase = true) ||
            it.operatorComment?.contains("BatteryFly", ignoreCase = true) == true
        }.toDouble()

        // 9. Malanka sessions count
        val malankaCount = completed.count {
            it.operatorName.contains("Malanka", ignoreCase = true) ||
            it.operatorName.contains("Маланк", ignoreCase = true) ||
            it.operatorComment?.contains("Malanka", ignoreCase = true) == true
        }.toDouble()

        return listOf(
            Award(
                id = "ENERGY_500",
                title = "Энерго-Спринтер",
                description = "Зарядить суммарно 500 кВт·ч энергии",
                category = AwardCategory.ENERGY,
                currentProgress = totalKwh,
                maxProgress = 500.0,
                unit = "кВт·ч",
                isUnlocked = totalKwh >= 500.0,
                avatarEffectReward = "neon_cyan",
                avatarEffectName = "Неоновый Циан",
                xpReward = 250,
                iconKey = "bolt"
            ),
            Award(
                id = "ENERGY_2000",
                title = "Гигаватт-Мастер",
                description = "Зарядить суммарно 2 000 кВт·ч чистой энергии",
                category = AwardCategory.ENERGY,
                currentProgress = totalKwh,
                maxProgress = 2000.0,
                unit = "кВт·ч",
                isUnlocked = totalKwh >= 2000.0,
                avatarEffectReward = "plasma_purple",
                avatarEffectName = "Плазменная Аура",
                xpReward = 600,
                iconKey = "bolt"
            ),
            Award(
                id = "DISTANCE_1000",
                title = "Первая тысяча",
                description = "Преодолеть 1 000 км пути на электротяге",
                category = AwardCategory.DISTANCE,
                currentProgress = totalDistance,
                maxProgress = 1000.0,
                unit = "км",
                isUnlocked = totalDistance >= 1000.0,
                avatarEffectReward = "emerald_glow",
                avatarEffectName = "Изумрудный Ореол",
                xpReward = 200,
                iconKey = "speed"
            ),
            Award(
                id = "DISTANCE_10000",
                title = "Дальнобойщик EV",
                description = "Преодолеть 10 000 км на электромобиле",
                category = AwardCategory.DISTANCE,
                currentProgress = totalDistance,
                maxProgress = 10000.0,
                unit = "км",
                isUnlocked = totalDistance >= 10000.0,
                avatarEffectReward = "golden_ribbon",
                avatarEffectName = "Золотой Ранг",
                xpReward = 1000,
                iconKey = "speed"
            ),
            Award(
                id = "NIGHT_CHARGER",
                title = "Ночной Страж",
                description = "Совершить 5 зарядок по выгодному ночному тарифу",
                category = AwardCategory.NIGHT,
                currentProgress = nightChargesCount,
                maxProgress = 5.0,
                unit = "зарядок",
                isUnlocked = nightChargesCount >= 5.0,
                avatarEffectReward = "twilight_ring",
                avatarEffectName = "Сумеречное Кольцо",
                xpReward = 300,
                iconKey = "moon"
            ),
            Award(
                id = "DC_LIGHTNING",
                title = "Молния DC",
                description = "Провести 3 скоростные зарядки постоянным током",
                category = AwardCategory.DC_POWER,
                currentProgress = dcFastCount,
                maxProgress = 3.0,
                unit = "сессий",
                isUnlocked = dcFastCount >= 3.0,
                avatarEffectReward = "lightning_spark",
                avatarEffectName = "Кибер-Молния",
                xpReward = 350,
                iconKey = "flash"
            ),
            Award(
                id = "NETWORK_PIONEER",
                title = "Сетевой Первопроходец",
                description = "Опробовать зарядки минимум 3 разных операторов",
                category = AwardCategory.NETWORK,
                currentProgress = distinctOperatorsCount,
                maxProgress = 3.0,
                unit = "сетей",
                isUnlocked = distinctOperatorsCount >= 3.0,
                avatarEffectReward = "cyber_matrix",
                avatarEffectName = "Кибер-Матрица",
                xpReward = 250,
                iconKey = "station"
            ),
            Award(
                id = "CENTURION",
                title = "Сотня Вольт",
                description = "Завершить 15 успешных сессий зарядки",
                category = AwardCategory.EFFICIENCY,
                currentProgress = sessionsCount,
                maxProgress = 15.0,
                unit = "сессий",
                isUnlocked = sessionsCount >= 15.0,
                avatarEffectReward = "pulsar_diamond",
                avatarEffectName = "Алмазный Пульсар",
                xpReward = 400,
                iconKey = "trophy"
            ),
            Award(
                id = "BATTERY_FLY_3",
                title = "Повелитель Бабочки",
                description = "Совершить 3 зарядки в сети BatteryFly",
                category = AwardCategory.NETWORK,
                currentProgress = batteryFlyCount,
                maxProgress = 3.0,
                unit = "зарядок",
                isUnlocked = batteryFlyCount >= 3.0,
                avatarEffectReward = "cyan_pulse",
                avatarEffectName = "Циановый Пульс",
                xpReward = 300,
                iconKey = "station"
            ),
            Award(
                id = "MALANKA_3",
                title = "Маланка Драйв",
                description = "Совершить 3 зарядки в сети Malanka",
                category = AwardCategory.NETWORK,
                currentProgress = malankaCount,
                maxProgress = 3.0,
                unit = "зарядок",
                isUnlocked = malankaCount >= 3.0,
                avatarEffectReward = "green_spark",
                avatarEffectName = "Зеленая Искра",
                xpReward = 300,
                iconKey = "bolt"
            )
        )
    }

    fun calculateTotalXp(
        sessions: List<ChargingSession>,
        awards: List<Award>
    ): Long {
        val completed = sessions.filter { it.status == "completed" }
        val energyXp = completed.sumOf { it.kwhDeliveredByStation }.toLong()
        val sessionsXp = completed.size * 10L
        val awardsXp = awards.filter { it.isUnlocked }.sumOf { it.xpReward.toLong() }
        return (energyXp + sessionsXp + awardsXp).coerceAtLeast(0L)
    }

    fun getRankTier(xp: Long): Pair<String, Int> {
        return when {
            xp >= 5000 -> "Гроссмейстер Вольт" to 4
            xp >= 1500 -> "Мастер Киловатт" to 3
            xp >= 500 -> "Энерджи-Драйвер" to 2
            else -> "Новичок" to 1
        }
    }
}
