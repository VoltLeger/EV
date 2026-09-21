package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailBackupHelper {

    fun sendBackupByEmail(
        context: Context,
        email: String,
        backupJson: String
    ) {
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val now = Date()
        val fileDateStr = fileDateFormat.format(now)
        val dateStr = displayDateFormat.format(now)
        val subject = "VoltLedger Резервная копия [$dateStr]"

        val body = buildString {
            append("Здравствуйте!\n\n")
            append("Во вложении находится файл полной резервной копии ваших данных, истории зарядок и электромобилей в приложении VoltLedger.\n\n")
            append("Привязанный E-mail: $email\n")
            append("Дата создания: $dateStr\n")
            append("Имя файла вложения: voltledger_backup_$fileDateStr.json\n\n")
            append("==========================================\n")
            append("ИНСТРУКЦИЯ ПО ВОССТАНОВЛЕНИЮ НА НОВОМ ТЕЛЕФОНЕ:\n")
            append("1. Скачайте прикреплённый .json файл из этого письма на телефон (или сохраните на Google Диск).\n")
            append("2. Откройте VoltLedger -> 'Настройки' -> 'Резервное копирование'.\n")
            append("3. Нажмите 'С Google Диска' (или 'Импорт JSON') и выберите скачанный файл.\n")
            append("Все ваши авто, сессии и расходы мгновенно восстановятся!\n")
            append("==========================================\n")
        }

        try {
            // Write backup JSON to cache directory for FileProvider sharing
            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val backupFile = File(backupDir, "voltledger_backup_$fileDateStr.json")
            backupFile.writeText(backupJson)

            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(email.trim()))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, "Отправить резервную копию на почту").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // Explicitly grant read permissions to all potential receiver apps
            val resInfoList = context.packageManager.queryIntentActivities(
                chooser,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val targetPackage = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    targetPackage,
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            context.startActivity(chooser)
        } catch (ex: Exception) {
            Toast.makeText(
                context,
                "Не удалось открыть почтовое приложение: ${ex.localizedMessage ?: "ошибка"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
