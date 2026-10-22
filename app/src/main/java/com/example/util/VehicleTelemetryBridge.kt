package com.example.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.Settings
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.widget.VoltLedgerWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.lang.reflect.Method

/**
 * Universal Vehicle Telemetry Bridge for Geely / Flyme Auto / ECARX / Android Automotive.
 * Probes multiple channels used by third-party Geely apps (Geely Tune, Geely Custom Profiles)
 * without requiring Geely platform signatures:
 *
 * 1. ECARX AdaptAPI (ecarx.xui.adaptapi.car.Car / com.ecarx.xui.adaptapi.car.Car)
 * 2. Geely OS GlyCar (com.geely.os.car.GlyCar)
 * 3. System / Global Settings (Settings.System / Settings.Global SOC keys)
 * 4. Android System Battery Broadcast (Intent.ACTION_BATTERY_CHANGED mirrored by EV firmware)
 * 5. Android Automotive CarPropertyManager (EV_BATTERY_LEVEL)
 */
object VehicleTelemetryBridge {

    private const val TAG = "VehicleTelemetryBridge"

    enum class SourceType {
        ECARX_ADAPT_API,
        GEELY_GLY_CAR,
        ZEEKR_API,
        BYD_DILINK_API,
        DEEPAL_CHANGAN_API,
        LI_AUTO_API,
        VOYAH_API,
        XIAOMI_CAR_API,
        SYSTEM_SETTINGS,
        BATTERY_BROADCAST,
        ANDROID_CAR_API,
        NONE
    }

    data class TelemetryResult(
        val success: Boolean,
        val source: SourceType,
        val batterySocPercent: Double? = null,
        val remainingKwh: Double? = null,
        val odometerKm: Double? = null,
        val message: String,
        val details: List<String> = emptyList()
    )

    /**
     * Probes all available vehicle telemetry channels in priority order across
     * popular EV cockpits (Geely, Zeekr, BYD DiLink, Deepal, Li Auto, Voyah, Xiaomi, AAOS).
     * If a valid battery SOC or energy reading is found, automatically updates the active car in Room DB
     * and refreshes desktop widgets.
     */
    suspend fun readVehicleTelemetry(context: Context): TelemetryResult = withContext(Dispatchers.IO) {
        val diagnosticLog = mutableListOf<String>()

        // -----------------------------------------------------------------------------------
        // Channel 1: ECARX AdaptAPI (Used by Geely EX5/EX2, Monjaro, Galaxy, Smart, Zeekr)
        // -----------------------------------------------------------------------------------
        try {
            val ecarxResult = probeEcarxAdaptApi(context, diagnosticLog)
            if (ecarxResult != null && (ecarxResult.batterySocPercent != null || ecarxResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, ecarxResult)
                return@withContext ecarxResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("ECARX AdaptAPI: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2: Geely OS GlyCar Bridge
        // -----------------------------------------------------------------------------------
        try {
            val glyResult = probeGeelyGlyCar(context, diagnosticLog)
            if (glyResult != null && (glyResult.batterySocPercent != null || glyResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, glyResult)
                return@withContext glyResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Geely GlyCar: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.1: Zeekr OS Dedicated API (Zeekr 001, 007, 7X, X, 009)
        // -----------------------------------------------------------------------------------
        try {
            val zeekrResult = probeZeekr(context, diagnosticLog)
            if (zeekrResult != null && (zeekrResult.batterySocPercent != null || zeekrResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, zeekrResult)
                return@withContext zeekrResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Zeekr API: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.2: BYD DiLink API (Song Plus, Yuan/Atto 3, Han, Tang, Seal, Dolphin, Seagull, Denza)
        // -----------------------------------------------------------------------------------
        try {
            val bydResult = probeBydDiLink(context, diagnosticLog)
            if (bydResult != null && (bydResult.batterySocPercent != null || bydResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, bydResult)
                return@withContext bydResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("BYD DiLink API: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.5: Deepal S05 / Changan Auto API
        // -----------------------------------------------------------------------------------
        try {
            val deepalResult = probeDeepalChangan(context, diagnosticLog)
            if (deepalResult != null && (deepalResult.batterySocPercent != null || deepalResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, deepalResult)
                return@withContext deepalResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Deepal/Changan: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.6: Li Auto / Lixiang OS API (L6, L7, L8, L9, Mega)
        // -----------------------------------------------------------------------------------
        try {
            val liResult = probeLiAuto(context, diagnosticLog)
            if (liResult != null && (liResult.batterySocPercent != null || liResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, liResult)
                return@withContext liResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Li Auto: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.7: Voyah OS API (Free, Dream, Passion)
        // -----------------------------------------------------------------------------------
        try {
            val voyahResult = probeVoyah(context, diagnosticLog)
            if (voyahResult != null && (voyahResult.batterySocPercent != null || voyahResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, voyahResult)
                return@withContext voyahResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Voyah: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 2.8: Xiaomi SU7 / HyperOS Car API
        // -----------------------------------------------------------------------------------
        try {
            val xiaomiResult = probeXiaomiCar(context, diagnosticLog)
            if (xiaomiResult != null && (xiaomiResult.batterySocPercent != null || xiaomiResult.remainingKwh != null)) {
                applyTelemetryToActiveCar(context, xiaomiResult)
                return@withContext xiaomiResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Xiaomi Car: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 3: System & Global Settings (Settings.System / Settings.Global)
        // Many head units mirror real-time CAN bus data to settings keys for status bar widgets
        // -----------------------------------------------------------------------------------
        try {
            val settingsResult = probeSystemSettings(context, diagnosticLog)
            if (settingsResult != null && settingsResult.batterySocPercent != null) {
                applyTelemetryToActiveCar(context, settingsResult)
                return@withContext settingsResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("System Settings: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 4: Android System Battery Broadcast (ACTION_BATTERY_CHANGED)
        // BYD DiLink, ECARX and Flyme Auto frequently mirror traction battery SOC to Android battery
        // -----------------------------------------------------------------------------------
        try {
            val batteryBroadcastResult = probeBatteryBroadcast(context, diagnosticLog)
            if (batteryBroadcastResult != null && batteryBroadcastResult.batterySocPercent != null) {
                applyTelemetryToActiveCar(context, batteryBroadcastResult)
                return@withContext batteryBroadcastResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Battery Broadcast: исключение (${e.message})")
        }

        // -----------------------------------------------------------------------------------
        // Channel 5: Android Automotive CarPropertyManager (Car.createCar)
        // -----------------------------------------------------------------------------------
        try {
            val androidCarResult = probeAndroidCarApi(context, diagnosticLog)
            if (androidCarResult != null && androidCarResult.batterySocPercent != null) {
                applyTelemetryToActiveCar(context, androidCarResult)
                return@withContext androidCarResult
            }
        } catch (e: Throwable) {
            diagnosticLog.add("Android Car API: исключение (${e.message})")
        }

        TelemetryResult(
            success = false,
            source = SourceType.NONE,
            message = "Автомобильные службы не ответили (устройство не в сети CAN авто или значения не экспортируются)",
            details = diagnosticLog
        )
    }

    /**
     * Channel 1: ECARX AdaptAPI
     * Queries ecarx.xui.adaptapi.car.Car or com.ecarx.xui.adaptapi.car.Car
     */
    private fun probeEcarxAdaptApi(context: Context, log: MutableList<String>): TelemetryResult? {
        val carClassNames = listOf(
            "ecarx.xui.adaptapi.car.Car",
            "com.ecarx.xui.adaptapi.car.Car"
        )

        var carClass: Class<*>? = null
        for (name in carClassNames) {
            try {
                carClass = Class.forName(name)
                log.add("ECARX AdaptAPI: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }

        if (carClass == null) {
            log.add("ECARX AdaptAPI: классы не найдены в системе")
            return null
        }

        // Car.create(context)
        val createMethod = try {
            carClass.getMethod("create", Context::class.java)
        } catch (_: NoSuchMethodException) {
            log.add("ECARX AdaptAPI: метод Car.create(context) не найден")
            return null
        }

        val carInstance = createMethod.invoke(null, context) ?: run {
            log.add("ECARX AdaptAPI: Car.create вернул null")
            return null
        }

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        // Try HevEv / Energy / Charging manager
        val managerGetters = listOf("getHevEv", "getEnergy", "getEnergyManager", "getCharging", "getSensorManager", "getSensor")
        for (getter in managerGetters) {
            try {
                val method = carInstance.javaClass.getMethod(getter)
                val manager = method.invoke(carInstance)
                if (manager != null) {
                    log.add("ECARX AdaptAPI: получен менеджер $getter()")

                    // Scan methods on this manager
                    for (m in manager.javaClass.methods) {
                        val mName = m.name.lowercase()
                        if (m.parameterCount == 0) {
                            if (mName.contains("soc") || mName.contains("batterylevel") || mName.contains("batterypower")) {
                                val value = invokeDoubleGetter(manager, m)
                                if (value != null && value in 1.0..100.0) {
                                    detectedSoc = value
                                    log.add("ECARX AdaptAPI: $getter.${m.name}() = $value%")
                                }
                            }
                            if (mName.contains("energy") || mName.contains("kwh") || mName.contains("remainingpower") || mName.contains("remainingcapacity")) {
                                val value = invokeDoubleGetter(manager, m)
                                if (value != null && value > 0.0) {
                                    detectedKwh = value
                                    log.add("ECARX AdaptAPI: $getter.${m.name}() = $value кВт·ч")
                                }
                            }
                            if (mName.contains("odometer") || mName.contains("mileage")) {
                                val value = invokeDoubleGetter(manager, m)
                                if (value != null && value > 0.0) {
                                    detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                                    log.add("ECARX AdaptAPI: $getter.${m.name}() = $detectedOdo км")
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (detectedSoc != null || detectedKwh != null) {
            val socText = detectedSoc?.let { String.format("%.0f%%", it) } ?: "--%"
            val kwhText = detectedKwh?.let { String.format("%.1f кВт·ч", it) } ?: ""
            return TelemetryResult(
                success = true,
                source = SourceType.ECARX_ADAPT_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через ECARX AdaptAPI ($socText $kwhText)",
                details = log
            )
        }

        log.add("ECARX AdaptAPI: экземпляр создан, но данные батареи не получены")
        return null
    }

    /**
     * Channel 2: Geely GlyCar Bridge (com.geely.os.car.GlyCar)
     */
    private fun probeGeelyGlyCar(context: Context, log: MutableList<String>): TelemetryResult? {
        val glyCarClass = try {
            Class.forName("com.geely.os.car.GlyCar")
        } catch (_: ClassNotFoundException) {
            log.add("Geely GlyCar: класс com.geely.os.car.GlyCar не найден")
            return null
        }

        log.add("Geely GlyCar: найден класс GlyCar")
        val instance = try {
            val getInstance = glyCarClass.getMethod("getInstance", Context::class.java)
            getInstance.invoke(null, context)
        } catch (_: Exception) {
            try {
                glyCarClass.getMethod("getInstance").invoke(null)
            } catch (_: Exception) {
                null
            }
        } ?: run {
            log.add("Geely GlyCar: не удалось получить экземпляр")
            return null
        }

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        for (m in instance.javaClass.methods) {
            val mName = m.name.lowercase()
            if (m.parameterCount == 0) {
                if (mName.contains("soc") || mName.contains("battery") || mName.contains("level")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value in 1.0..100.0) {
                        detectedSoc = value
                        log.add("Geely GlyCar: ${m.name}() = $value%")
                    }
                }
                if (mName.contains("kwh") || mName.contains("energy") || mName.contains("power")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedKwh = value
                        log.add("Geely GlyCar: ${m.name}() = $value кВт·ч")
                    }
                }
                if (mName.contains("odometer") || mName.contains("totalmileage")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                        log.add("Geely GlyCar: ${m.name}() = $detectedOdo км")
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.GEELY_GLY_CAR,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Geely GlyCar API (${detectedSoc?.toInt()}%)",
                details = log
            )
        }

        return null
    }

    /**
     * Channel 2.5: Deepal S05 / Changan Auto API
     */
    private fun probeDeepalChangan(context: Context, log: MutableList<String>): TelemetryResult? {
        val deepalClasses = listOf(
            "com.changan.car.Car",
            "com.deepal.car.Car",
            "com.changan.os.car.Car",
            "com.changan.car.manager.CarManager"
        )

        var foundClass: Class<*>? = null
        for (name in deepalClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("Deepal/Changan: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }

        if (foundClass == null) return null

        val instance = try {
            val getInst = foundClass.getMethod("getInstance", Context::class.java)
            getInst.invoke(null, context)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("create", Context::class.java).invoke(null, context)
            } catch (_: Exception) { null }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        for (m in instance.javaClass.methods) {
            val mName = m.name.lowercase()
            if (m.parameterCount == 0) {
                if (mName.contains("soc") || mName.contains("battery") || mName.contains("level")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value in 1.0..100.0) {
                        detectedSoc = value
                        log.add("Deepal/Changan: ${m.name}() = $value%")
                    }
                }
                if (mName.contains("kwh") || mName.contains("energy") || mName.contains("remaining")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedKwh = value
                        log.add("Deepal/Changan: ${m.name}() = $value кВт·ч")
                    }
                }
                if (mName.contains("odometer") || mName.contains("mileage")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                        log.add("Deepal/Changan: ${m.name}() = $detectedOdo км")
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.DEEPAL_CHANGAN_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Deepal/Changan OS (${detectedSoc?.toInt()}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 2.1: Zeekr OS Dedicated API (Zeekr 001, 007, 7X, X, 009)
     */
    private fun probeZeekr(context: Context, log: MutableList<String>): TelemetryResult? {
        val zeekrClasses = listOf(
            "com.zeekr.car.Car",
            "com.zeekr.automotive.car.Car",
            "com.zeekr.sdk.vehicle.VehicleAPI",
            "com.zeekr.sdk.car.CarAPI"
        )
        var foundClass: Class<*>? = null
        for (name in zeekrClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("Zeekr API: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }
        if (foundClass == null) return null

        val instance = try {
            val getMethod = foundClass.getMethod("get")
            getMethod.invoke(null)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("getInstance", Context::class.java).invoke(null, context)
            } catch (_: Exception) {
                try {
                    foundClass.getMethod("create", Context::class.java).invoke(null, context)
                } catch (_: Exception) { null }
            }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        val targets = mutableListOf<Any>(instance)
        val subGetters = listOf("getEnergyAPI", "getCarAPI", "getVehicleAPI", "getBatteryAPI")
        for (sg in subGetters) {
            try {
                val sub = instance.javaClass.getMethod(sg).invoke(instance)
                if (sub != null) targets.add(sub)
            } catch (_: Exception) {}
        }

        for (target in targets) {
            for (m in target.javaClass.methods) {
                val mName = m.name.lowercase()
                if (m.parameterCount == 0) {
                    if (mName.contains("soc") || mName.contains("battery") || mName.contains("powerpercent")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value in 1.0..100.0) {
                            detectedSoc = value
                            log.add("Zeekr API: ${target.javaClass.simpleName}.${m.name}() = $value%")
                        }
                    }
                    if (mName.contains("kwh") || mName.contains("remainingpower") || mName.contains("energy")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value > 0.0) {
                            detectedKwh = value
                            log.add("Zeekr API: ${target.javaClass.simpleName}.${m.name}() = $value кВт·ч")
                        }
                    }
                    if (mName.contains("odometer") || mName.contains("mileage")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value > 0.0) {
                            detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                            log.add("Zeekr API: ${target.javaClass.simpleName}.${m.name}() = $detectedOdo км")
                        }
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.ZEEKR_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Zeekr OS API (${detectedSoc?.toInt() ?: "--"}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 2.2: BYD DiLink API (Song Plus, Yuan/Atto 3, Han, Tang, Seal, Dolphin, Seagull, Denza)
     */
    private fun probeBydDiLink(context: Context, log: MutableList<String>): TelemetryResult? {
        val bydClasses = listOf(
            "com.byd.car.Car",
            "com.byd.carmanager.CarManager",
            "com.byd.car.carmanager.CarManager",
            "com.byd.car.carmanager.energy.EnergyManager",
            "com.byd.car.carmanager.bms.BmsManager",
            "android.hardware.byd.car.CarManager"
        )
        var foundClass: Class<*>? = null
        for (name in bydClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("BYD DiLink: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }
        if (foundClass == null) return null

        val instance = try {
            foundClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("getInstance").invoke(null)
            } catch (_: Exception) {
                try {
                    foundClass.getMethod("create", Context::class.java).invoke(null, context)
                } catch (_: Exception) {
                    try {
                        foundClass.getDeclaredConstructor(Context::class.java).newInstance(context)
                    } catch (_: Exception) { null }
                }
            }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        val targets = mutableListOf<Any>(instance)
        val managerGetters = listOf("getEnergyManager", "getBmsManager", "getPowerManager", "getCarInfo")
        for (getter in managerGetters) {
            try {
                val sub = instance.javaClass.getMethod(getter).invoke(instance)
                if (sub != null) targets.add(sub)
            } catch (_: Exception) {}
        }

        for (target in targets) {
            for (m in target.javaClass.methods) {
                val mName = m.name.lowercase()
                if (m.parameterCount == 0) {
                    if (mName.contains("soc") || mName.contains("batterylevel") || mName.contains("batterypower") || mName.contains("electricpower")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value in 1.0..100.0) {
                            detectedSoc = value
                            log.add("BYD DiLink: ${target.javaClass.simpleName}.${m.name}() = $value%")
                        }
                    }
                    if (mName.contains("kwh") || mName.contains("remainpower") || mName.contains("energy")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value > 0.0) {
                            detectedKwh = value
                            log.add("BYD DiLink: ${target.javaClass.simpleName}.${m.name}() = $value кВт·ч")
                        }
                    }
                    if (mName.contains("odometer") || mName.contains("mileage") || mName.contains("totalmileage")) {
                        val value = invokeDoubleGetter(target, m)
                        if (value != null && value > 0.0) {
                            detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                            log.add("BYD DiLink: ${target.javaClass.simpleName}.${m.name}() = $detectedOdo км")
                        }
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.BYD_DILINK_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через BYD DiLink (${detectedSoc?.toInt() ?: "--"}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 2.6: Li Auto / Lixiang OS API (L6, L7, L8, L9, Mega)
     */
    private fun probeLiAuto(context: Context, log: MutableList<String>): TelemetryResult? {
        val liClasses = listOf(
            "com.lixiang.car.Car",
            "com.lixiang.car.manager.CarManager",
            "com.lixiang.os.car.CarService"
        )
        var foundClass: Class<*>? = null
        for (name in liClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("Li Auto: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }
        if (foundClass == null) return null

        val instance = try {
            foundClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("get", Context::class.java).invoke(null, context)
            } catch (_: Exception) { null }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        for (m in instance.javaClass.methods) {
            val mName = m.name.lowercase()
            if (m.parameterCount == 0) {
                if (mName.contains("soc") || mName.contains("battery") || mName.contains("level")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value in 1.0..100.0) {
                        detectedSoc = value
                        log.add("Li Auto: ${m.name}() = $value%")
                    }
                }
                if (mName.contains("kwh") || mName.contains("energy") || mName.contains("remaining")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedKwh = value
                        log.add("Li Auto: ${m.name}() = $value кВт·ч")
                    }
                }
                if (mName.contains("odometer") || mName.contains("mileage")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                        log.add("Li Auto: ${m.name}() = $detectedOdo км")
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.LI_AUTO_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Li Auto OS (${detectedSoc?.toInt() ?: "--"}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 2.7: Voyah OS API (Free, Dream, Passion)
     */
    private fun probeVoyah(context: Context, log: MutableList<String>): TelemetryResult? {
        val voyahClasses = listOf(
            "com.voyah.car.Car",
            "com.voyah.os.car.CarService",
            "com.voyah.carmanager.CarManager"
        )
        var foundClass: Class<*>? = null
        for (name in voyahClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("Voyah: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }
        if (foundClass == null) return null

        val instance = try {
            foundClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("create", Context::class.java).invoke(null, context)
            } catch (_: Exception) { null }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        for (m in instance.javaClass.methods) {
            val mName = m.name.lowercase()
            if (m.parameterCount == 0) {
                if (mName.contains("soc") || mName.contains("battery") || mName.contains("level")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value in 1.0..100.0) {
                        detectedSoc = value
                        log.add("Voyah: ${m.name}() = $value%")
                    }
                }
                if (mName.contains("kwh") || mName.contains("energy") || mName.contains("remaining")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedKwh = value
                        log.add("Voyah: ${m.name}() = $value кВт·ч")
                    }
                }
                if (mName.contains("odometer") || mName.contains("mileage")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                        log.add("Voyah: ${m.name}() = $detectedOdo км")
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.VOYAH_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Voyah OS (${detectedSoc?.toInt() ?: "--"}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 2.8: Xiaomi SU7 / HyperOS Car API
     */
    private fun probeXiaomiCar(context: Context, log: MutableList<String>): TelemetryResult? {
        val xiaomiClasses = listOf(
            "com.xiaomi.car.Car",
            "com.xiaomi.car.manager.CarManager"
        )
        var foundClass: Class<*>? = null
        for (name in xiaomiClasses) {
            try {
                foundClass = Class.forName(name)
                log.add("Xiaomi Car: найден класс $name")
                break
            } catch (_: ClassNotFoundException) {}
        }
        if (foundClass == null) return null

        val instance = try {
            foundClass.getMethod("getInstance", Context::class.java).invoke(null, context)
        } catch (_: Exception) {
            try {
                foundClass.getMethod("get", Context::class.java).invoke(null, context)
            } catch (_: Exception) { null }
        } ?: return null

        var detectedSoc: Double? = null
        var detectedKwh: Double? = null
        var detectedOdo: Double? = null

        for (m in instance.javaClass.methods) {
            val mName = m.name.lowercase()
            if (m.parameterCount == 0) {
                if (mName.contains("soc") || mName.contains("battery") || mName.contains("level")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value in 1.0..100.0) {
                        detectedSoc = value
                        log.add("Xiaomi: ${m.name}() = $value%")
                    }
                }
                if (mName.contains("kwh") || mName.contains("energy") || mName.contains("remaining")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedKwh = value
                        log.add("Xiaomi: ${m.name}() = $value кВт·ч")
                    }
                }
                if (mName.contains("odometer") || mName.contains("mileage")) {
                    val value = invokeDoubleGetter(instance, m)
                    if (value != null && value > 0.0) {
                        detectedOdo = if (value > 1_000_000.0) value / 1000.0 else value
                        log.add("Xiaomi: ${m.name}() = $detectedOdo км")
                    }
                }
            }
        }

        if (detectedSoc != null || detectedKwh != null) {
            return TelemetryResult(
                success = true,
                source = SourceType.XIAOMI_CAR_API,
                batterySocPercent = detectedSoc,
                remainingKwh = detectedKwh,
                odometerKm = detectedOdo,
                message = "Подключено через Xiaomi HyperOS (${detectedSoc?.toInt() ?: "--"}%)",
                details = log
            )
        }
        return null
    }

    /**
     * Channel 3: System & Global Settings
     * Reads CAN bus status mirrored into Settings.System / Settings.Global across
     * all major Chinese & international EV head units.
     */
    private fun probeSystemSettings(context: Context, log: MutableList<String>): TelemetryResult? {
        val resolver = context.contentResolver
        val socKeys = listOf(
            // Geely / Lynk&Co / ECARX
            "car_battery_soc",
            "ev_battery_soc",
            "battery_soc",
            "ecarx_battery_soc",
            "gly_battery_soc",
            "deepal_battery_soc",
            "changan_battery_soc",
            "deepal_soc",
            "changan_soc",
            "gkui_battery_soc",
            "geely_battery_soc",
            // Zeekr
            "zeekr_battery_soc",
            "zeekr_soc",
            "zeekr_remaining_power",
            "zeekr_car_soc",
            // BYD DiLink
            "byd_battery_soc",
            "byd_soc",
            "battery_soc_display",
            "byd_instrument_soc",
            "bms_soc_percent",
            "byd_ev_soc",
            // Li Auto / Lixiang
            "li_battery_soc",
            "li_soc",
            "lixiang_battery_soc",
            "lixiang_soc",
            // Voyah
            "voyah_battery_soc",
            "voyah_soc",
            // Xiaomi SU7
            "xiaomi_battery_soc",
            "xiaomi_car_soc",
            "mi_car_soc",
            // Chery / Exeed / Jaecoo / Omoda
            "chery_battery_soc",
            "exeed_battery_soc",
            // Great Wall / ORA / Haval / Tank
            "ora_battery_soc",
            "gwm_battery_soc",
            "haval_battery_soc",
            // Huawei / HarmonyOS / Avatr / Aito
            "huawei_car_battery_soc",
            "harmony_battery_soc",
            "aito_battery_soc",
            // Generic AOSP / Cockpit status bar keys
            "soc_display",
            "instrument_battery_soc",
            "bms_soc",
            "ev_soc",
            "soc"
        )

        var foundSoc: Double? = null
        var matchedKey: String? = null

        for (key in socKeys) {
            try {
                val sysVal = Settings.System.getInt(resolver, key, -1)
                if (sysVal in 1..100) {
                    foundSoc = sysVal.toDouble()
                    matchedKey = "Settings.System.$key"
                    break
                }
            } catch (_: Exception) {}

            try {
                val globVal = Settings.Global.getInt(resolver, key, -1)
                if (globVal in 1..100) {
                    foundSoc = globVal.toDouble()
                    matchedKey = "Settings.Global.$key"
                    break
                }
            } catch (_: Exception) {}
        }

        // Try reading remaining energy / kWh
        val kwhKeys = listOf(
            "car_battery_kwh",
            "battery_remaining_kwh",
            "remaining_energy",
            "battery_capacity",
            "byd_remaining_power",
            "zeekr_remaining_kwh",
            "li_battery_remaining_kwh",
            "voyah_remaining_kwh",
            "ev_remaining_energy"
        )
        var foundKwh: Double? = null
        for (key in kwhKeys) {
            try {
                val sysVal = Settings.System.getFloat(resolver, key, -1f)
                if (sysVal > 0f) {
                    foundKwh = sysVal.toDouble()
                    break
                }
            } catch (_: Exception) {}
        }

        // Try reading odometer
        val odoKeys = listOf(
            "car_odometer",
            "total_odometer",
            "vehicle_mileage",
            "byd_remaining_mileage",
            "total_mileage",
            "vehicle_odometer"
        )
        var foundOdo: Double? = null
        for (key in odoKeys) {
            try {
                val sysVal = Settings.System.getFloat(resolver, key, -1f)
                if (sysVal > 0f) {
                    foundOdo = sysVal.toDouble()
                    break
                }
            } catch (_: Exception) {}
        }

        if (foundSoc != null) {
            log.add("System Settings: найден ключ $matchedKey = $foundSoc%")
            return TelemetryResult(
                success = true,
                source = SourceType.SYSTEM_SETTINGS,
                batterySocPercent = foundSoc,
                remainingKwh = foundKwh,
                odometerKm = foundOdo,
                message = "Подключено через $matchedKey ($foundSoc%)",
                details = log
            )
        }

        log.add("System Settings: ключи SOC не обнаружены")
        return null
    }

    /**
     * Channel 4: Android System Battery Broadcast (Intent.ACTION_BATTERY_CHANGED)
     * On Flyme Auto and ECARX, the head unit firmware frequently mirrors traction battery SOC
     * directly into the system battery manager.
     */
    private fun probeBatteryBroadcast(context: Context, log: MutableList<String>): TelemetryResult? {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent == null) {
            log.add("Battery Broadcast: Intent.ACTION_BATTERY_CHANGED не получен")
            return null
        }

        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

        var socPercent: Double? = null
        if (level >= 0 && scale > 0) {
            val calc = (level.toDouble() / scale.toDouble()) * 100.0
            if (calc in 1.0..100.0) {
                socPercent = calc
            }
        }

        // Also check if intent extras contain specific EV fields
        var extraKwh: Double? = null
        var extraOdo: Double? = null
        val bundle = batteryIntent.extras
        if (bundle != null) {
            for (key in bundle.keySet()) {
                val lk = key.lowercase()
                if (lk.contains("soc") || lk.contains("ev_level")) {
                    val v = bundle.get(key)
                    if (v is Number && v.toDouble() in 1.0..100.0) {
                        socPercent = v.toDouble()
                        log.add("Battery Broadcast: Extra $key = $socPercent%")
                    }
                }
                if (lk.contains("kwh") || lk.contains("energy")) {
                    val v = bundle.get(key)
                    if (v is Number && v.toDouble() > 0.0) {
                        extraKwh = v.toDouble()
                        log.add("Battery Broadcast: Extra $key = $extraKwh кВт·ч")
                    }
                }
                if (lk.contains("odometer") || lk.contains("odo")) {
                    val v = bundle.get(key)
                    if (v is Number && v.toDouble() > 0.0) {
                        extraOdo = v.toDouble()
                        log.add("Battery Broadcast: Extra $key = $extraOdo км")
                    }
                }
            }
        }

        if (socPercent != null) {
            log.add("Battery Broadcast: получено $socPercent%")
            return TelemetryResult(
                success = true,
                source = SourceType.BATTERY_BROADCAST,
                batterySocPercent = socPercent,
                remainingKwh = extraKwh,
                odometerKm = extraOdo,
                message = "Подключено через системный статус батареи ГУ (${socPercent.toInt()}%)",
                details = log
            )
        }

        return null
    }

    /**
     * Channel 5: Android Automotive Car API (fallback)
     */
    private fun probeAndroidCarApi(context: Context, log: MutableList<String>): TelemetryResult? {
        val carClass = try {
            Class.forName("android.car.Car")
        } catch (_: ClassNotFoundException) {
            log.add("Android Car API: класс android.car.Car отсутствует")
            return null
        }

        return try {
            val createCarMethod = carClass.getMethod("createCar", Context::class.java)
            val carInstance = createCarMethod.invoke(null, context) ?: return null

            val propertyServiceField = carClass.getField("PROPERTY_SERVICE")
            val propertyServiceName = propertyServiceField.get(null) as String
            val getCarManagerMethod = carClass.getMethod("getCarManager", String::class.java)
            val carPropertyManager = getCarManagerMethod.invoke(carInstance, propertyServiceName) ?: return null

            val managerClass = carPropertyManager.javaClass
            val getFloatPropertyMethod = managerClass.getMethod(
                "getFloatProperty",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )

            val evBatteryLevelPropId = 291504906
            val evBatteryCapacityPropId = 291504907
            val odometerPropId = 291504644

            val currentLevel = getFloatPropertyMethod.invoke(carPropertyManager, evBatteryLevelPropId, 0) as? Float
            val capacityLevel = try {
                getFloatPropertyMethod.invoke(carPropertyManager, evBatteryCapacityPropId, 0) as? Float
            } catch (_: Exception) { null }

            val soc = if (currentLevel != null && currentLevel > 0f) {
                if (capacityLevel != null && capacityLevel > 0f) {
                    ((currentLevel / capacityLevel) * 100f).toDouble()
                } else if (currentLevel <= 100f) {
                    currentLevel.toDouble()
                } else null
            } else null

            val odoMeters = try {
                getFloatPropertyMethod.invoke(carPropertyManager, odometerPropId, 0) as? Float
            } catch (_: Exception) { null }
            val odo = if (odoMeters != null && odoMeters > 0f) (odoMeters / 1000.0) else null

            if (soc != null) {
                log.add("Android Car API: получено $soc%")
                TelemetryResult(
                    success = true,
                    source = SourceType.ANDROID_CAR_API,
                    batterySocPercent = soc,
                    odometerKm = odo,
                    message = "Подключено через Android Automotive CarPropertyManager ($soc%)",
                    details = log
                )
            } else null
        } catch (sec: SecurityException) {
            log.add("Android Car API: требуется системное разрешение CAR_ENERGY")
            null
        } catch (e: Exception) {
            log.add("Android Car API: ошибка (${e.message})")
            null
        }
    }

    /**
     * Automatically updates the active car in database with live telemetry
     */
    private suspend fun applyTelemetryToActiveCar(context: Context, telemetry: TelemetryResult) {
        val soc = telemetry.batterySocPercent ?: return
        try {
            val db = AppDatabase.getDatabase(context)
            val settingsManager = SettingsManager(context)
            val settings = settingsManager.settingsFlow.firstOrNull()
            val allCars = db.carDao().getAllCarsList()
            val activeCar = allCars.find { it.id == settings?.selectedCarId }
                ?: allCars.find { it.isActive }
                ?: allCars.firstOrNull()

            if (activeCar != null) {
                val clampedSoc = soc.coerceIn(1.0, 100.0)
                val newOdo = if (telemetry.odometerKm != null && telemetry.odometerKm > activeCar.initialOdometer) {
                    telemetry.odometerKm
                } else {
                    activeCar.initialOdometer
                }

                // If remaining kWh is known, optionally verify usable capacity
                val newCapacity = if (telemetry.remainingKwh != null && telemetry.remainingKwh > 5.0 && clampedSoc > 10.0) {
                    val calcCapacity = (telemetry.remainingKwh / (clampedSoc / 100.0))
                    if (calcCapacity in 20.0..150.0) calcCapacity else activeCar.usableCapacityKwh
                } else {
                    activeCar.usableCapacityKwh
                }

                val updated = activeCar.copy(
                    currentSoc = clampedSoc,
                    initialOdometer = newOdo,
                    usableCapacityKwh = newCapacity
                )
                db.carDao().updateCar(updated)
                VoltLedgerWidgetProvider.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply vehicle telemetry to car: ${e.message}")
        }
    }

    private fun invokeDoubleGetter(target: Any, method: Method): Double? {
        return try {
            when (val res = method.invoke(target)) {
                is Double -> res
                is Float -> res.toDouble()
                is Int -> res.toDouble()
                is Long -> res.toDouble()
                is Number -> res.toDouble()
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
