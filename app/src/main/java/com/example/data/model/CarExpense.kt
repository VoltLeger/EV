package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "car_expenses",
    indices = [
        Index(value = ["carId", "timestamp"]),
        Index(value = ["category"]),
        Index(value = ["timestamp"])
    ]
)
data class CarExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val carId: Long,
    val category: String, // e.g. "Мойка", "ТО", "Страховка", "Шиномонтаж", "Парковка", "Омывайка и химия", "Ремонт", "Тюнинг", "Налоги", "Штрафы", "Другое"
    val amount: Double,
    val currency: String = "BYN",
    val timestamp: Long = System.currentTimeMillis(),
    val odometer: Double? = null,
    val comment: String? = null
)
