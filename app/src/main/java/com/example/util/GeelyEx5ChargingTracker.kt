package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.ChargingSession
import com.example.widget.VoltLedgerWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

/**
 * Intelligent Automated Charging Tracker & Real Consumption Engine for Geely EX5 (Flyme Auto / ECARX).
 *
 * Tracks the full lifecycle of vehicle charging sessions automatically:
 * 1. Detects plug-in and charging start event (ECARX AdaptAPI / Settings.System / BatteryManager).
 * 2. Automatically records starting SOC%, Odometer, and computes real driving consumption (кВт·ч/100 км)
 *    from the previous charge until this charge!
 * 3. Monitors charging progress (added energy, power).
 * 4. Detects completion / gun removal, auto-records the completed session into Room database,
 *    and refreshes desktop widgets.
 */
object GeelyEx5ChargingTracker {

    private const val TAG = "GeelyEx5ChargingTracker"
    private const val PREFS_NAME = "volt_geely_ex5_charging"

    private const val KEY_IS_CHARGING = "is_charging"
    private const val KEY_START_SOC = "start_soc"
    private const val KEY_START_ODO = "start_odo"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_STATION_TYPE = "station_type"
    private const val KEY_LAST_CHARGE_END_SOC = "last_charge_end_soc"
    private const val KEY_LAST_CHARGE_END_ODO = "last_charge_end_odo"

    data class LiveChargingStatus(
        val isCharging: Boolean,
        val currentSoc: Double?,
        val currentOdometer: Double?,
        val stationType: String, // "AC" or "DC"
        val chargingPowerKw: Double?,
        val chargedEnergyKwh: Double?,
        val sessionActive: Boolean,
        val startSoc: Double?,
        val startOdometer: Double?,
        val startTime: Long?,
        val tripConsumptionBeforeCharge: Double?, // кВт·ч/100 км since last charge
        val tripDistanceKm: Double?,             // км driven since last charge
        val diagnosticSource: String
    )

    data class AutoSessionResult(
        val sessionCreated: Boolean,
        val session: ChargingSession? = null,
        val addedKwh: Double? = null,
        val startSoc: Double? = null,
        val endSoc: Double? = null,
        val tripConsumption: Double? = null,
        val message: String
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Inspects Geely EX5 vehicle charging state across all available channels:
     * 1. ECARX AdaptAPI Charging Manager
     * 2. Flyme Auto Settings.System keys
     * 3. Android Battery Broadcast (Plugged & Charging flags)
     */
    suspend fun getLiveStatus(context: Context): LiveChargingStatus = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val sessionActive = prefs.getBoolean(KEY_IS_CHARGING, false)
        val startSoc = if (sessionActive) prefs.getFloat(KEY_START_SOC, -1f).takeIf { it > 0 }?.toDouble() else null
        val startOdo = if (sessionActive) prefs.getFloat(KEY_START_ODO, -1f).takeIf { it > 0 }?.toDouble() else null
        val startTime = if (sessionActive) prefs.getLong(KEY_START_TIME, 0L).takeIf { it > 0 } else null

        // 1. Probe Telemetry for current SOC and Odometer
        val telemetry = VehicleTelemetryBridge.readVehicleTelemetry(context)
        val currentSoc = telemetry.batterySocPercent
        val currentOdo = telemetry.odometerKm

        // 2. Probe Charging State
        var detectedCharging = false
        var stationType = "AC"
        var powerKw: Double? = null
        var chargedKwh: Double? = null
        var sourceDescription = "Нет активности зарядки"

        // Channel A: ECARX AdaptAPI Charging Manager
        try {
            val ecarxCharging = probeEcarxCharging(context)
            if (ecarxCharging != null && ecarxCharging.isCharging) {
                detectedCharging = true
                stationType = ecarxCharging.stationType
                powerKw = ecarxCharging.powerKw
                chargedKwh = ecarxCharging.energyKwh
                sourceDescription = "ECARX AdaptAPI (Flyme Auto)"
            }
        } catch (_: Exception) {}

        // Channel B: Flyme Auto Settings.System
        if (!detectedCharging) {
            val settingsCharging = probeSettingsCharging(context)
            if (settingsCharging != null && settingsCharging.isCharging) {
                detectedCharging = true
                stationType = settingsCharging.stationType
                powerKw = settingsCharging.powerKw
                chargedKwh = settingsCharging.energyKwh
                sourceDescription = "Flyme Auto System Settings"
            }
        }

        // Channel C: Android Battery Intent
        if (!detectedCharging) {
            val batteryCharging = probeBatteryIntent(context)
            if (batteryCharging != null && batteryCharging.isCharging) {
                detectedCharging = true
                stationType = batteryCharging.stationType
                sourceDescription = "Системная шина питания Android"
            }
        }

        // Calculate trip consumption before charge if charging just started or session is active
        var tripConsumption: Double? = null
        var tripDistance: Double? = null
        val lastEndSoc = prefs.getFloat(KEY_LAST_CHARGE_END_SOC, -1f).takeIf { it > 0 }?.toDouble()
        val lastEndOdo = prefs.getFloat(KEY_LAST_CHARGE_END_ODO, -1f).takeIf { it > 0 }?.toDouble()

        val refSoc = startSoc ?: currentSoc
        val refOdo = startOdo ?: currentOdo

        if (lastEndSoc != null && lastEndOdo != null && refSoc != null && refOdo != null) {
            val distance = refOdo - lastEndOdo
            val deltaSoc = lastEndSoc - refSoc
            if (distance > 2.0 && deltaSoc > 0.5) {
                val db = AppDatabase.getDatabase(context)
                val settingsManager = SettingsManager(context)
                val activeCarId = settingsManager.settingsFlow.firstOrNull()?.selectedCarId
                val car = db.carDao().getAllCarsList().find { it.id == activeCarId }
                    ?: db.carDao().getAllCarsList().firstOrNull()
                val capacity = car?.usableCapacityKwh ?: 60.22 // Default Geely EX5 usable capacity ~60.22 kWh
                val usedKwh = (deltaSoc / 100.0) * capacity
                tripConsumption = (usedKwh / distance) * 100.0
                tripDistance = distance
            }
        }

        LiveChargingStatus(
            isCharging = detectedCharging,
            currentSoc = currentSoc,
            currentOdometer = currentOdo,
            stationType = stationType,
            chargingPowerKw = powerKw,
            chargedEnergyKwh = chargedKwh,
            sessionActive = sessionActive,
            startSoc = startSoc,
            startOdometer = startOdo,
            startTime = startTime,
            tripConsumptionBeforeCharge = tripConsumption,
            tripDistanceKm = tripDistance,
            diagnosticSource = sourceDescription
        )
    }

    /**
     * Call this when vehicle charging begins (e.g. gun inserted and charging acknowledged)
     */
    suspend fun startChargingSession(
        context: Context,
        stationType: String = "AC",
        forcedStartSoc: Double? = null,
        forcedStartOdo: Double? = null
    ): LiveChargingStatus = withContext(Dispatchers.IO) {
        val telemetry = VehicleTelemetryBridge.readVehicleTelemetry(context)
        val soc = forcedStartSoc ?: telemetry.batterySocPercent ?: 50.0
        val odo = forcedStartOdo ?: telemetry.odometerKm ?: 0.0
        val now = System.currentTimeMillis()

        getPrefs(context).edit()
            .putBoolean(KEY_IS_CHARGING, true)
            .putFloat(KEY_START_SOC, soc.toFloat())
            .putFloat(KEY_START_ODO, odo.toFloat())
            .putLong(KEY_START_TIME, now)
            .putString(KEY_STATION_TYPE, stationType)
            .apply()

        Log.i(TAG, "Started automated charging session: $soc% at $odo km ($stationType)")
        getLiveStatus(context)
    }

    /**
     * Call this when vehicle charging finishes (or user taps "Завершить и сохранить в расходы").
     * Computes added energy, saves ChargingSession into Room DB, calculates real trip consumption,
     * updates active car and desktop widgets.
     */
    suspend fun finishChargingSession(
        context: Context,
        forcedEndSoc: Double? = null,
        forcedEndOdo: Double? = null,
        operatorName: String = "Geely EX5 Auto",
        pricePerKwh: Double? = null
    ): AutoSessionResult = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)
        val sessionActive = prefs.getBoolean(KEY_IS_CHARGING, false)
        val startSoc = prefs.getFloat(KEY_START_SOC, 0f).toDouble()
        val startOdo = prefs.getFloat(KEY_START_ODO, 0f).toDouble()
        val startTime = prefs.getLong(KEY_START_TIME, System.currentTimeMillis())
        val stationType = prefs.getString(KEY_STATION_TYPE, "AC") ?: "AC"

        val telemetry = VehicleTelemetryBridge.readVehicleTelemetry(context)
        val endSoc = forcedEndSoc ?: telemetry.batterySocPercent ?: (startSoc + 30.0).coerceAtMost(100.0)
        val endOdo = forcedEndOdo ?: telemetry.odometerKm ?: startOdo

        val db = AppDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        val settings = settingsManager.settingsFlow.firstOrNull()
        val allCars = db.carDao().getAllCarsList()
        val activeCar = allCars.find { it.id == settings?.selectedCarId }
            ?: allCars.find { it.isActive }
            ?: allCars.firstOrNull()

        val capacity = activeCar?.usableCapacityKwh ?: 60.22
        val deltaSoc = (endSoc - startSoc).coerceAtLeast(0.0)
        val addedKwh = (deltaSoc / 100.0) * capacity
        val deliveredKwh = addedKwh * (if (stationType == "DC") 1.05 else 1.10) // Conversion efficiency

        val resolvedPrice = pricePerKwh ?: 0.40 // default tariff
        val totalCost = deliveredKwh * resolvedPrice

        // Calculate trip consumption since previous charge
        val lastEndSoc = prefs.getFloat(KEY_LAST_CHARGE_END_SOC, -1f).takeIf { it > 0 }?.toDouble()
        val lastEndOdo = prefs.getFloat(KEY_LAST_CHARGE_END_ODO, -1f).takeIf { it > 0 }?.toDouble()
        var tripConsumption: Double? = null
        if (lastEndSoc != null && lastEndOdo != null && startOdo > lastEndOdo) {
            val dist = startOdo - lastEndOdo
            val usedSoc = (lastEndSoc - startSoc).coerceAtLeast(0.0)
            val usedEnergy = (usedSoc / 100.0) * capacity
            if (dist > 2.0 && usedEnergy > 0.1) {
                tripConsumption = (usedEnergy / dist) * 100.0
            }
        }

        if (activeCar != null && addedKwh > 0.5) {
            val newSession = ChargingSession(
                carId = activeCar.id,
                startOdometer = startOdo,
                startSoc = startSoc,
                endSoc = endSoc,
                kwhDeliveredByStation = Math.round(deliveredKwh * 100.0) / 100.0,
                kwhReceivedByCar = Math.round(addedKwh * 100.0) / 100.0,
                pricePerKwh = resolvedPrice,
                energyCost = Math.round(totalCost * 100.0) / 100.0,
                totalCost = Math.round(totalCost * 100.0) / 100.0,
                currency = settings?.currency ?: "BYN",
                stationType = stationType,
                operatorName = operatorName,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                status = "completed"
            )

            db.chargingSessionDao().insertSession(newSession)

            // Update active car
            val updatedCar = activeCar.copy(
                currentSoc = endSoc,
                initialOdometer = endOdo.coerceAtLeast(activeCar.initialOdometer)
            )
            db.carDao().updateCar(updatedCar)
            VoltLedgerWidgetProvider.updateAllWidgets(context)

            // Store this charge's end state for next trip consumption calculation
            prefs.edit()
                .putBoolean(KEY_IS_CHARGING, false)
                .putFloat(KEY_LAST_CHARGE_END_SOC, endSoc.toFloat())
                .putFloat(KEY_LAST_CHARGE_END_ODO, endOdo.toFloat())
                .remove(KEY_START_SOC)
                .remove(KEY_START_ODO)
                .remove(KEY_START_TIME)
                .apply()

            val tripStr = tripConsumption?.let { String.format(" • Расход до зарядки: %.1f кВт·ч/100км", it) } ?: ""
            return@withContext AutoSessionResult(
                sessionCreated = true,
                session = newSession,
                addedKwh = addedKwh,
                startSoc = startSoc,
                endSoc = endSoc,
                tripConsumption = tripConsumption,
                message = "Зарядка Geely EX5 зафиксирована: ${startSoc.toInt()}% -> ${endSoc.toInt()}% (+${String.format("%.1f", addedKwh)} кВт·ч)$tripStr"
            )
        }

        // Clear active session if not saved
        prefs.edit().putBoolean(KEY_IS_CHARGING, false).apply()
        AutoSessionResult(
            sessionCreated = false,
            message = "Зарядка не сохранена (слишком мало энергии или нет активного авто)"
        )
    }

    private data class InternalChargingProbe(
        val isCharging: Boolean,
        val stationType: String = "AC",
        val powerKw: Double? = null,
        val energyKwh: Double? = null
    )

    private fun probeEcarxCharging(context: Context): InternalChargingProbe? {
        val carClassNames = listOf("ecarx.xui.adaptapi.car.Car", "com.ecarx.xui.adaptapi.car.Car")
        for (name in carClassNames) {
            try {
                val clazz = Class.forName(name)
                val createMethod = clazz.getMethod("create", Context::class.java)
                val carInstance = createMethod.invoke(null, context) ?: continue

                for (mgrName in listOf("getCharging", "getChargingManager", "getEnergy", "getHevEv")) {
                    try {
                        val mgr = carInstance.javaClass.getMethod(mgrName).invoke(carInstance) ?: continue
                        for (m in mgr.javaClass.methods) {
                            val mn = m.name.lowercase()
                            if (m.parameterCount == 0 && (mn.contains("charge") || mn.contains("charging"))) {
                                val res = m.invoke(mgr)
                                if (res is Number) {
                                    val code = res.toInt()
                                    // Usually: 1 = connecting, 2 = charging, 3 = complete
                                    if (code in 1..2) {
                                        return InternalChargingProbe(isCharging = true, stationType = "AC")
                                    }
                                } else if (res is Boolean && res) {
                                    return InternalChargingProbe(isCharging = true, stationType = "AC")
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun probeSettingsCharging(context: Context): InternalChargingProbe? {
        val resolver = context.contentResolver
        val chargeStatusKeys = listOf(
            "car_charging_status",
            "charge_state",
            "ecarx_charging_state",
            "charging_status",
            "car_charge_state",
            "charging_gun_status"
        )
        for (key in chargeStatusKeys) {
            try {
                val state = Settings.System.getInt(resolver, key, -1)
                if (state in 1..3) {
                    return InternalChargingProbe(isCharging = true, stationType = "AC")
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun probeBatteryIntent(context: Context): InternalChargingProbe? {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged != 0
        val isDc = plugged == BatteryManager.BATTERY_PLUGGED_AC && batteryIntent.getIntExtra("charge_rate_kw", 0) > 22
        return if (isCharging) {
            InternalChargingProbe(isCharging = true, stationType = if (isDc) "DC" else "AC")
        } else null
    }
}
