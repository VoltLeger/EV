package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "voltledger_notifications"
    private const val CHANNEL_NAME = "VoltLedger Notifications"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "VoltLedger charging and consumption notifications"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(id, builder.build())
    }

    fun showUnfinishedChargeNotification(context: Context, isEn: Boolean) {
        val title = if (isEn) "VoltLedger: Active Charge" else "VoltLedger: Активная зарядка"
        val message = if (isEn) {
            "You haven't completed your charging session — tap to enter final data."
        } else {
            "Вы не завершили зарядку — введите конечные данные."
        }
        showNotification(context, 1001, title, message)
    }

    fun showWeeklyReportNotification(context: Context, isEn: Boolean, spentDiffPercent: Int, totalCost: Double, currency: String) {
        val title = if (isEn) "Weekly Charging Summary" else "Еженедельный отчёт"
        val sign = if (spentDiffPercent >= 0) "+" else ""
        val message = if (isEn) {
            "This week you spent $totalCost $currency ($sign$spentDiffPercent% vs last week)."
        } else {
            "За эту неделю вы потратили $totalCost $currency ($sign$spentDiffPercent% к прошлой неделе)."
        }
        showNotification(context, 1002, title, message)
    }
}
