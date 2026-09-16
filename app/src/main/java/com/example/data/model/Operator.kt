package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "operators",
    indices = [
        Index(value = ["name"])
    ]
)
data class Operator(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "AC" or "DC"
    val subType: String = "", // e.g. "Дом", "Дача" for AC
    val priceAc: Double = 0.55,
    val priceDc: Double = 0.73,
    val nightPriceAc: Double? = null,
    val nightPriceDc: Double? = null,
    val nightStartHour: Int = 23,
    val nightEndHour: Int = 6,
    val penaltyIdlePerMin: Double = 0.0,
    val penaltyFreeMinutes: Int = 0,
    val comment: String = "",
    val isBuiltin: Boolean = false,
    val isFree: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
