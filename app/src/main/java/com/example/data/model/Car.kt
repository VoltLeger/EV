package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cars",
    indices = [
        Index(value = ["isActive"])
    ]
)
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val declaredCapacityKwh: Double,
    val usableCapacityKwh: Double,
    val initialOdometer: Double,
    val currentSoc: Double,
    val passportConsumption: Double = 16.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    // Car Passport additional fields
    val licensePlate: String? = null,
    val purchaseDate: String? = null,
    val purchasePrice: Double? = null,
    val manufactureYear: Int? = null,
    val vin: String? = null,
    val registrationNumber: String? = null,
    val insuranceNumber: String? = null,
    val notes: String? = null,
    val photoUri: String? = null
)
