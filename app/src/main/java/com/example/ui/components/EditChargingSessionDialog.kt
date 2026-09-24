package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChargingSession
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditChargingSessionDialog(
    session: ChargingSession,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ChargingSession) -> Unit,
    onDelete: (ChargingSession) -> Unit,
    onShare: (ChargingSession) -> Unit
) {
    val strings = LocalAppStrings.current

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    var editOperatorName by remember(session.id) { mutableStateOf(session.operatorName) }
    var editStationType by remember(session.id) { mutableStateOf(session.stationType) }
    var editStartSoc by remember(session.id) { mutableStateOf(session.startSoc.toInt().toString()) }
    var editEndSoc by remember(session.id) { mutableStateOf(session.endSoc.toInt().toString()) }
    var editKwh by remember(session.id) { mutableStateOf(String.format(Locale.US, "%.2f", session.kwhDeliveredByStation)) }
    var editKwhReceived by remember(session.id) {
        mutableStateOf(session.kwhReceivedByCar?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var editPricePerKwh by remember(session.id) { mutableStateOf(String.format(Locale.US, "%.4f", session.pricePerKwh)) }
    var editPenaltyCost by remember(session.id) { mutableStateOf(String.format(Locale.US, "%.2f", session.penaltyCost)) }
    var editFixedAmount by remember(session.id) { mutableStateOf(String.format(Locale.US, "%.2f", session.fixedAmount)) }
    var editCost by remember(session.id) { mutableStateOf(String.format(Locale.US, "%.2f", session.totalCost)) }
    var editOdometer by remember(session.id) { mutableStateOf(session.startOdometer.toInt().toString()) }
    var editComment by remember(session.id) { mutableStateOf(session.operatorComment ?: "") }
    var editDateTimeText by remember(session.id) { mutableStateOf(dateFormat.format(Date(session.startTime))) }

    fun recalculateCost() {
        val kwh = editKwh.toDoubleOrNull() ?: 0.0
        val price = editPricePerKwh.toDoubleOrNull() ?: 0.0
        val penalty = editPenaltyCost.toDoubleOrNull() ?: 0.0
        val fixed = editFixedAmount.toDoubleOrNull() ?: 0.0
        val computed = (kwh * price) + penalty + fixed
        editCost = String.format(Locale.US, "%.2f", computed)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Редактирование зарядки",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = {
                        onShare(session)
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться открыткой",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Station name / operator
                OutlinedTextField(
                    value = editOperatorName,
                    onValueChange = { editOperatorName = it },
                    label = { Text("Оператор / Название станции") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Station Type Chips
                Column {
                    Text("Тип станции:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AC", "DC", "Home").forEach { type ->
                            val isSelected = editStationType.equals(type, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { editStationType = type },
                                label = { Text(type, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan.copy(alpha = 0.25f),
                                    selectedLabelColor = ElectricCyan
                                )
                            )
                        }
                    }
                }

                // SOC Start -> End
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editStartSoc,
                        onValueChange = { editStartSoc = it },
                        label = { Text("Начальный %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editEndSoc,
                        onValueChange = { editEndSoc = it },
                        label = { Text("Конечный %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Delivered kWh and Price per kWh
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editKwh,
                        onValueChange = { editKwh = it },
                        label = { Text("Залито кВт·ч") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editPricePerKwh,
                        onValueChange = { editPricePerKwh = it },
                        label = { Text("Тариф ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Total Cost with Quick Recalculate Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editCost,
                        onValueChange = { editCost = it },
                        label = { Text("Итоговая сумма ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedButton(
                        onClick = { recalculateCost() },
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "Пересчитать", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Авто", fontSize = 12.sp)
                    }
                }

                // Optional received by car kWh (for charging efficiency)
                OutlinedTextField(
                    value = editKwhReceived,
                    onValueChange = { editKwhReceived = it },
                    label = { Text("Принято батареей кВт·ч (опционально)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Odometer
                OutlinedTextField(
                    value = editOdometer,
                    onValueChange = { editOdometer = it },
                    label = { Text("Одометр / Пробег (км)") },
                    leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = SoftBlue) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Date & Time
                OutlinedTextField(
                    value = editDateTimeText,
                    onValueChange = { editDateTimeText = it },
                    label = { Text("Дата и время (ГГГГ-ММ-ДД ЧЧ:ММ)") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, tint = SoftBlue) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Comment or Meter reading
                OutlinedTextField(
                    value = editComment,
                    onValueChange = { editComment = it },
                    label = { Text("Заметка / Счётчик / Тег") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDate = try {
                        dateFormat.parse(editDateTimeText)?.time ?: session.startTime
                    } catch (e: Exception) {
                        session.startTime
                    }

                    val kwh = editKwh.toDoubleOrNull() ?: session.kwhDeliveredByStation
                    val price = editPricePerKwh.toDoubleOrNull() ?: session.pricePerKwh
                    val penalty = editPenaltyCost.toDoubleOrNull() ?: session.penaltyCost
                    val fixed = editFixedAmount.toDoubleOrNull() ?: session.fixedAmount
                    val cost = editCost.toDoubleOrNull() ?: session.totalCost

                    val updated = session.copy(
                        operatorName = editOperatorName.trim().ifBlank { "Зарядная станция" },
                        stationType = editStationType,
                        startSoc = (editStartSoc.toDoubleOrNull() ?: session.startSoc).coerceIn(0.0, 100.0),
                        endSoc = (editEndSoc.toDoubleOrNull() ?: session.endSoc).coerceIn(0.0, 100.0),
                        kwhDeliveredByStation = kwh,
                        kwhReceivedByCar = editKwhReceived.toDoubleOrNull(),
                        pricePerKwh = price,
                        penaltyCost = penalty,
                        fixedAmount = fixed,
                        totalCost = cost,
                        energyCost = kwh * price,
                        startOdometer = editOdometer.toDoubleOrNull() ?: session.startOdometer,
                        operatorComment = editComment.trim().ifEmpty { null },
                        startTime = parsedDate
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onDelete(session) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Удалить")
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
