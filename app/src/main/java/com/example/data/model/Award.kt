package com.example.data.model

enum class AwardCategory(val title: String) {
    ENERGY("Энергия"),
    DISTANCE("Пробег"),
    NIGHT("Ночные зарядки"),
    DC_POWER("Мощность"),
    EFFICIENCY("Эффективность"),
    NETWORK("Сеть станций")
}

data class Award(
    val id: String,
    val title: String,
    val description: String,
    val category: AwardCategory,
    val currentProgress: Double,
    val maxProgress: Double,
    val unit: String,
    val isUnlocked: Boolean,
    val avatarEffectReward: String,
    val avatarEffectName: String,
    val xpReward: Int,
    val iconKey: String // "bolt", "speed", "moon", "flash", "leaf", "station", "trophy"
) {
    val progressPercent: Float
        get() = if (maxProgress <= 0) 0f else (currentProgress / maxProgress).coerceIn(0.0, 1.0).toFloat()
}
