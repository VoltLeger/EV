package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cars")
data class Car(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val declaredCapacityKwh: Double,
    val usableCapacityKwh: Double,
    val initialOdometer: Double,
    val currentSoc: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
