package com.example.util

import com.example.data.model.Car
import com.example.data.model.CarExpense
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

data class BackupData(
    val email: String? = null,
    val profile: UserProfile? = null,
    val cars: List<Car> = emptyList(),
    val sessions: List<ChargingSession> = emptyList(),
    val expenses: List<CarExpense> = emptyList(),
    val operators: List<Operator> = emptyList(),
    val tags: List<Tag> = emptyList()
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

    /**
     * Splits a CSV line respecting quoted strings.
     */
    fun splitCsvLine(line: String, delimiter: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private fun parseTimestamp(value: Any?): Long {
        if (value is Number) {
            val l = value.toLong()
            return if (l in 1_000_000_000L..9_999_999_999L) l * 1000L else l
        }
        val str = value?.toString()?.trim() ?: return System.currentTimeMillis()
        val num = str.toLongOrNull()
        if (num != null) {
            return if (num in 1_000_000_000L..9_999_999_999L) num * 1000L else num
        }
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                val d = sdf.parse(str)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    private fun parseDouble(value: Any?, default: Double = 0.0): Double {
        if (value is Number) return value.toDouble()
        val str = value?.toString()?.trim()?.replace(" ", "")?.replace(",", ".") ?: return default
        return str.toDoubleOrNull() ?: default
    }

    private fun parseInt(value: Any?, default: Int = 0): Int {
        if (value is Number) return value.toInt()
        val str = value?.toString()?.trim() ?: return default
        return str.toIntOrNull() ?: default
    }

    private fun parseLong(value: Any?, default: Long = 0L): Long {
        if (value is Number) return value.toLong()
        val str = value?.toString()?.trim() ?: return default
        return str.toLongOrNull() ?: default
    }

    private fun parseBoolean(value: Any?, default: Boolean = false): Boolean {
        if (value is Boolean) return value
        val s = value?.toString()?.trim()?.lowercase(Locale.ROOT) ?: return default
        return when (s) {
            "true", "1", "yes", "да" -> true
            "false", "0", "no", "нет" -> false
            else -> default
        }
    }

    private fun JSONObject.optDoubleAny(vararg keys: String, default: Double = 0.0): Double {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                return parseDouble(opt(k), default)
            }
        }
        return default
    }

    private fun JSONObject.optDoubleOrNull(vararg keys: String): Double? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val s = opt(k)?.toString()?.trim()?.replace(" ", "")?.replace(",", ".")
                val d = s?.toDoubleOrNull()
                if (d != null) return d
            }
        }
        return null
    }

    private fun JSONObject.optLongAny(vararg keys: String, default: Long = 0L): Long {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                return parseLong(opt(k), default)
            }
        }
        return default
    }

    private fun JSONObject.optLongOrNull(vararg keys: String): Long? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val l = opt(k)?.toString()?.trim()?.toLongOrNull()
                if (l != null) return l
            }
        }
        return null
    }

    private fun JSONObject.optIntAny(vararg keys: String, default: Int = 0): Int {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                return parseInt(opt(k), default)
            }
        }
        return default
    }

    private fun JSONObject.optIntOrNull(vararg keys: String): Int? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val i = opt(k)?.toString()?.trim()?.toIntOrNull()
                if (i != null) return i
            }
        }
        return null
    }

    private fun JSONObject.optStringAny(vararg keys: String, default: String = ""): String {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val s = optString(k, "").trim()
                if (s.isNotEmpty()) return s
            }
        }
        return default
    }

    private fun JSONObject.optStringOrNull(vararg keys: String): String? {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                val s = optString(k, "").trim()
                if (s.isNotEmpty() && s != "null") return s
            }
        }
        return null
    }

    private fun JSONObject.optBooleanAny(vararg keys: String, default: Boolean = false): Boolean {
        for (k in keys) {
            if (has(k) && !isNull(k)) {
                return parseBoolean(opt(k), default)
            }
        }
        return default
    }

    fun parseSessionsFromCsv(csvText: String, carId: Long): List<ChargingSession> {
        val list = mutableListOf<ChargingSession>()
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return list

        val firstLine = lines.first()
        val commaCount = firstLine.count { it == ',' }
        val semiCount = firstLine.count { it == ';' }
        val tabCount = firstLine.count { it == '\t' }
        val delimiter = when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }

        val headerTokens = splitCsvLine(firstLine, delimiter).map { it.trim().removeSurrounding("\"").lowercase(Locale.ROOT) }
        val hasHeader = headerTokens.any { token ->
            token.contains("id") || token.contains("time") || token.contains("дата") ||
                    token.contains("odo") || token.contains("soc") || token.contains("kwh") ||
                    token.contains("квт") || token.contains("cost") || token.contains("цена") ||
                    token.contains("operator") || token.contains("станция")
        }

        fun col(vararg patterns: String): Int {
            for (p in patterns) {
                val idx = headerTokens.indexOfFirst { it == p || it.contains(p) }
                if (idx != -1) return idx
            }
            return -1
        }

        val idCol = if (hasHeader) col("id", "номер") else 0
        val carIdCol = if (hasHeader) col("car_id", "carid", "car") else 1
        val startTimeCol = if (hasHeader) col("start_time", "starttime", "date", "дата", "время", "time", "timestamp") else 2
        val endTimeCol = if (hasHeader) col("end_time", "endtime", "end_date", "конец") else 3
        val startOdoCol = if (hasHeader) col("start_odometer", "startodometer", "odometer", "пробег", "одометр") else 4
        val startSocCol = if (hasHeader) col("start_soc", "startsoc", "soc_start", "soc") else 5
        val endSocCol = if (hasHeader) col("end_soc", "endsoc", "soc_end") else 6
        val stationTypeCol = if (hasHeader) col("station_type", "stationtype", "type", "тип") else 7
        val operatorCol = if (hasHeader) col("operator_name", "operatorname", "operator", "оператор", "станция", "station") else 8
        val kwhCol = if (hasHeader) col("kwh_delivered", "kwhdelivered", "kwh", "квт", "квтч", "квт⋅ч", "energy") else 9
        val kwhRecCol = if (hasHeader) col("kwh_received", "kwhreceived") else 10
        val priceCol = if (hasHeader) col("price_per_kwh", "priceperkwh", "price", "цена", "тариф") else 11
        val energyCostCol = if (hasHeader) col("energy_cost", "energycost") else 12
        val penaltyCostCol = if (hasHeader) col("penalty_cost", "penaltycost") else 13
        val fixedAmountCol = if (hasHeader) col("fixed_amount", "fixedamount") else 14
        val totalCostCol = if (hasHeader) col("total_cost", "totalcost", "cost", "сумма", "стоимость", "total") else 15
        val currencyCol = if (hasHeader) col("currency", "валюта") else 16
        val statusCol = if (hasHeader) col("status", "статус") else 17

        val startIndex = if (hasHeader) 1 else 0
        for (i in startIndex until lines.size) {
            val line = lines[i]
            val tokens = splitCsvLine(line, delimiter).map { it.trim().removeSurrounding("\"") }
            if (tokens.isEmpty()) continue

            fun tokenAt(idx: Int): String? = if (idx in tokens.indices) tokens[idx].trim().ifEmpty { null } else null

            try {
                val startOdo = parseDouble(tokenAt(startOdoCol), 0.0)
                val kwh = parseDouble(tokenAt(kwhCol), 0.0)
                val totalCost = parseDouble(tokenAt(totalCostCol), 0.0)
                val startTime = parseTimestamp(tokenAt(startTimeCol))
                val endTime = tokenAt(endTimeCol)?.let { parseTimestamp(it) }
                val startSoc = parseDouble(tokenAt(startSocCol), 0.0)
                val endSoc = parseDouble(tokenAt(endSocCol), 0.0)
                val price = parseDouble(tokenAt(priceCol), if (kwh > 0.0 && totalCost > 0.0) totalCost / kwh else 0.5)
                val opName = tokenAt(operatorCol) ?: ""
                val stType = tokenAt(stationTypeCol)?.ifEmpty { "AC" } ?: "AC"
                val curr = tokenAt(currencyCol)?.ifEmpty { "BYN" } ?: "BYN"
                val stat = tokenAt(statusCol)?.ifEmpty { "completed" } ?: "completed"
                val energyCost = parseDouble(tokenAt(energyCostCol), totalCost)
                val penaltyCost = parseDouble(tokenAt(penaltyCostCol), 0.0)
                val fixedAmount = parseDouble(tokenAt(fixedAmountCol), 0.0)
                val kwhRec = tokenAt(kwhRecCol)?.let { parseDouble(it, 0.0) }
                val rowCarId = tokenAt(carIdCol)?.toLongOrNull() ?: carId

                val session = ChargingSession(
                    id = 0L,
                    carId = rowCarId,
                    startTime = startTime,
                    endTime = endTime,
                    startOdometer = startOdo,
                    startSoc = startSoc,
                    endSoc = endSoc,
                    stationType = stType,
                    operatorName = opName,
                    kwhDeliveredByStation = kwh,
                    kwhReceivedByCar = kwhRec,
                    pricePerKwh = price,
                    energyCost = energyCost,
                    penaltyCost = penaltyCost,
                    fixedAmount = fixedAmount,
                    totalCost = totalCost,
                    currency = curr,
                    status = stat
                )
                list.add(session)
            } catch (_: Exception) {
                // Ignore malformed line
            }
        }
        return list
    }

    fun exportFullBackupJson(
        cars: List<Car>,
        sessions: List<ChargingSession>,
        expenses: List<CarExpense> = emptyList(),
        operators: List<Operator> = emptyList(),
        tags: List<Tag> = emptyList(),
        profile: UserProfile? = null
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("app", "VoltLedger")
        root.put("timestamp", System.currentTimeMillis())

        if (profile != null) {
            val profObj = JSONObject()
            profObj.put("id", profile.id)
            profObj.put("callsign", profile.callsign)
            profObj.put("displayName", profile.displayName)
            profObj.put("bio", profile.bio)
            profObj.put("avatarEffect", profile.avatarEffect)
            profObj.put("avatarIcon", profile.avatarIcon)
            profObj.put("countryCode", profile.countryCode)
            profObj.put("isLeaderboardOptIn", profile.isLeaderboardOptIn)
            profObj.put("totalXp", profile.totalXp)
            profObj.put("rankTier", profile.rankTier)
            if (profile.email != null) profObj.put("email", profile.email)
            if (profile.backupGistId != null) profObj.put("backupGistId", profile.backupGistId)
            root.put("profile", profObj)
            if (profile.email != null) {
                root.put("userEmail", profile.email)
            }
        }

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
            obj.put("isActive", c.isActive)
            if (c.licensePlate != null) obj.put("licensePlate", c.licensePlate)
            if (c.purchaseDate != null) obj.put("purchaseDate", c.purchaseDate)
            if (c.purchasePrice != null) obj.put("purchasePrice", c.purchasePrice)
            if (c.manufactureYear != null) obj.put("manufactureYear", c.manufactureYear)
            if (c.vin != null) obj.put("vin", c.vin)
            if (c.registrationNumber != null) obj.put("registrationNumber", c.registrationNumber)
            if (c.insuranceNumber != null) obj.put("insuranceNumber", c.insuranceNumber)
            if (c.notes != null) obj.put("notes", c.notes)
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

        val expArray = JSONArray()
        for (e in expenses) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("carId", e.carId)
            obj.put("category", e.category)
            obj.put("amount", e.amount)
            obj.put("currency", e.currency)
            obj.put("timestamp", e.timestamp)
            if (e.odometer != null) obj.put("odometer", e.odometer)
            if (e.comment != null) obj.put("comment", e.comment)
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        val opArray = JSONArray()
        for (o in operators) {
            val obj = JSONObject()
            obj.put("id", o.id)
            obj.put("name", o.name)
            obj.put("type", o.type)
            obj.put("subType", o.subType)
            obj.put("priceAc", o.priceAc)
            obj.put("priceDc", o.priceDc)
            if (o.nightPriceAc != null) obj.put("nightPriceAc", o.nightPriceAc)
            if (o.nightPriceDc != null) obj.put("nightPriceDc", o.nightPriceDc)
            obj.put("nightStartHour", o.nightStartHour)
            obj.put("nightEndHour", o.nightEndHour)
            obj.put("penaltyIdlePerMin", o.penaltyIdlePerMin)
            obj.put("penaltyFreeMinutes", o.penaltyFreeMinutes)
            obj.put("comment", o.comment)
            obj.put("isBuiltin", o.isBuiltin)
            obj.put("isFree", o.isFree)
            obj.put("updatedAt", o.updatedAt)
            opArray.put(obj)
        }
        root.put("operators", opArray)

        val tagsArray = JSONArray()
        for (t in tags) {
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            obj.put("color", t.color)
            obj.put("isBuiltin", t.isBuiltin)
            tagsArray.put(obj)
        }
        root.put("tags", tagsArray)

        return root.toString(2)
    }

    private fun parseSessionObject(obj: JSONObject, defaultCarId: Long = 1L): ChargingSession {
        return ChargingSession(
            id = obj.optLongAny("id", default = 0L),
            carId = obj.optLongAny("carId", "car_id", default = defaultCarId),
            startOdometer = obj.optDoubleAny("startOdometer", "start_odometer", "odometer", "odo", default = 0.0),
            startSoc = obj.optDoubleAny("startSoc", "start_soc", "soc_start", "soc", default = 0.0),
            endSoc = obj.optDoubleAny("endSoc", "end_soc", "soc_end", default = 0.0),
            kwhDeliveredByStation = obj.optDoubleAny("kwhDeliveredByStation", "kwh_delivered", "kwhDelivered", "kwh", "energy", default = 0.0),
            kwhReceivedByCar = obj.optDoubleOrNull("kwhReceivedByCar", "kwh_received", "kwhReceived"),
            pricePerKwh = obj.optDoubleAny("pricePerKwh", "price_per_kwh", "price", "tariff", default = 0.5),
            energyCost = obj.optDoubleAny("energyCost", "energy_cost", default = 0.0),
            penaltyCost = obj.optDoubleAny("penaltyCost", "penalty_cost", default = 0.0),
            fixedAmount = obj.optDoubleAny("fixedAmount", "fixed_amount", default = 0.0),
            totalCost = obj.optDoubleAny("totalCost", "total_cost", "cost", "amount", default = 0.0),
            currency = obj.optStringAny("currency", "valuta", default = "BYN"),
            stationType = obj.optStringAny("stationType", "station_type", "type", default = "AC"),
            operatorName = obj.optStringAny("operatorName", "operator_name", "operator", "station", default = ""),
            isFreeCharge = obj.optBooleanAny("isFreeCharge", "is_free_charge", "free", default = false),
            nightTariffApplied = obj.optBooleanAny("nightTariffApplied", "night_tariff_applied", "is_night", default = false),
            startTime = parseTimestamp(obj.opt("startTime") ?: obj.opt("start_time") ?: obj.opt("timestamp") ?: obj.opt("date")),
            endTime = (obj.opt("endTime") ?: obj.opt("end_time"))?.let { parseTimestamp(it) },
            status = obj.optStringAny("status", default = "completed")
        )
    }

    private fun parseCarObject(obj: JSONObject): Car {
        return Car(
            id = obj.optLongAny("id", default = 0L),
            name = obj.optStringAny("name", "car_name", "title", default = "EV"),
            declaredCapacityKwh = obj.optDoubleAny("declaredCapacityKwh", "declared_capacity", "capacity", default = 60.0),
            usableCapacityKwh = obj.optDoubleAny("usableCapacityKwh", "usable_capacity", default = 57.0),
            initialOdometer = obj.optDoubleAny("initialOdometer", "initial_odometer", "odometer", default = 0.0),
            currentSoc = obj.optDoubleAny("currentSoc", "current_soc", "soc", default = 50.0),
            passportConsumption = obj.optDoubleAny("passportConsumption", "passport_consumption", default = 16.0),
            createdAt = obj.optLongAny("createdAt", "created_at", default = System.currentTimeMillis()),
            isActive = obj.optBooleanAny("isActive", "is_active", default = true),
            licensePlate = obj.optStringOrNull("licensePlate", "license_plate", "plate"),
            purchaseDate = obj.optStringOrNull("purchaseDate", "purchase_date"),
            purchasePrice = obj.optDoubleOrNull("purchasePrice", "purchase_price"),
            manufactureYear = obj.optIntOrNull("manufactureYear", "manufacture_year", "year"),
            vin = obj.optStringOrNull("vin"),
            registrationNumber = obj.optStringOrNull("registrationNumber", "registration_number"),
            insuranceNumber = obj.optStringOrNull("insuranceNumber", "insurance_number"),
            notes = obj.optStringOrNull("notes", "comment")
        )
    }

    /**
     * Parses a full backup JSON string or a JSON array of sessions.
     */
    fun parseFullBackupJson(jsonString: String): BackupData? {
        val clean = jsonString.trim().removePrefix("\uFEFF").trim()
        if (clean.isEmpty()) return null

        return try {
            // Case 1: Direct JSON Array of sessions
            if (clean.startsWith("[")) {
                val array = JSONArray(clean)
                val sessions = mutableListOf<ChargingSession>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    sessions.add(parseSessionObject(obj))
                }
                return BackupData(sessions = sessions)
            }

            // Case 2: Standard JSON Root Object
            val root = JSONObject(clean)
            val userEmail = root.optStringOrNull("userEmail", "email")

            var profile: UserProfile? = null
            if (root.has("profile")) {
                val p = root.getJSONObject("profile")
                profile = UserProfile(
                    id = p.optStringAny("id", default = "pilot_user"),
                    callsign = p.optStringAny("callsign", default = "VOLT-1000"),
                    displayName = p.optStringAny("displayName", "display_name", default = "EV Пилот"),
                    bio = p.optStringAny("bio", default = "Электромобилист"),
                    avatarEffect = p.optStringAny("avatarEffect", "avatar_effect", default = "neon_cyan"),
                    avatarIcon = p.optStringAny("avatarIcon", "avatar_icon", default = "bolt"),
                    countryCode = p.optStringAny("countryCode", "country_code", default = "BY"),
                    isLeaderboardOptIn = p.optBooleanAny("isLeaderboardOptIn", "is_leaderboard_opt_in", default = true),
                    totalXp = p.optLongAny("totalXp", "total_xp", default = 0L),
                    rankTier = p.optStringAny("rankTier", "rank_tier", default = "Новичок"),
                    email = p.optStringOrNull("email") ?: userEmail,
                    backupGistId = p.optStringOrNull("backupGistId", "backup_gist_id")
                )
            }

            val cars = mutableListOf<Car>()
            val carsArray = root.optJSONArray("cars") ?: root.optJSONArray("vehicles") ?: root.optJSONArray("autos")
            if (carsArray != null) {
                for (i in 0 until carsArray.length()) {
                    val obj = carsArray.optJSONObject(i) ?: continue
                    cars.add(parseCarObject(obj))
                }
            }

            val sessions = mutableListOf<ChargingSession>()
            val sessionsArray = root.optJSONArray("sessions")
                ?: root.optJSONArray("history")
                ?: root.optJSONArray("charging_sessions")
                ?: root.optJSONArray("chargingSessions")
                ?: root.optJSONArray("records")
                ?: root.optJSONArray("items")
                ?: root.optJSONArray("data")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    val obj = sessionsArray.optJSONObject(i) ?: continue
                    sessions.add(parseSessionObject(obj))
                }
            }

            val expenses = mutableListOf<CarExpense>()
            val expArray = root.optJSONArray("expenses") ?: root.optJSONArray("car_expenses") ?: root.optJSONArray("costs")
            if (expArray != null) {
                for (i in 0 until expArray.length()) {
                    val obj = expArray.optJSONObject(i) ?: continue
                    expenses.add(
                        CarExpense(
                            id = obj.optLongAny("id", default = 0L),
                            carId = obj.optLongAny("carId", "car_id", default = 1L),
                            category = obj.optStringAny("category", default = "Другое"),
                            amount = obj.optDoubleAny("amount", "cost", default = 0.0),
                            currency = obj.optStringAny("currency", default = "BYN"),
                            timestamp = parseTimestamp(obj.opt("timestamp") ?: obj.opt("date")),
                            odometer = obj.optDoubleOrNull("odometer", "odo"),
                            comment = obj.optStringOrNull("comment", "note")
                        )
                    )
                }
            }

            val operators = mutableListOf<Operator>()
            val opArray = root.optJSONArray("operators") ?: root.optJSONArray("stations") ?: root.optJSONArray("tariffs")
            if (opArray != null) {
                for (i in 0 until opArray.length()) {
                    val obj = opArray.optJSONObject(i) ?: continue
                    operators.add(
                        Operator(
                            id = obj.optLongAny("id", default = 0L),
                            name = obj.optStringAny("name", default = "Оператор"),
                            type = obj.optStringAny("type", default = "AC"),
                            subType = obj.optStringAny("subType", "sub_type", default = ""),
                            priceAc = obj.optDoubleAny("priceAc", "price_ac", default = 0.55),
                            priceDc = obj.optDoubleAny("priceDc", "price_dc", default = 0.73),
                            nightPriceAc = obj.optDoubleOrNull("nightPriceAc", "night_price_ac"),
                            nightPriceDc = obj.optDoubleOrNull("nightPriceDc", "night_price_dc"),
                            nightStartHour = obj.optIntAny("nightStartHour", "night_start_hour", default = 23),
                            nightEndHour = obj.optIntAny("nightEndHour", "night_end_hour", default = 6),
                            penaltyIdlePerMin = obj.optDoubleAny("penaltyIdlePerMin", "penalty_idle_per_min", default = 0.0),
                            penaltyFreeMinutes = obj.optIntAny("penaltyFreeMinutes", "penalty_free_minutes", default = 0),
                            comment = obj.optStringAny("comment", default = ""),
                            isBuiltin = obj.optBooleanAny("isBuiltin", "is_builtin", default = false),
                            isFree = obj.optBooleanAny("isFree", "is_free", default = false),
                            updatedAt = obj.optLongAny("updatedAt", "updated_at", default = System.currentTimeMillis())
                        )
                    )
                }
            }

            val tags = mutableListOf<Tag>()
            val tagsArray = root.optJSONArray("tags")
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    val obj = tagsArray.optJSONObject(i) ?: continue
                    tags.add(
                        Tag(
                            id = obj.optLongAny("id", default = 0L),
                            name = obj.optStringAny("name", default = "Тег"),
                            color = obj.optLongAny("color", default = 0xFF2196F3),
                            isBuiltin = obj.optBooleanAny("isBuiltin", "is_builtin", default = false)
                        )
                    )
                }
            }

            BackupData(
                email = userEmail ?: profile?.email,
                profile = profile,
                cars = cars,
                sessions = sessions,
                expenses = expenses,
                operators = operators,
                tags = tags
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Universal backup parser that detects JSON object, JSON array, or CSV text.
     */
    fun parseAnyBackup(rawText: String, defaultCarId: Long = 1L): BackupData? {
        val clean = rawText.trim().removePrefix("\uFEFF").trim()
        if (clean.isEmpty()) return null

        // Try JSON first if it looks like JSON
        if (clean.startsWith("{") || clean.startsWith("[")) {
            val jsonResult = parseFullBackupJson(clean)
            if (jsonResult != null && (jsonResult.sessions.isNotEmpty() || jsonResult.cars.isNotEmpty() || jsonResult.expenses.isNotEmpty())) {
                return jsonResult
            }
        }

        // Try CSV parsing
        val csvSessions = parseSessionsFromCsv(clean, defaultCarId)
        if (csvSessions.isNotEmpty()) {
            return BackupData(sessions = csvSessions)
        }

        // Final fallback: try JSON if it wasn't started with standard braces
        return parseFullBackupJson(clean)
    }
}
