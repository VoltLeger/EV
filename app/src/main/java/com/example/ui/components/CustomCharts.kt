package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class ChartPoint(
    val label: String,
    val value: Float
)

data class BarChartItem(
    val label: String,
    val energyCost: Float,
    val penaltyCost: Float
)

data class DonutSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun ConsumptionLineChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = SoftBlue,
    fillGradient: Brush = Brush.verticalGradient(
        colors = listOf(SoftBlue.copy(alpha = 0.35f), Color.Transparent)
    )
) {
    if (points.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val maxVal = remember(points) { (points.maxOfOrNull { it.value } ?: 10f).coerceAtLeast(10f) * 1.15f }
    val minVal = remember(points) { (points.minOfOrNull { it.value } ?: 0f).coerceAtLeast(0f) * 0.85f }
    val range = remember(maxVal, minVal) { (maxVal - minVal).coerceAtLeast(1f) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height - 24.dp.toPx()

                // Draw background grid lines
                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = h * (i.toFloat() / gridLines)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.15f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (points.size == 1) {
                    // Single point
                    val cx = w / 2f
                    val cy = h - ((points[0].value - minVal) / range) * h
                    drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(cx, cy))
                    return@Canvas
                }

                val path = Path()
                val fillPath = Path()
                val stepX = w / (points.size - 1)

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = h - ((pt.value - minVal) / range) * h
                    if (i == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevX = (i - 1) * stepX
                        val prevY = h - ((points[i - 1].value - minVal) / range) * h
                        val cx1 = prevX + (x - prevX) / 2f
                        val cx2 = cx1
                        path.cubicTo(cx1, prevY, cx2, y, x, y)
                        fillPath.cubicTo(cx1, prevY, cx2, y, x, y)
                    }
                    if (i == points.size - 1) {
                        fillPath.lineTo(x, h)
                        fillPath.close()
                    }
                }

                drawPath(path = fillPath, brush = fillGradient)
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw points
                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = h - ((pt.value - minVal) / range) * h
                    drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = lineColor, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        // X-axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (points.size > 5) points.size / 4 else 1
            points.forEachIndexed { i, pt ->
                if (i % step == 0 || i == points.size - 1) {
                    Text(
                        text = pt.label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ExpensesBarChart(
    items: List<BarChartItem>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val maxTotal = remember(items) { items.maxOfOrNull { it.energyCost + it.penaltyCost }?.coerceAtLeast(1f) ?: 10f }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height - 24.dp.toPx()
                val barWidth = (w / (items.size * 1.5f)).coerceIn(12.dp.toPx(), 36.dp.toPx())
                val spacing = (w - (barWidth * items.size)) / (items.size + 1)

                items.forEachIndexed { i, item ->
                    val x = spacing + i * (barWidth + spacing)
                    val total = item.energyCost + item.penaltyCost
                    val totalH = (total / maxTotal) * h
                    val penaltyH = if (total > 0) (item.penaltyCost / maxTotal) * h else 0f
                    val energyH = totalH - penaltyH

                    // Energy Bar
                    drawRoundRect(
                        color = SoftBlue,
                        topLeft = Offset(x, h - totalH),
                        size = Size(barWidth, energyH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Penalty Bar on top
                    if (penaltyH > 0) {
                        drawRoundRect(
                            color = BatteryOrange,
                            topLeft = Offset(x, h - penaltyH),
                            size = Size(barWidth, penaltyH),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach {
                Text(
                    text = it.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutBreakdownChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    centerTitle: String = "",
    centerValue: String = ""
) {
    if (slices.isEmpty() || slices.all { it.value <= 0f }) {
        EmptyChartPlaceholder()
        return
    }

    val total = remember(slices) { slices.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24.dp.toPx()
                var startAngle = -90f

                slices.forEach { slice ->
                    val sweepAngle = (slice.value / total) * 360f
                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle - 2f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth)
                        )
                    }
                    startAngle += sweepAngle
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (centerValue.isNotBlank()) {
                    Text(
                        text = centerValue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (centerTitle.isNotBlank()) {
                    Text(
                        text = centerTitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Legend
        FlowRow(
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            slices.forEach { slice ->
                val pct = if (total > 0) (slice.value / total) * 100 else 0f
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(slice.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${slice.label} (${String.format(Locale.getDefault(), "%.0f%%", pct)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChartPlaceholder(text: String = "Нет данных для графика") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * High-precision smooth monthly consumption chart.
 * Top: consumption metric & min/max bounds.
 * Center: smooth cubic Bezier curve showing how consumption evolves.
 * Bottom: dates of the month.
 */
@Composable
fun SmoothConsumptionMonthChart(
    points: List<ChartPoint>,
    modifier: Modifier = Modifier,
    averageVal: Double? = null,
    passportVal: Double? = null
) {
    VoltCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        borderColor = ElectricCyan.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Top Section: Header & Numbers (сверху расход)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📈 КРИВАЯ РАСХОДА ЭНЕРГИИ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricCyan,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayAvg = averageVal ?: (points.map { it.value.toDouble() }.average().takeIf { !it.isNaN() })
                    if (displayAvg != null && displayAvg > 0.0) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = String.format(Locale.US, "%.1f", displayAvg),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "кВт·ч / 100 км",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ElectricCyan,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "— кВт·ч / 100 км",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Min and Max badges on top right
                if (points.isNotEmpty()) {
                    val maxVal = points.maxOf { it.value }
                    val minVal = points.minOf { it.value }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Макс: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", maxVal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BatteryOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Мин: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${String.format(Locale.US, "%.1f", minVal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BatteryGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Body: Smooth Curve Canvas
            if (points.isEmpty()) {
                EmptyChartPlaceholder(text = "Заряжайте авто для построения кривой расхода")
            } else {
                val maxVal = remember(points) {
                    val m = points.maxOfOrNull { it.value } ?: 20f
                    (m * 1.15f).coerceAtLeast(15f)
                }
                val minVal = remember(points) {
                    val m = points.minOfOrNull { it.value } ?: 10f
                    (m * 0.85f).coerceAtLeast(0f)
                }
                val range = remember(maxVal, minVal) { (maxVal - minVal).coerceAtLeast(1f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height - 18.dp.toPx()

                        // Grid lines
                        val gridLines = 3
                        for (i in 0..gridLines) {
                            val y = h * (i.toFloat() / gridLines)
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        if (points.size == 1) {
                            val cx = w / 2f
                            val cy = h - ((points[0].value - minVal) / range) * h
                            drawCircle(color = ElectricCyan.copy(alpha = 0.3f), radius = 12.dp.toPx(), center = Offset(cx, cy))
                            drawCircle(color = ElectricCyan, radius = 6.dp.toPx(), center = Offset(cx, cy))
                            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, cy))
                            return@Canvas
                        }

                        val path = Path()
                        val fillPath = Path()
                        val stepX = w / (points.size - 1)

                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = h - ((pt.value - minVal) / range) * h
                            if (i == 0) {
                                path.moveTo(x, y)
                                fillPath.moveTo(x, h)
                                fillPath.lineTo(x, y)
                            } else {
                                val prevX = (i - 1) * stepX
                                val prevY = h - ((points[i - 1].value - minVal) / range) * h
                                val cx1 = prevX + (x - prevX) / 2f
                                val cx2 = cx1
                                path.cubicTo(cx1, prevY, cx2, y, x, y)
                                fillPath.cubicTo(cx1, prevY, cx2, y, x, y)
                            }
                            if (i == points.size - 1) {
                                fillPath.lineTo(x, h)
                                fillPath.close()
                            }
                        }

                        // Gradient fill under curve
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(ElectricCyan.copy(alpha = 0.40f), BatteryGreen.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )

                        // Smooth curve stroke
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(listOf(ElectricCyan, BatteryGreen)),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Points with luminous glow
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = h - ((pt.value - minVal) / range) * h
                            drawCircle(color = ElectricCyan.copy(alpha = 0.35f), radius = 8.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = ElectricCyan, radius = 4.5.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. Bottom Section: Dates of the month (снизу даты месяца)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val step = if (points.size > 6) points.size / 5 else 1
                    points.forEachIndexed { i, pt ->
                        if (i % step == 0 || i == points.size - 1) {
                            Text(
                                text = pt.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
