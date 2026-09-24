package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PostcardTheme(
    val id: Int,
    val name: String,
    val bgStartColor: Int,
    val bgEndColor: Int,
    val primaryAccent: Int,
    val secondaryAccent: Int,
    val cardBgColor: Int,
    val cardBorderColor: Int
)

object PostcardGenerator {

    val THEMES = listOf(
        PostcardTheme(
            id = 0,
            name = "⚡ Кибер Неон",
            bgStartColor = Color.parseColor("#0B0F19"),
            bgEndColor = Color.parseColor("#131B2E"),
            primaryAccent = Color.parseColor("#00E5FF"), // Neon Cyan
            secondaryAccent = Color.parseColor("#A855F7"), // Electric Purple
            cardBgColor = Color.parseColor("#1E293B"),
            cardBorderColor = Color.parseColor("#334155")
        ),
        PostcardTheme(
            id = 1,
            name = "🌿 Эко Изумруд",
            bgStartColor = Color.parseColor("#042017"),
            bgEndColor = Color.parseColor("#0B3828"),
            primaryAccent = Color.parseColor("#10B981"), // Emerald Green
            secondaryAccent = Color.parseColor("#06D6A0"), // Mint
            cardBgColor = Color.parseColor("#0F3E30"),
            cardBorderColor = Color.parseColor("#1A5D49")
        ),
        PostcardTheme(
            id = 2,
            name = "🌅 Закатный Драйв",
            bgStartColor = Color.parseColor("#1E0B24"),
            bgEndColor = Color.parseColor("#38112C"),
            primaryAccent = Color.parseColor("#F43F5E"), // Coral Rose
            secondaryAccent = Color.parseColor("#F59E0B"), // Amber Gold
            cardBgColor = Color.parseColor("#311833"),
            cardBorderColor = Color.parseColor("#502753")
        ),
        PostcardTheme(
            id = 3,
            name = "🌌 Космос",
            bgStartColor = Color.parseColor("#070B1E"),
            bgEndColor = Color.parseColor("#121A3D"),
            primaryAccent = Color.parseColor("#38BDF8"), // Sky Blue
            secondaryAccent = Color.parseColor("#818CF8"), // Indigo
            cardBgColor = Color.parseColor("#162044"),
            cardBorderColor = Color.parseColor("#26376E")
        )
    )

    val PRESET_QUOTES = listOf(
        "⚡ Чистая энергия будущего! Путь свободен.",
        "🔋 Батарея заряжена на максимум — вперед к целям!",
        "🌱 Зелёные километры с заботой о планете.",
        "🚀 Быстрее чашки кофе, мощнее стихии!",
        "✨ Электродрайв в каждом пройденном километре."
    )

    fun generatePostcardBitmap(
        context: Context,
        session: ChargingSession,
        car: Car?,
        currency: String,
        themeIndex: Int,
        quoteText: String
    ): Bitmap {
        val theme = THEMES.getOrElse(themeIndex) { THEMES[0] }
        val width = 1080
        val height = 1350

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                theme.bgStartColor, theme.bgEndColor,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Decorative Background Circles / Glows
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        glowPaint.color = Color.argb(40, Color.red(theme.primaryAccent), Color.green(theme.primaryAccent), Color.blue(theme.primaryAccent))
        canvas.drawCircle(width * 0.85f, height * 0.15f, 320f, glowPaint)

        glowPaint.color = Color.argb(35, Color.red(theme.secondaryAccent), Color.green(theme.secondaryAccent), Color.blue(theme.secondaryAccent))
        canvas.drawCircle(width * 0.15f, height * 0.85f, 380f, glowPaint)

        // 3. Card Outer Frame with subtle border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.argb(60, 255, 255, 255)
        }
        val outerMargin = 40f
        val frameRect = RectF(outerMargin, outerMargin, width - outerMargin, height - outerMargin)
        canvas.drawRoundRect(frameRect, 40f, 40f, framePaint)

        // 4. Header Bar
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        textPaint.color = theme.primaryAccent
        textPaint.textSize = 34f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("⚡ VOLTLEDGER", 80f, 130f, textPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.08f
        }
        canvas.drawText("EV CHARGING REPORT", 80f, 165f, subPaint)

        // Date right-aligned
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
        val dateStr = dateFormat.format(Date(session.startTime))
        subPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dateStr, width - 80f, 140f, subPaint)
        subPaint.textAlign = Paint.Align.LEFT

        // 5. Vehicle & Station Header Box
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = theme.cardBgColor
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = theme.cardBorderColor
        }

        val stationBoxRect = RectF(80f, 210f, width - 80f, 340f)
        canvas.drawRoundRect(stationBoxRect, 28f, 28f, boxPaint)
        canvas.drawRoundRect(stationBoxRect, 28f, 28f, borderPaint)

        val carName = car?.name ?: "Мой Электромобиль"
        val opName = session.operatorName.ifBlank { "Зарядная станция" }
        val isDc = session.stationType.equals("DC", ignoreCase = true)
        val isHome = session.operatorName.contains("Дом", ignoreCase = true)

        textPaint.color = Color.WHITE
        textPaint.textSize = 36f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(carName, 120f, 270f, textPaint)

        subPaint.color = Color.argb(210, 255, 255, 255)
        subPaint.textSize = 24f
        canvas.drawText("📍 $opName", 120f, 308f, subPaint)

        // Station Type Pill Badge
        val badgeText = if (isHome) "HOME AC" else "${session.stationType} FAST"
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (isDc) theme.secondaryAccent else theme.primaryAccent
        }
        val badgeRect = RectF(width - 290f, 248f, width - 110f, 302f)
        canvas.drawRoundRect(badgeRect, 18f, 18f, badgePaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(badgeText, badgeRect.centerX(), badgeRect.centerY() + 8f, badgeTextPaint)

        // 6. Giant Highlight Box: Delivered kWh & Battery Visualization
        val mainBoxRect = RectF(80f, 370f, width - 80f, 650f)
        canvas.drawRoundRect(mainBoxRect, 32f, 32f, boxPaint)
        canvas.drawRoundRect(mainBoxRect, 32f, 32f, borderPaint)

        val kwhTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 255, 255, 255)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText("ЗАЛИЧКА ЭНЕРГИИ", mainBoxRect.centerX(), 425f, kwhTitlePaint)

        val bigKwhPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 86f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            shader = LinearGradient(
                mainBoxRect.centerX() - 250f, 460f,
                mainBoxRect.centerX() + 250f, 530f,
                theme.primaryAccent, theme.secondaryAccent,
                Shader.TileMode.CLAMP
            )
        }
        val kwhDelivered = session.kwhDeliveredByStation
        canvas.drawText("+${String.format(Locale.US, "%.1f", kwhDelivered)} кВт·ч", mainBoxRect.centerX(), 525f, bigKwhPaint)

        // Battery Progress Bar
        val barWidth = width - 240f
        val barHeight = 44f
        val barLeft = 120f
        val barTop = 560f
        val barRect = RectF(barLeft, barTop, barLeft + barWidth, barTop + barHeight)

        val barBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(barRect, 22f, 22f, barBgPaint)

        // Progress fill from startSoc to endSoc
        val startRatio = (session.startSoc / 100.0).coerceIn(0.0, 1.0).toFloat()
        val endRatio = (session.endSoc / 100.0).coerceIn(0.0, 1.0).toFloat()
        val fillLeft = barLeft + (barWidth * startRatio)
        val fillRight = barLeft + (barWidth * endRatio)

        if (fillRight > fillLeft) {
            val fillRect = RectF(fillLeft, barTop, fillRight, barTop + barHeight)
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    fillLeft, barTop, fillRight, barTop + barHeight,
                    theme.primaryAccent, theme.secondaryAccent,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(fillRect, 22f, 22f, fillPaint)
        }

        // Battery text percentages
        val socTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("${session.startSoc.toInt()}%  ➔  ${session.endSoc.toInt()}%", mainBoxRect.centerX(), 592f, socTextPaint)

        // 7. Four Stats Cards (2x2 Grid)
        val gridTop = 680f
        val cardGap = 24f
        val cardWidth = (width - 160f - cardGap) / 2f
        val cardHeight = 150f

        val passportConsumption = car?.passportConsumption?.takeIf { it > 5.0 } ?: 16.0
        val addedRangeKm = ((kwhDelivered / passportConsumption) * 100.0).toInt().coerceAtLeast(0)
        val co2SavedKg = String.format(Locale.US, "%.1f", kwhDelivered * 0.52) // ~0.52 kg CO2 saved per kWh vs gasoline
        val earnedXp = (kwhDelivered * 3 + if (isDc) 30 else 15).toInt()

        data class StatItem(val emoji: String, val label: String, val value: String, val color: Int)
        val stats = listOf(
            StatItem("🛣️", "ЗАПАС ХОДА", "+$addedRangeKm км", theme.primaryAccent),
            StatItem("💰", "СТОИМОСТЬ", "${String.format(Locale.US, "%.2f", session.totalCost)} $currency", Color.WHITE),
            StatItem("🌱", "СБЕРЕЖЕНО CO₂", "-$co2SavedKg кг", theme.secondaryAccent),
            StatItem("🏆", "НАГРАДА", "+$earnedXp XP", Color.parseColor("#FBBF24"))
        )

        for (i in 0 until 4) {
            val col = i % 2
            val row = i / 2
            val cLeft = 80f + col * (cardWidth + cardGap)
            val cTop = gridTop + row * (cardHeight + cardGap)
            val cRect = RectF(cLeft, cTop, cLeft + cardWidth, cTop + cardHeight)

            canvas.drawRoundRect(cRect, 24f, 24f, boxPaint)
            canvas.drawRoundRect(cRect, 24f, 24f, borderPaint)

            val sItem = stats[i]

            // Label
            val statLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 255, 255, 255)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("${sItem.emoji} ${sItem.label}", cLeft + 24f, cTop + 45f, statLabelPaint)

            // Value
            val statValPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sItem.color
                textSize = 36f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(sItem.value, cLeft + 24f, cTop + 105f, statValPaint)
        }

        // 8. Quote / Motto Box
        val quoteTop = 1040f
        val quoteRect = RectF(80f, quoteTop, width - 80f, quoteTop + 120f)
        val quoteBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, Color.red(theme.primaryAccent), Color.green(theme.primaryAccent), Color.blue(theme.primaryAccent))
        }
        canvas.drawRoundRect(quoteRect, 24f, 24f, quoteBgPaint)
        canvas.drawRoundRect(quoteRect, 24f, 24f, borderPaint)

        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("\"$quoteText\"", quoteRect.centerX(), quoteRect.centerY() + 9f, quotePaint)

        // 9. Watermark Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(160, 255, 255, 255)
            textSize = 22f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        canvas.drawText("⚡ Заряжено и посчитано в VoltLedger • Умный ассистент EV", width / 2f, height - 70f, footerPaint)

        return bitmap
    }

    fun savePostcardBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cacheDir = File(context.cacheDir, "postcards").apply { mkdirs() }
            val file = File(cacheDir, "voltledger_card_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun buildShareText(
        session: ChargingSession,
        car: Car?,
        currency: String,
        quoteText: String
    ): String {
        val opName = session.operatorName.ifBlank { "Электрозарядка" }
        val carName = car?.name ?: "Мой Электромобиль"
        val delivered = session.kwhDeliveredByStation
        val passportConsumption = car?.passportConsumption?.takeIf { it > 5.0 } ?: 16.0
        val addedRangeKm = ((delivered / passportConsumption) * 100.0).toInt().coerceAtLeast(0)
        val co2SavedKg = String.format(Locale.US, "%.1f", delivered * 0.52)
        val earnedXp = (delivered * 3 + if (session.stationType.equals("DC", true)) 30 else 15).toInt()

        return """
            ⚡ $quoteText
            
            🚗 Автомобиль: $carName
            📍 Станция: $opName (${session.stationType})
            🔋 Залито энергии: +${String.format(Locale.US, "%.1f", delivered)} кВт·ч (${session.startSoc.toInt()}% → ${session.endSoc.toInt()}%)
            🛣️ Добавлено хода: ~+$addedRangeKm км
            💰 Стоимость: ${String.format(Locale.US, "%.2f", session.totalCost)} $currency
            🌱 Эко-эффект: -$co2SavedKg кг CO₂
            🏆 Награда: +$earnedXp XP в VoltLedger!
            
            #VoltLedger #EV #ElectroCar #Charging #Электромобиль
        """.trimIndent()
    }

    fun shareCardImage(
        context: Context,
        session: ChargingSession,
        car: Car?,
        currency: String,
        themeIndex: Int,
        quoteText: String
    ) {
        try {
            val bitmap = generatePostcardBitmap(context, session, car, currency, themeIndex, quoteText)
            val uri = savePostcardBitmapToCache(context, bitmap)
            val text = buildShareText(session, car, currency, quoteText)

            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Поделиться открыткой зарядки")
                context.startActivity(chooser)
            } else {
                shareCardText(context, session, car, currency, quoteText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Не удалось сформировать изображение, отправляем текстом", Toast.LENGTH_SHORT).show()
            shareCardText(context, session, car, currency, quoteText)
        }
    }

    fun shareCardText(
        context: Context,
        session: ChargingSession,
        car: Car?,
        currency: String,
        quoteText: String
    ) {
        val text = buildShareText(session, car, currency, quoteText)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Поделиться сессией зарядки")
        context.startActivity(chooser)
    }
}
