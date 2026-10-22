package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.util.EVCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoltLedgerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, VoltLedgerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.widget.ACTION_REFRESH_WIDGET"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, VoltLedgerWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                if (appWidgetIds.isNotEmpty()) {
                    updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
                }
            } catch (_: Exception) {
                val intent = Intent(context, VoltLedgerWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_WIDGET
                }
                context.sendBroadcast(intent)
            }
        }

        private fun updateWidgetsAsync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            if (appWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val settingsManager = SettingsManager(context)
                    val settings = settingsManager.settingsFlow.firstOrNull()
                    val allCars = db.carDao().getAllCarsList()
                    val activeCar = allCars.find { it.id == settings?.selectedCarId }
                        ?: allCars.find { it.isActive }
                        ?: allCars.firstOrNull()

                    val allSessions = db.chargingSessionDao().getAllSessionsDirect()
                    val activeSession = allSessions.find { it.status == "active" }
                    val lastCompleted = allSessions
                        .filter { it.status == "completed" && (activeCar == null || it.carId == activeCar.id) }
                        .maxByOrNull { it.startTime }

                    for (widgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_voltledger)

                        // 1. App Launch Intent (tap on root card opens app)
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val mainPendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId * 10,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                        // 2. Start Charging Shortcut (tap on "+" button opens Start Charging)
                        val chargeIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("open_screen", "start_charging")
                        }
                        val chargePendingIntent = PendingIntent.getActivity(
                            context,
                            widgetId * 10 + 1,
                            chargeIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_btn_add, chargePendingIntent)

                        // 3. Refresh Widget Intent
                        val refreshIntent = Intent(context, VoltLedgerWidgetProvider::class.java).apply {
                            action = ACTION_REFRESH_WIDGET
                        }
                        val refreshPendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId * 10 + 2,
                            refreshIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

                        // 4. Fill in vehicle state
                        if (activeCar != null) {
                            views.setTextViewText(R.id.widget_car_name, activeCar.name)
                            val soc = activeCar.currentSoc.toInt().coerceIn(0, 100)
                            views.setTextViewText(R.id.widget_battery_soc, "$soc%")
                            views.setProgressBar(R.id.widget_battery_progress, 100, soc, false)

                            // Estimated range calculation
                            val capacity = activeCar.usableCapacityKwh.takeIf { it > 0.0 } ?: 57.0
                            val passport = activeCar.passportConsumption.takeIf { it > 0.0 } ?: 16.0
                            val carSessions = allSessions.filter { it.carId == activeCar.id && it.status == "completed" }
                            val cons = EVCalculator.calculateEffectiveMonthConsumption(carSessions, capacity, passport).consumption ?: passport
                            val estimatedRange = EVCalculator.calculateRangeForSoc(capacity, activeCar.currentSoc, cons)
                            views.setTextViewText(R.id.widget_range_text, "~${estimatedRange.toInt()} км")

                            // Status / Charging line
                            if (activeSession != null) {
                                views.setTextViewText(
                                    R.id.widget_status_text,
                                    "⚡ Идёт зарядка (${activeSession.stationType}) • Начало ${formatTime(activeSession.startTime)}"
                                )
                                views.setViewVisibility(R.id.widget_charging_indicator, View.VISIBLE)
                            } else if (lastCompleted != null) {
                                val kwh = String.format(Locale.US, "%.1f", lastCompleted.kwhDeliveredByStation)
                                views.setTextViewText(
                                    R.id.widget_status_text,
                                    "Посл. зарядка: +$kwh кВт·ч • ${formatDate(lastCompleted.startTime)}"
                                )
                                views.setViewVisibility(R.id.widget_charging_indicator, View.GONE)
                            } else {
                                views.setTextViewText(
                                    R.id.widget_status_text,
                                    "Расход: ${String.format(Locale.US, "%.1f", cons)} кВт·ч/100км"
                                )
                                views.setViewVisibility(R.id.widget_charging_indicator, View.GONE)
                            }
                        } else {
                            views.setTextViewText(R.id.widget_car_name, "VoltLedger")
                            views.setTextViewText(R.id.widget_battery_soc, "--%")
                            views.setProgressBar(R.id.widget_battery_progress, 100, 0, false)
                            views.setTextViewText(R.id.widget_range_text, "Добавьте авто")
                            views.setTextViewText(R.id.widget_status_text, "Нажмите для добавления авто")
                            views.setViewVisibility(R.id.widget_charging_indicator, View.GONE)
                        }

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                } catch (_: Exception) {
                    // Safe catch
                }
            }
        }

        private fun formatTime(millis: Long): String {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        }

        private fun formatDate(millis: Long): String {
            return SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(millis))
        }
    }
}
