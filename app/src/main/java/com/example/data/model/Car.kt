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
    val isActive: Boolean = true
)
