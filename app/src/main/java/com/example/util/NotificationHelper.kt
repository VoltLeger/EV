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
        try {
            manager.notify(id, builder.build())
        } catch (e: Exception) {
            // Silently ignore if notifications permission is not granted or blocked
        }
    }

    fun showUnfinishedChargeNotification(context: Context, isEn: Boolean, hoursElapsed: Int = 4) {
        val title = if (isEn) "VoltLedger: Active Charge" else "VoltLedger: Активная зарядка"
        val message = if (isEn) {
            "Your charging session has been running for $hoursElapsed+ hours. Tap to complete and log final metrics."
        } else {
            "Зарядка продолжается уже более $hoursElapsed ч. Нажмите, чтобы завершить сессию и внести данные."
        }
        showNotification(context, 1001, title, message)
    }

    fun showDcSessionExceededNotification(context: Context, isEn: Boolean) {
        val title = if (isEn) "⚠️ DC Fast Charging: 2+ Hours" else "⚠️ Быстрая зарядка DC: более 2 часов"
        val message = if (isEn) {
            "DC fast charging session has exceeded 2 hours! Check the station to prevent idle penalties or battery overheating."
        } else {
            "Сессия быстрой зарядки DC длится более 2 часов! Проверьте станцию, чтобы избежать штрафов за простой."
        }
        showNotification(context, 1003, title, message)
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

    fun showAchievementUnlockedNotification(
        context: Context,
        title: String,
        description: String,
        xpReward: Int,
        isEn: Boolean = false
    ) {
        val notifTitle = if (isEn) "🏆 Achievement Unlocked: $title" else "🏆 Награда разблокирована: $title"
        val notifMsg = if (isEn) {
            "$description (+$xpReward XP)"
        } else {
            "$description (+ $xpReward XP)"
        }
        showNotification(context, 1004, notifTitle, notifMsg)
    }

    fun showAchievementProgressNotification(
        context: Context,
        title: String,
        current: Int,
        total: Int,
        unit: String = "зарядок",
        isEn: Boolean = false
    ) {
        val notifTitle = if (isEn) "🎯 Achievement Progress: $title" else "🎯 Прогресс достижения: $title"
        val remaining = (total - current).coerceAtLeast(0)
        val notifMsg = if (isEn) {
            "Done $current of $total $unit! $remaining left to unlock."
        } else {
            val remainingText = if (remaining > 0) ", осталось ещё $remaining" else ""
            "Сделана $current из $total $unit ($title)$remainingText!"
        }
        showNotification(context, 1005, notifTitle, notifMsg)
    }
}
