package com.example.util

import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val cars: List<Car>,
    val sessions: List<ChargingSession>,
    val operators: List<Operator>,
    val tags: List<Tag>
)

object CsvJsonBackupHelper {

    fun exportSessionsToCsv(sessions: List<ChargingSession>): String {
        val sb = StringBuilder()
        sb.append("id,car_id,start_time,end_time,start_odometer,start_soc,end_soc,station_type,operator_name,kwh_delivered,kwh_received,price_per_kwh,energy_cost,penalty_cost,fixed_amount,total_cost,currency,status\n")
        for (s in sessions) {
            sb.append("${s.id},")
            sb.append("${s.carId},")
            sb.append("${s.startTime},")
            sb.append("${s.endTime ?: ""},")
            sb.append("${s.startOdometer},")
            sb.append("${s.startSoc},")
            sb.append("${s.endSoc},")
            sb.append("\"${s.stationType}\",")
            sb.append("\"${s.operatorName.replace("\"", "\"\"")}\",")
            sb.append("${s.kwhDeliveredByStation},")
            sb.append("${s.kwhReceivedByCar ?: ""},")
            sb.append("${s.pricePerKwh},")
            sb.append("${s.energyCost},")
            sb.append("${s.penaltyCost},")
            sb.append("${s.fixedAmount},")
            sb.append("${s.totalCost},")
            sb.append("\"${s.currency}\",")
            sb.append("\"${s.status}\"\n")
        }
        return sb.toString()
    }

    fun parseSessionsFromCsv(csvText: String, carId: Long): List<ChargingSession> {
        val list = mutableListOf<ChargingSession>()
        val lines = csvText.lines()
        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            if (tokens.size >= 17) {
                try {
                    val clean = tokens.map { it.trim().removeSurrounding("\"") }
                    val session = ChargingSession(
                        carId = carId,
                        startTime = clean[2].toLongOrNull() ?: System.currentTimeMillis(),
                        endTime = clean[3].toLongOrNull(),
                        startOdometer = clean[4].toDoubleOrNull() ?: 0.0,
                        startSoc = clean[5].toDoubleOrNull() ?: 0.0,
                        endSoc = clean[6].toDoubleOrNull() ?: 0.0,
                        stationType = clean[7].ifEmpty { "AC" },
                        operatorName = clean[8],
                        kwhDeliveredByStation = clean[9].toDoubleOrNull() ?: 0.0,
                        kwhReceivedByCar = clean[10].toDoubleOrNull(),
                        pricePerKwh = clean[11].toDoubleOrNull() ?: 0.5,
                        energyCost = clean[12].toDoubleOrNull() ?: 0.0,
                        penaltyCost = clean[13].toDoubleOrNull() ?: 0.0,
                        fixedAmount = clean[14].toDoubleOrNull() ?: 0.0,
                        totalCost = clean[15].toDoubleOrNull() ?: 0.0,
                        currency = clean[16].ifEmpty { "BYN" },
                        status = if (clean.size > 17) clean[17] else "completed"
                    )
                    list.add(session)
                } catch (_: Exception) {
                    // Skip invalid line
                }
            }
        }
        return list
    }

    fun exportFullBackupJson(
        cars: List<Car>,
        sessions: List<ChargingSession>,
        operators: List<Operator>,
        tags: List<Tag>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "VoltLedger")
        root.put("timestamp", System.currentTimeMillis())

        val carsArray = JSONArray()
        for (c in cars) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("declaredCapacityKwh", c.declaredCapacityKwh)
            obj.put("usableCapacityKwh", c.usableCapacityKwh)
            obj.put("initialOdometer", c.initialOdometer)
            obj.put("currentSoc", c.currentSoc)
            obj.put("passportConsumption", c.passportConsumption)
            obj.put("createdAt", c.createdAt)
            carsArray.put(obj)
        }
        root.put("cars", carsArray)

        val sessionsArray = JSONArray()
        for (s in sessions) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("carId", s.carId)
            obj.put("startOdometer", s.startOdometer)
            obj.put("startSoc", s.startSoc)
            obj.put("endSoc", s.endSoc)
            obj.put("kwhDeliveredByStation", s.kwhDeliveredByStation)
            if (s.kwhReceivedByCar != null) obj.put("kwhReceivedByCar", s.kwhReceivedByCar)
            obj.put("pricePerKwh", s.pricePerKwh)
            obj.put("energyCost", s.energyCost)
            obj.put("penaltyCost", s.penaltyCost)
            obj.put("fixedAmount", s.fixedAmount)
            obj.put("totalCost", s.totalCost)
            obj.put("currency", s.currency)
            obj.put("stationType", s.stationType)
            obj.put("operatorName", s.operatorName)
            obj.put("isFreeCharge", s.isFreeCharge)
            obj.put("nightTariffApplied", s.nightTariffApplied)
            obj.put("startTime", s.startTime)
            if (s.endTime != null) obj.put("endTime", s.endTime)
            obj.put("status", s.status)
            sessionsArray.put(obj)
        }
        root.put("sessions", sessionsArray)

        return root.toString(2)
    }

    fun parseFullBackupJson(jsonString: String): BackupData? {
        return try {
            val root = JSONObject(jsonString)
            val cars = mutableListOf<Car>()
            val carsArray = root.optJSONArray("cars") ?: JSONArray()
            for (i in 0 until carsArray.length()) {
                val obj = carsArray.getJSONObject(i)
                cars.add(
                    Car(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "EV"),
                        declaredCapacityKwh = obj.optDouble("declaredCapacityKwh", 60.0),
                        usableCapacityKwh = obj.optDouble("usableCapacityKwh", 57.0),
                        initialOdometer = obj.optDouble("initialOdometer", 0.0),
                        currentSoc = obj.optDouble("currentSoc", 50.0),
                        passportConsumption = obj.optDouble("passportConsumption", 16.0),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val sessions = mutableListOf<ChargingSession>()
            val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.getJSONObject(i)
                sessions.add(
                    ChargingSession(
                        id = obj.optLong("id", 0L),
                        carId = obj.optLong("carId", 1L),
                        startOdometer = obj.optDouble("startOdometer", 0.0),
                        startSoc = obj.optDouble("startSoc", 0.0),
                        endSoc = obj.optDouble("endSoc", 0.0),
                        kwhDeliveredByStation = obj.optDouble("kwhDeliveredByStation", 0.0),
                        kwhReceivedByCar = if (obj.has("kwhReceivedByCar")) obj.optDouble("kwhReceivedByCar") else null,
                        pricePerKwh = obj.optDouble("pricePerKwh", 0.5),
                        energyCost = obj.optDouble("energyCost", 0.0),
                        penaltyCost = obj.optDouble("penaltyCost", 0.0),
                        fixedAmount = obj.optDouble("fixedAmount", 0.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        currency = obj.optString("currency", "BYN"),
                        stationType = obj.optString("stationType", "AC"),
                        operatorName = obj.optString("operatorName", ""),
                        isFreeCharge = obj.optBoolean("isFreeCharge", false),
                        nightTariffApplied = obj.optBoolean("nightTariffApplied", false),
                        startTime = obj.optLong("startTime", System.currentTimeMillis()),
                        endTime = if (obj.has("endTime")) obj.optLong("endTime") else null,
                        status = obj.optString("status", "completed")
                    )
                )
            }

            BackupData(cars, sessions, emptyList(), emptyList())
        } catch (_: Exception) {
            null
        }
    }
}
