package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "charging_sessions",
    indices = [
        Index(value = ["carId", "startTime"]),
        Index(value = ["status"]),
        Index(value = ["startTime"])
    ]
)
data class ChargingSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val startOdometer: Double,
    val startSoc: Double,
    val endSoc: Double = 0.0,
    val kwhDeliveredByStation: Double = 0.0,
    val kwhReceivedByCar: Double? = null,
    val pricePerKwh: Double,
    val energyCost: Double = 0.0,
    val penaltyCost: Double = 0.0,
    val fixedAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val currency: String = "BYN",
    val stationType: String, // "AC" or "DC"
    val operatorId: Long? = null,
    val operatorName: String = "",
    val operatorComment: String? = null,
    val avgPowerKw: Double? = null,
    val isFreeCharge: Boolean = false,
    val nightTariffApplied: Boolean = false,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: String = "active" // "active" or "completed"
)
