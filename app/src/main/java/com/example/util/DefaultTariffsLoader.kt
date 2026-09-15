package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.Operator
import com.example.data.model.Tag
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DefaultTariffsLoader {

    private const val TAG = "DefaultTariffsLoader"
    const val FILE_NAME = "default_tariffs.json"

    val FALLBACK_OPERATORS = listOf(
        Operator(
            name = "Malanka (Белоруснефть)",
            type = "both",
            priceAc = 0.55,
            priceDc = 0.73,
            nightPriceDc = 0.45,
            nightStartHour = 23,
            nightEndHour = 6,
            comment = "Плата за резервирование коннектора",
            isBuiltin = true
        ),
        Operator(
            name = "Evika (Белтелеком)",
            type = "both",
            priceAc = 0.54,
            priceDc = 0.72,
            penaltyIdlePerMin = 0.05,
            penaltyFreeMinutes = 30,
            comment = "Штраф за простой 0.05 BYN/мин после 30 мин",
            isBuiltin = true
        ),
        Operator(
            name = "forEVo (включая А-100)",
            type = "both",
            priceAc = 0.55,
            priceDc = 0.73,
            nightPriceDc = 0.55,
            nightStartHour = 21,
            nightEndHour = 8,
            comment = "Ночной тариф DC при старте 21:00–08:30",
            isBuiltin = true
        ),
        Operator(
            name = "BatteryFly",
            type = "both",
            priceAc = 0.46,
            priceDc = 0.65,
            nightPriceDc = 0.49,
            nightStartHour = 22,
            nightEndHour = 9,
            comment = "Ночной тариф 22:00–09:00",
            isBuiltin = true
        ),
        Operator(
            name = "Zaryadka",
            type = "both",
            priceAc = 0.55,
            priceDc = 0.73,
            penaltyIdlePerMin = 0.30,
            penaltyFreeMinutes = 15,
            comment = "Штраф за простой DC 0.30 BYN/мин после 15 мин",
            isBuiltin = true
        ),
        Operator(
            name = "Дача / Дом",
            type = "AC",
            priceAc = 0.36,
            priceDc = 0.36,
            comment = "Бытовой тариф для дома и дачи",
            isBuiltin = true
        ),
        Operator(
            name = "Бесплатная зарядка",
            type = "both",
            priceAc = 0.00001,
            priceDc = 0.00001,
            comment = "Бонусные или бесплатные киловатты",
            isBuiltin = true,
            isFree = true
        ),
        Operator(
            name = "Другое",
            type = "both",
            priceAc = 0.50,
            priceDc = 0.70,
            comment = "Ручной ввод параметров",
            isBuiltin = true
        )
    )

    fun loadTariffsFromAssets(context: Context): List<Operator> {
        return try {
            val jsonString = context.assets.open(FILE_NAME).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
            parseTariffsJson(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading $FILE_NAME from assets, using fallback: ${e.message}")
            FALLBACK_OPERATORS
        }
    }

    fun parseTariffsJson(jsonString: String): List<Operator> {
        val result = mutableListOf<Operator>()
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val operator = Operator(
                name = obj.optString("name", "Unknown"),
                type = obj.optString("type", "both"),
                priceAc = obj.optDouble("priceAc", 0.55),
                priceDc = obj.optDouble("priceDc", 0.73),
                nightPriceAc = if (obj.has("nightPriceAc") && !obj.isNull("nightPriceAc")) obj.getDouble("nightPriceAc") else null,
                nightPriceDc = if (obj.has("nightPriceDc") && !obj.isNull("nightPriceDc")) obj.getDouble("nightPriceDc") else null,
                nightStartHour = obj.optInt("nightStartHour", 23),
                nightEndHour = obj.optInt("nightEndHour", 6),
                penaltyIdlePerMin = obj.optDouble("penaltyIdlePerMin", 0.0),
                penaltyFreeMinutes = obj.optInt("penaltyFreeMinutes", 0),
                comment = obj.optString("comment", ""),
                isBuiltin = obj.optBoolean("isBuiltin", true),
                isFree = obj.optBoolean("isFree", false)
            )
            result.add(operator)
        }
        return if (result.isNotEmpty()) result else FALLBACK_OPERATORS
    }

    suspend fun syncTariffsAndCleanDuplicates(context: Context, db: AppDatabase) {
        try {
            // 1. Clean up any existing duplicate operators and tags in SQLite
            db.operatorDao().deleteDuplicateOperators()
            db.tagDao().deleteDuplicateTags()

            // 2. Load standard tariffs from external default_tariffs.json
            val operatorsFromFile = loadTariffsFromAssets(context)

            // 3. Upsert operators by name to prevent duplicate creation
            for (op in operatorsFromFile) {
                val existing = db.operatorDao().getOperatorByName(op.name)
                if (existing == null) {
                    db.operatorDao().insertOperator(op)
                } else if (existing.isBuiltin) {
                    // Update tariff values from file without affecting user ID
                    db.operatorDao().updateOperator(
                        existing.copy(
                            priceAc = op.priceAc,
                            priceDc = op.priceDc,
                            nightPriceAc = op.nightPriceAc,
                            nightPriceDc = op.nightPriceDc,
                            nightStartHour = op.nightStartHour,
                            nightEndHour = op.nightEndHour,
                            penaltyIdlePerMin = op.penaltyIdlePerMin,
                            penaltyFreeMinutes = op.penaltyFreeMinutes,
                            comment = op.comment,
                            isFree = op.isFree
                        )
                    )
                }
            }

            // 4. Default tags if needed
            if (db.tagDao().countTags() == 0) {
                val defaultTags = listOf(
                    Tag(name = "Дом", color = 0xFF10B981, isBuiltin = true),
                    Tag(name = "Работа", color = 0xFF3B82F6, isBuiltin = true),
                    Tag(name = "Трасса", color = 0xFFF59E0B, isBuiltin = true),
                    Tag(name = "Быстрая", color = 0xFF8B5CF6, isBuiltin = true),
                    Tag(name = "Ночная", color = 0xFF6366F1, isBuiltin = true)
                )
                db.tagDao().insertAll(defaultTags)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tariffs: ${e.message}", e)
        }
    }
}
