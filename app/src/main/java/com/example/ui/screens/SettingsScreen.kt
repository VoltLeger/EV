package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.ui.components.TagBadge
import com.example.ui.components.VoltCard
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    allCars: List<Car>,
    activeCar: Car?,
    operators: List<Operator>,
    tags: List<Tag>,
    onSelectCar: (Long) -> Unit,
    onAddCar: (String, Double, Double, Double) -> Unit,
    onDeleteCar: (Car) -> Unit,
    onAddOperator: (Operator) -> Unit,
    onUpdateOperator: (Operator) -> Unit,
    onDeleteOperator: (Operator) -> Unit,
    onAddTag: (String, Long) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onUpdateLanguage: (String) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateCurrency: (String) -> Unit,
    onUpdateAutoNightTariff: (Boolean) -> Unit,
    onUpdateNotificationsEnabled: (Boolean) -> Unit,
    onUpdateNotifyUnfinished: (Boolean) -> Unit,
    onUpdateUnfinishedHours: (Int) -> Unit,
    onUpdateNotifyWeekly: (Boolean) -> Unit,
    onUpdateNotifyMonthly: (Boolean) -> Unit,
    onSendTestNotification: () -> Unit,
    onExportCsv: () -> String,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onRefreshTariffs: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddOperatorDialog by remember { mutableStateOf(false) }
    var editingOperator by remember { mutableStateOf<Operator?>(null) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section 1: Vehicles
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.carsSection,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showAddCarDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = strings.addCar, tint = SoftBlue)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                allCars.forEach { car ->
                    val isActive = car.id == activeCar?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) SoftBlue.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelectCar(car.id) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = car.name,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "✓ Активен",
                                        fontSize = 12.sp,
                                        color = SoftBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ёмкость: ${car.declaredCapacityKwh.toInt()} кВт·ч (полезная: ${String.format(Locale.getDefault(), "%.1f", car.usableCapacityKwh)})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (allCars.size > 1 && !isActive) {
                            IconButton(onClick = { onDeleteCar(car) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // Section 2: Operators & Tariffs Directory
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.operatorsSection,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.directoryVersion,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            onRefreshTariffs()
                            Toast.makeText(context, strings.reloadTariffsFromFile, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = strings.reloadTariffsFromFile, tint = SoftBlue)
                        }
                        IconButton(onClick = { showAddOperatorDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = strings.addOperator, tint = SoftBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        onRefreshTariffs()
                        Toast.makeText(context, strings.reloadTariffsFromFile, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.reloadTariffsFromFile, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto Night Tariff Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.autoNightTariff,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = settings.autoNightTariff,
                        onCheckedChange = { onUpdateAutoNightTariff(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                operators.forEach { op ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = op.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            val priceStr = if (op.isFree) {
                                "Бесплатно"
                            } else {
                                "AC: ${String.format(Locale.US, "%.2f", op.priceAc)} | DC: ${String.format(Locale.US, "%.2f", op.priceDc)} $currency"
                            }
                            Text(text = priceStr, fontSize = 12.sp, color = ElectricCyan)
                            if (op.penaltyIdlePerMin > 0) {
                                Text(
                                    text = "Простой: ${String.format(Locale.US, "%.2f", op.penaltyIdlePerMin)}/мин после ${op.penaltyFreeMinutes} мин",
                                    fontSize = 11.sp,
                                    color = BatteryGreen
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingOperator = op },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = SoftBlue, modifier = Modifier.size(18.dp))
                            }
                            if (!op.isBuiltin) {
                                IconButton(
                                    onClick = { onDeleteOperator(op) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Tags
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.tagsSection,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { showAddTagDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = strings.addTag, tint = SoftBlue)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tags.forEach { tag ->
                        TagBadge(name = tag.name, color = tag.color)
                    }
                }
            }
        }

        // Section 4: Currency, Language & Theme
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.appearanceSection,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Currency selector
                Text(text = strings.currency, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("BYN", "RUB", "PLN", "USD", "EUR").forEach { curr ->
                        FilterChip(
                            selected = settings.currency == curr,
                            onClick = { onUpdateCurrency(curr) },
                            label = { Text(curr) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftBlue, selectedLabelColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Language selector
                Text(text = strings.language, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ru" to "Русский", "en" to "English").forEach { (code, label) ->
                        FilterChip(
                            selected = settings.language == code,
                            onClick = { onUpdateLanguage(code) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftBlue, selectedLabelColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Theme selector: Dark, AMOLED, Light, System
                Text(text = strings.theme, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "dark" to strings.themeDark,
                        "amoled" to strings.themeAmoled,
                        "light" to strings.themeLight,
                        "system" to strings.themeSystem
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.theme == mode,
                            onClick = { onUpdateTheme(mode) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SoftBlue, selectedLabelColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Section 5: Smart Notifications
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.notificationsSection,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Enable notifications
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.enableNotifications, fontSize = 13.sp)
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { onUpdateNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Unfinished charge alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.unfinishedNotify, fontSize = 13.sp)
                    Switch(
                        checked = settings.notifyUnfinished,
                        onCheckedChange = { onUpdateNotifyUnfinished(it) },
                        enabled = settings.notificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                    )
                }

                // Weekly report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.weeklyReport, fontSize = 13.sp)
                    Switch(
                        checked = settings.notifyWeekly,
                        onCheckedChange = { onUpdateNotifyWeekly(it) },
                        enabled = settings.notificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                    )
                }

                // Monthly report
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = strings.monthlyReport, fontSize = 13.sp)
                    Switch(
                        checked = settings.notifyMonthly,
                        onCheckedChange = { onUpdateNotifyMonthly(it) },
                        enabled = settings.notificationsEnabled,
                        colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onSendTestNotification,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = strings.testNotification, fontSize = 13.sp)
                }
            }
        }

        // Section 6: Data & Backup
        item {
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = strings.dataSection,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Export CSV
                Button(
                    onClick = {
                        val csv = onExportCsv()
                        shareText(context, csv, "VoltLedger_export.csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlue)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.exportCsv)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Export JSON Full Backup
                OutlinedButton(
                    onClick = {
                        val json = onExportJson()
                        shareText(context, json, "VoltLedger_backup.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.exportJson)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Import JSON
                OutlinedButton(
                    onClick = { showImportJsonDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = strings.importData)
                }
            }
        }
    }

    // Dialog: Add Car
    if (showAddCarDialog) {
        var newName by remember { mutableStateOf("") }
        var newCapacity by remember { mutableStateOf("60.0") }
        var newOdo by remember { mutableStateOf("0") }
        var newSoc by remember { mutableStateOf("50") }

        AlertDialog(
            onDismissRequest = { showAddCarDialog = false },
            title = { Text(strings.addCar) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(strings.carName) }, singleLine = true)
                    OutlinedTextField(value = newCapacity, onValueChange = { newCapacity = it }, label = { Text(strings.declaredCapacity) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = newOdo, onValueChange = { newOdo = it }, label = { Text(strings.initialOdometer) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = newSoc, onValueChange = { newSoc = it }, label = { Text(strings.currentSoc) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cap = newCapacity.toDoubleOrNull() ?: 60.0
                    val odo = newOdo.toDoubleOrNull() ?: 0.0
                    val soc = newSoc.toDoubleOrNull() ?: 50.0
                    if (newName.isNotBlank()) {
                        onAddCar(newName.trim(), cap, odo, soc)
                        showAddCarDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCarDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Edit or Add Operator
    if (showAddOperatorDialog || editingOperator != null) {
        val isEditing = editingOperator != null
        val target = editingOperator
        var opName by remember { mutableStateOf(target?.name ?: "") }
        var opPriceAc by remember { mutableStateOf(target?.priceAc?.toString() ?: "0.55") }
        var opPriceDc by remember { mutableStateOf(target?.priceDc?.toString() ?: "0.73") }
        var opPenaltyPerMin by remember { mutableStateOf(target?.penaltyIdlePerMin?.toString() ?: "0.00") }
        var opPenaltyFreeMin by remember { mutableStateOf(target?.penaltyFreeMinutes?.toString() ?: "0") }
        var opComment by remember { mutableStateOf(target?.comment ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddOperatorDialog = false
                editingOperator = null
            },
            title = { Text(if (isEditing) "Редактировать оператора" else strings.addOperator) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = opName, onValueChange = { opName = it }, label = { Text("Название") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opPriceAc, onValueChange = { opPriceAc = it }, label = { Text("Цена AC") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = opPriceDc, onValueChange = { opPriceDc = it }, label = { Text("Цена DC") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opPenaltyPerMin, onValueChange = { opPenaltyPerMin = it }, label = { Text("Штраф/мин") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = opPenaltyFreeMin, onValueChange = { opPenaltyFreeMin = it }, label = { Text("Бесплатно (мин)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    OutlinedTextField(value = opComment, onValueChange = { opComment = it }, label = { Text("Комментарий / Примечание") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pAc = opPriceAc.toDoubleOrNull() ?: 0.55
                    val pDc = opPriceDc.toDoubleOrNull() ?: 0.73
                    val pPen = opPenaltyPerMin.toDoubleOrNull() ?: 0.0
                    val freeM = opPenaltyFreeMin.toIntOrNull() ?: 0
                    if (opName.isNotBlank()) {
                        if (isEditing && target != null) {
                            onUpdateOperator(target.copy(name = opName, priceAc = pAc, priceDc = pDc, penaltyIdlePerMin = pPen, penaltyFreeMinutes = freeM, comment = opComment))
                        } else {
                            onAddOperator(Operator(name = opName, type = "both", priceAc = pAc, priceDc = pDc, penaltyIdlePerMin = pPen, penaltyFreeMinutes = freeM, comment = opComment, isBuiltin = false))
                        }
                        showAddOperatorDialog = false
                        editingOperator = null
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddOperatorDialog = false
                    editingOperator = null
                }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Add Tag
    if (showAddTagDialog) {
        var tagName by remember { mutableStateOf("") }
        var selectedColor by remember { mutableStateOf(0xFF3B82F6) }

        val colorOptions = listOf(0xFF10B981, 0xFF3B82F6, 0xFFF59E0B, 0xFF8B5CF6, 0xFFEC4899, 0xFF06B6D4)

        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text(strings.addTag) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = tagName, onValueChange = { tagName = it }, label = { Text("Название тега") }, singleLine = true)
                    Text("Выберите цвет:", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        colorOptions.forEach { col ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(col))
                                    .clickable { selectedColor = col }
                                    .border(
                                        width = if (selectedColor == col) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tagName.isNotBlank()) {
                        onAddTag(tagName.trim(), selectedColor)
                        showAddTagDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Import Backup
    if (showImportJsonDialog) {
        AlertDialog(
            onDismissRequest = { showImportJsonDialog = false },
            title = { Text(strings.importData) },
            text = {
                Column {
                    Text("Вставьте JSON или текст резервной копии:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        placeholder = { Text("{ ... }") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonText.isNotBlank()) {
                        onImportJson(importJsonText.trim())
                        showImportJsonDialog = false
                    }
                }) { Text("Импортировать") }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

fun shareText(context: Context, text: String, fileName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, fileName)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share $fileName"))
}
