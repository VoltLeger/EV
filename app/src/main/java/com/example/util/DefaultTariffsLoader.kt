package com.example.util

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.local.dataStore
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.data.model.Tag
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

object DefaultTariffsLoader {

    private const val TAG = "DefaultTariffsLoader"
    const val FILE_NAME = "default_tariffs.json"

    val FALLBACK_OPERATORS = listOf(
        // AC Operators
        Operator(
            name = "Домашняя зарядка",
            type = "AC",
            subType = "Дом",
            priceAc = 0.36,
            nightPriceAc = 0.18,
            nightStartHour = 22,
            nightEndHour = 6,
            comment = "Бытовой тариф AC (Дом)",
            isBuiltin = true
        ),
        Operator(
            name = "Дачная зарядка",
            type = "AC",
            subType = "Дача",
            priceAc = 0.40,
            nightPriceAc = 0.20,
            nightStartHour = 22,
            nightEndHour = 6,
            comment = "Бытовой тариф AC (Дача)",
            isBuiltin = true
        ),
        Operator(
            name = "Malanka AC",
            type = "AC",
            subType = "Город",
            priceAc = 0.55,
            comment = "Медленная зарядка AC",
            isBuiltin = true
        ),
        Operator(
            name = "Evika AC",
            type = "AC",
            subType = "Город",
            priceAc = 0.54,
            comment = "Белтелеком AC",
            isBuiltin = true
        ),

        // DC Operators
        Operator(
            name = "Malanka DC",
            type = "DC",
            priceDc = 0.73,
            nightPriceDc = 0.45,
            nightStartHour = 23,
            nightEndHour = 6,
            comment = "Быстрая зарядка DC (Белоруснефть)",
            isBuiltin = true
        ),
        Operator(
            name = "Evika DC",
            type = "DC",
            priceDc = 0.72,
            penaltyIdlePerMin = 0.05,
            penaltyFreeMinutes = 30,
            comment = "Штраф за простой 0.05/мин после 30 мин",
            isBuiltin = true
        ),
        Operator(
            name = "forEVo DC (А-100)",
            type = "DC",
            priceDc = 0.73,
            nightPriceDc = 0.55,
            nightStartHour = 21,
            nightEndHour = 8,
            comment = "Ночной тариф DC 21:00–08:30",
            isBuiltin = true
        ),
        Operator(
            name = "BatteryFly DC",
            type = "DC",
            priceDc = 0.65,
            nightPriceDc = 0.49,
            nightStartHour = 22,
            nightEndHour = 9,
            comment = "Ночной тариф 22:00–09:00",
            isBuiltin = true
        ),
        Operator(
            name = "Zaryadka DC",
            type = "DC",
            priceDc = 0.73,
            penaltyIdlePerMin = 0.30,
            penaltyFreeMinutes = 15,
            comment = "Штраф за простой 0.30/мин после 15 мин",
            isBuiltin = true
        ),
        Operator(
            name = "Бесплатная",
            type = "AC",
            subType = "Бонус",
            priceAc = 0.00001,
            priceDc = 0.00001,
            comment = "Бесплатные киловатты",
            isBuiltin = true,
            isFree = true
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
                subType = obj.optString("subType", ""),
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

    suspend fun syncTariffsAndCleanDuplicates(
        context: Context,
        db: AppDatabase,
        forceReinsert: Boolean = false
    ) {
        try {
            // 1. Clean up any existing duplicate operators and tags in SQLite
            db.operatorDao().deleteDuplicateOperators()
            db.tagDao().deleteDuplicateTags()

            val prefs = context.dataStore.data.first()
            val alreadySeeded = prefs[SettingsManager.KEY_INITIAL_SEED_COMPLETED] ?: false
            val currentOpCount = db.operatorDao().countOperators()

            // Only seed default operators if never seeded before, or if user explicitly requested forceReinsert
            if ((!alreadySeeded && currentOpCount == 0) || forceReinsert) {
                // 2. Load standard tariffs from external default_tariffs.json
                val operatorsFromFile = loadTariffsFromAssets(context)

                // 3. Upsert operators by name to prevent duplicate creation
                for (op in operatorsFromFile) {
                    val existing = db.operatorDao().getOperatorByName(op.name)
                    if (existing == null) {
                        db.operatorDao().insertOperator(op)
                    } else if (existing.isBuiltin && forceReinsert) {
                        // Update tariff values from file without affecting user ID
                        db.operatorDao().updateOperator(
                            existing.copy(
                                type = op.type,
                                subType = op.subType,
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

                // 4. Default car if none exists
                if (db.carDao().countCars() == 0) {
                    val defaultCar = Car(
                        name = "Электромобиль",
                        declaredCapacityKwh = 60.0,
                        usableCapacityKwh = 58.0,
                        initialOdometer = 12000.0,
                        currentSoc = 65.0,
                        isActive = true
                    )
                    db.carDao().insertCar(defaultCar)
                }

                // 5. Default tags if needed
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

                context.dataStore.edit {
                    it[SettingsManager.KEY_INITIAL_SEED_COMPLETED] = true
                }
            } else if (!alreadySeeded) {
                // If operators already existed in DB, mark seeded so we don't re-insert deleted items later
                context.dataStore.edit {
                    it[SettingsManager.KEY_INITIAL_SEED_COMPLETED] = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing tariffs: ${e.message}", e)
        }
    }
}
