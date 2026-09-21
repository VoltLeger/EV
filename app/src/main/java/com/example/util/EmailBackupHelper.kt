package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailBackupHelper {

    fun sendBackupByEmail(
        context: Context,
        email: String,
        backupJson: String,
        gistId: String? = null
    ) {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val subject = "VoltLedger Резервная копия [$dateStr]"

        val body = buildString {
            append("Здравствуйте!\n\n")
            append("Это полная резервная копия ваших данных, настроек и электромобилей в приложении VoltLedger.\n\n")
            append("Привязанный E-mail: $email\n")
            append("Дата создания: $dateStr\n")
            if (!gistId.isNullOrBlank()) {
                append("GitHub Gist ID: $gistId\n")
                append("Ссылка на облачную копию: https://gist.github.com/$gistId\n")
            }
            append("\n==========================================\n")
            append("ИНСТРУКЦИЯ ПО ВОССТАНОВЛЕНИЮ НА НОВОМ ТЕЛЕФОНЕ:\n")
            append("1. Установите VoltLedger на новый телефон.\n")
            if (!gistId.isNullOrBlank()) {
                append("2. В разделе 'Настройки' -> 'Восстановление из GitHub' введите ваш Gist ID: $gistId\n")
                append("ЛИБО:\n")
            }
            append("Скопируйте весь JSON-код, приведённый ниже, перейдите в Настройки -> 'Импорт данных' и вставьте его.\n")
            append("==========================================\n\n")
            append("--- КОД РЕЗЕРВНОЙ КОПИИ ---\n")
            append(backupJson)
        }

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${email.trim()}")
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            val chooser = Intent.createChooser(intent, "Отправить резервную копию на почту")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Fallback to standard ACTION_SEND
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(email.trim()))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                val chooser = Intent.createChooser(sendIntent, "Отправить резервную копию")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "Не удалось открыть почтовое приложение", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
