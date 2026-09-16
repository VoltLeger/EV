package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey
    val id: String = "pilot_" + UUID.randomUUID().toString().replace("-", "").take(12),
    val callsign: String = "VOLT-" + (1000..9999).random(),
    val displayName: String = "EV Пилот",
    val bio: String = "Электромобилист",
    val avatarEffect: String = "neon_cyan", // "neon_cyan", "plasma_purple", "golden_ribbon", "emerald_glow", "lightning_spark"
    val avatarIcon: String = "bolt", // "bolt", "car", "speed", "leaf", "star"
    val countryCode: String = "BY",
    val isLeaderboardOptIn: Boolean = true,
    val totalXp: Long = 0,
    val rankTier: String = "Новичок", // "Новичок", "Энерджи-Драйвер", "Мастер Киловатт", "Гроссмейстер Вольт"
    val createdAt: Long = System.currentTimeMillis(),
    val lastActiveAt: Long = System.currentTimeMillis()
)
