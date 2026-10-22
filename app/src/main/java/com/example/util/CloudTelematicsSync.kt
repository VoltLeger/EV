package com.example.util

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/**
 * Architectural Foundation for Cloud Telematics Sync ("Volt Cloud Relay").
 * Connects the in-car head unit (Geely EX5/EX2, Deepal S05) with the owner's smartphone.
 *
 * Architecture Flow:
 * [ Car Head Unit (In-Car Node) ]
 *        │ (reads SOC% via VehicleTelemetryBridge every 1-5 mins or on charge change)
 *        ▼
 * [ Cloud Relay / Firestore / REST Server ]
 *        │ (realtime websocket / push notification / poll)
 *        ▼
 * [ Owner's Smartphone (Mobile Client) ]
 *   -> Phone app & Home Screen Widget show live battery, charging power & range!
 */
object CloudTelematicsSync {

    data class TelemetryPacket(
        val syncCode: String,
        val carName: String,
        val batterySoc: Double,
        val remainingKwh: Double?,
        val isCharging: Boolean,
        val stationType: String?,
        val chargingPowerKw: Double?,
        val odometerKm: Double?,
        val estimatedRangeKm: Double?,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Generates a human-friendly pairing code for pairing Car Tablet with Owner Smartphone
     * e.g. "VOLT-8392"
     */
    fun generatePairingCode(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val rnd = SecureRandom()
        val suffix = (0 until 4).map { chars[rnd.nextInt(chars.length)] }.joinToString("")
        return "VOLT-$suffix"
    }

    /**
     * Builds the current telemetry packet from the active car and database to be sent to cloud
     */
    suspend fun buildCurrentCarPacket(context: Context, pairingCode: String): TelemetryPacket? = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        val settings = settingsManager.settingsFlow.firstOrNull()
        val allCars = db.carDao().getAllCarsList()
        val activeCar = allCars.find { it.id == settings?.selectedCarId }
            ?: allCars.find { it.isActive }
            ?: allCars.firstOrNull() ?: return@withContext null

        val allSessions = db.chargingSessionDao().getAllSessionsDirect()
        val activeSession = allSessions.find { it.status == "active" }

        val capacity = activeCar.usableCapacityKwh.takeIf { it > 0.0 } ?: 57.0
        val passport = activeCar.passportConsumption.takeIf { it > 0.0 } ?: 16.0
        val completed = allSessions.filter { it.carId == activeCar.id && it.status == "completed" }
        val lastThree = EVCalculator.calculateLastThreeChargesConsumption(completed, capacity)
        val cons = if (lastThree.hasEnoughData && lastThree.avgConsumption != null && lastThree.avgConsumption > 0.0) {
            lastThree.avgConsumption
        } else {
            EVCalculator.calculateEffectiveMonthConsumption(completed, capacity, passport).consumption ?: passport
        }

        val estimatedRange = EVCalculator.calculateRangeForSoc(capacity, activeCar.currentSoc, cons)
        val remainingKwh = capacity * (activeCar.currentSoc / 100.0)

        TelemetryPacket(
            syncCode = pairingCode,
            carName = activeCar.name,
            batterySoc = activeCar.currentSoc,
            remainingKwh = remainingKwh,
            isCharging = activeSession != null,
            stationType = activeSession?.stationType,
            chargingPowerKw = activeSession?.avgPowerKw,
            odometerKm = activeCar.initialOdometer,
            estimatedRangeKm = estimatedRange,
            timestamp = System.currentTimeMillis()
        )
    }
}
