package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double, currency: String): String {
    return if (amount <= 0.0001) {
        "0.00 $currency"
    } else {
        String.format(Locale.getDefault(), "%.2f %s", amount, currency)
    }
}

fun formatPricePerKwh(price: Double, currency: String): String {
    return if (price <= 0.0001) {
        "0 $currency"
    } else {
        String.format(Locale.getDefault(), "%.2f %s", price, currency)
    }
}

fun formatDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

fun formatShortDate(timeMillis: Long): String {
    val sdf = SimpleDateFormat("dd.MM", Locale.getDefault())
    return sdf.format(Date(timeMillis))
}

@Composable
fun VoltCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
    content: @Composable () -> Unit
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(20.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun StationTypeBadge(type: String) {
    val isDc = type.equals("DC", ignoreCase = true)
    val bgColor = if (isDc) ElectricCyan.copy(alpha = 0.18f) else BatteryGreen.copy(alpha = 0.18f)
    val textColor = if (isDc) ElectricCyan else BatteryGreen
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = type.uppercase(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TagBadge(name: String, color: Long) {
    if (name.isBlank()) return
    val tagColor = Color(color)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tagColor.copy(alpha = 0.2f))
            .border(0.5.dp, tagColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tagColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = name,
                color = tagColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String = "",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = SoftBlue
) {
    VoltCard(
        modifier = modifier,
        borderColor = accentColor.copy(alpha = 0.35f)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (unit.isNotBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
