package com.example.util

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.ChargingSession
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URL
import java.util.UUID

/**
 * Local P2P Synchronization Engine (LocalSend & Bluetooth RFCOMM)
 * Connects Geely EX5 Car Tablet with the Owner's Smartphone without external internet servers.
 *
 * Supported Communication Modes:
 * 1. Bluetooth RFCOMM / SPP: Direct data exchange using vehicle-phone Bluetooth pairing.
 * 2. Local Wi-Fi / Hotspot Server (LocalSend compatible port: 53317):
 *    Direct HTTP sync when car tablet is connected to phone hotspot or in-car Wi-Fi.
 * 3. LocalSend Intent Integration:
 *    Direct one-tap export to LocalSend app via Android Share Sheet.
 */
object LocalP2PSyncManager {

    private const val TAG = "LocalP2PSyncManager"
    const val DEFAULT_HTTP_PORT = 53317
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard Serial Port Profile

    private var activeHttpServer: HttpServer? = null

    data class PairedDevice(
        val name: String,
        val address: String
    )

    data class SyncResult(
        val success: Boolean,
        val message: String,
        val batterySoc: Double? = null,
        val sessionsSynced: Int = 0,
        val details: String = ""
    )

    /**
     * Retrieves paired Bluetooth devices (e.g. Geely EX5, Flyme Auto, Owner's phone)
     */
    fun getPairedBluetoothDevices(context: Context): List<PairedDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()

            adapter.bondedDevices.map { device ->
                PairedDevice(
                    name = device.name ?: "Неизвестное устройство",
                    address = device.address
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Bluetooth permission not granted: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying paired devices: ${e.message}")
            emptyList()
        }
    }

    /**
     * Builds complete JSON sync payload from current Car Tablet state
     */
    suspend fun buildSyncPayload(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val settingsManager = SettingsManager(context)
        val settings = settingsManager.settingsFlow.firstOrNull()
        val allCars = db.carDao().getAllCarsList()
        val activeCar = allCars.find { it.id == settings?.selectedCarId }
            ?: allCars.find { it.isActive }
            ?: allCars.firstOrNull()

        val sessions = db.chargingSessionDao().getAllSessionsDirect()

        val root = JSONObject()
        root.put("protocol", "VoltLedgerP2P/1.0")
        root.put("timestamp", System.currentTimeMillis())
        root.put("deviceName", android.os.Build.MODEL ?: "Geely EX5")

        if (activeCar != null) {
            val carObj = JSONObject()
            carObj.put("name", activeCar.name)
            carObj.put("batterySoc", activeCar.currentSoc)
            carObj.put("odometer", activeCar.initialOdometer)
            carObj.put("usableCapacityKwh", activeCar.usableCapacityKwh)
            root.put("car", carObj)
        }

        val sessionsArray = JSONArray()
        for (s in sessions.take(20)) {
            val sObj = JSONObject()
            sObj.put("id", s.id)
            sObj.put("startTime", s.startTime)
            sObj.put("startSoc", s.startSoc)
            sObj.put("endSoc", s.endSoc)
            sObj.put("kwhReceived", s.kwhReceivedByCar ?: 0.0)
            sObj.put("stationType", s.stationType)
            sObj.put("operatorName", s.operatorName)
            sessionsArray.put(sObj)
        }
        root.put("recentSessions", sessionsArray)

        root.toString(2)
    }

    /**
     * Imports received sync payload into local database (Smartphone side)
     */
    suspend fun applySyncPayload(context: Context, jsonString: String): SyncResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val db = AppDatabase.getDatabase(context)
            val allCars = db.carDao().getAllCarsList()
            val activeCar = allCars.find { it.isActive } ?: allCars.firstOrNull()

            var updatedSoc: Double? = null
            if (root.has("car") && activeCar != null) {
                val carObj = root.getJSONObject("car")
                val soc = carObj.optDouble("batterySoc", -1.0)
                val odo = carObj.optDouble("odometer", -1.0)
                if (soc in 1.0..100.0) {
                    updatedSoc = soc
                    val updatedCar = activeCar.copy(
                        currentSoc = soc,
                        initialOdometer = if (odo > activeCar.initialOdometer) odo else activeCar.initialOdometer
                    )
                    db.carDao().updateCar(updatedCar)
                    com.example.widget.VoltLedgerWidgetProvider.updateAllWidgets(context)
                }
            }

            var importedCount = 0
            if (root.has("recentSessions") && activeCar != null) {
                val arr = root.getJSONArray("recentSessions")
                val existing = db.chargingSessionDao().getAllSessionsDirect()
                for (i in 0 until arr.length()) {
                    val sObj = arr.getJSONObject(i)
                    val sTime = sObj.optLong("startTime", 0L)
                    val exists = existing.any { Math.abs(it.startTime - sTime) < 5000 }
                    if (!exists && sTime > 0) {
                        val newSession = ChargingSession(
                            carId = activeCar.id,
                            startOdometer = activeCar.initialOdometer,
                            startSoc = sObj.optDouble("startSoc", 0.0),
                            endSoc = sObj.optDouble("endSoc", 0.0),
                            kwhReceivedByCar = sObj.optDouble("kwhReceived", 0.0),
                            kwhDeliveredByStation = sObj.optDouble("kwhReceived", 0.0) * 1.05,
                            pricePerKwh = 0.40,
                            currency = "BYN",
                            stationType = sObj.optString("stationType", "AC"),
                            operatorName = sObj.optString("operatorName", "Geely EX5 P2P"),
                            startTime = sTime,
                            endTime = sTime + 3600_000,
                            status = "completed"
                        )
                        db.chargingSessionDao().insertSession(newSession)
                        importedCount++
                    }
                }
            }

            SyncResult(
                success = true,
                message = "Успешно синхронизировано (Заряд: ${updatedSoc?.toInt() ?: "--"}%, сессий: $importedCount)",
                batterySoc = updatedSoc,
                sessionsSynced = importedCount
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                message = "Ошибка разбора данных: ${e.message}"
            )
        }
    }

    /**
     * Starts local in-car HTTP Server (Port 53317 - LocalSend compatible)
     * Allows phone connected to car Wi-Fi / Hotspot to pull live telemetry in 1 tap!
     */
    fun startLocalHotspotServer(context: Context, port: Int = DEFAULT_HTTP_PORT): Boolean {
        return try {
            stopLocalHotspotServer()
            val server = HttpServer.create(InetSocketAddress(port), 0)
            server.createContext("/api/volt/status", HttpHandler { exchange: HttpExchange ->
                try {
                    val jsonResponse = runBlockingOnIo { buildSyncPayload(context) }
                    val bytes = jsonResponse.toByteArray(Charsets.UTF_8)
                    exchange.responseHeaders.set("Content-Type", "application/json; charset=UTF-8")
                    exchange.sendResponseHeaders(200, bytes.size.toLong())
                    exchange.responseBody.use { it.write(bytes) }
                } catch (e: Exception) {
                    exchange.sendResponseHeaders(500, 0)
                }
            })
            server.executor = null
            server.start()
            activeHttpServer = server
            Log.i(TAG, "Local Hotspot P2P Server started on port $port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local hotspot server: ${e.message}")
            false
        }
    }

    fun stopLocalHotspotServer() {
        activeHttpServer?.stop(0)
        activeHttpServer = null
    }

    fun isLocalServerRunning(): Boolean = activeHttpServer != null

    /**
     * Smartphone Client: Connects to Car Head Unit via Wi-Fi Hotspot (default gateway 192.168.43.1:53317)
     */
    suspend fun syncFromCarHotspot(context: Context, carIp: String = "192.168.43.1", port: Int = DEFAULT_HTTP_PORT): SyncResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://$carIp:$port/api/volt/status")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "GET"

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val content = reader.readText()
                reader.close()
                return@withContext applySyncPayload(context, content)
            } else {
                return@withContext SyncResult(
                    success = false,
                    message = "Автомобиль ответил с ошибкой HTTP ${conn.responseCode}"
                )
            }
        } catch (e: Exception) {
            return@withContext SyncResult(
                success = false,
                message = "Не удалось подключиться к ГУ авто по адресу $carIp:$port (${e.message})"
            )
        }
    }

    /**
     * Direct Bluetooth Sync Client: Connects to paired Car Tablet via Bluetooth RFCOMM
     */
    suspend fun syncViaBluetooth(context: Context, deviceAddress: String): SyncResult = withContext(Dispatchers.IO) {
        val adapter = BluetoothAdapter.getDefaultAdapter()
            ?: return@withContext SyncResult(success = false, message = "Bluetooth не поддерживается на устройстве")

        var socket: BluetoothSocket? = null
        try {
            val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            adapter.cancelDiscovery()
            socket.connect()

            // 1. Send our request/status
            val out = OutputStreamWriter(socket.outputStream, Charsets.UTF_8)
            val myPayload = buildSyncPayload(context)
            out.write("SYNC_REQUEST\n$myPayload\nEOF_VOLT\n")
            out.flush()

            // 2. Read response
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            val sb = java.lang.StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line == "EOF_VOLT") break
                sb.append(line).append("\n")
            }

            val received = sb.toString().trim()
            if (received.isNotEmpty()) {
                val result = applySyncPayload(context, received)
                return@withContext result
            }

            SyncResult(success = true, message = "Синхронизация по Bluetooth завершена")
        } catch (e: Exception) {
            SyncResult(success = false, message = "Bluetooth ошибка связи с $deviceAddress: ${e.message}")
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    /**
     * LocalSend One-Tap Share: Exports JSON file and triggers Android Share Sheet targeting LocalSend
     */
    suspend fun shareViaLocalSend(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = buildSyncPayload(context)
            val cacheDir = File(context.cacheDir, "localsend_share")
            cacheDir.mkdirs()
            val file = File(cacheDir, "voltledger_ex5_sync.json")
            file.writeText(payload, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "VoltLedger Geely EX5 Telemetry")
                putExtra(Intent.EXTRA_TEXT, "Синхронизация данных электромобиля Geely EX5")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(Intent.createChooser(sendIntent, "Отправить через LocalSend / Bluetooth").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share via LocalSend: ${e.message}")
            false
        }
    }

    private fun <T> runBlockingOnIo(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) { block() }
    }
}
