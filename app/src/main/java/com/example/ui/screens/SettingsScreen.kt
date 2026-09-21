package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppSettings
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.VoltAvatar
import com.example.ui.components.VoltCard
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    allCars: List<Car>,
    activeCar: Car?,
    operators: List<Operator>,
    tags: List<Tag>,
    onSelectCar: (Long) -> Unit,
    onAddCar: (String, Double, Double, Double, Double) -> Unit,
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
    onUpdateNotifyAchievements: (Boolean) -> Unit = {},
    onSendTestNotification: () -> Unit,
    onTestDcNotification: () -> Unit = {},
    onTestAchievementUnlocked: () -> Unit = {},
    onTestAchievementProgress: () -> Unit = {},
    onUpdatePinSettings: (Boolean, String) -> Unit = { _, _ -> },
    onUpdateBiometricSettings: (Boolean) -> Unit = {},
    onExportCsv: () -> String,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit,
    onRefreshTariffs: () -> Unit = {},
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    onUpdateUserEmail: (String) -> Unit = {},
    onSendEmailBackup: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    var showAddCarDialog by remember { mutableStateOf(false) }
    var showAddOperatorDialog by remember { mutableStateOf(false) }
    var editingOperator by remember { mutableStateOf<Operator?>(null) }
    var operatorToDelete by remember { mutableStateOf<Operator?>(null) }
    var showImportJsonDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember(userProfile?.email) { mutableStateOf(userProfile?.email ?: "") }
    var showPhoneTransferAdviceDialog by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = onExportJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Файл резервной копии успешно сохранён!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!content.isNullOrBlank()) {
                    onImportJson(content)
                    Toast.makeText(context, "Резервная копия успешно восстановлена!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Выбранный файл пуст", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Ошибка чтения: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LiquidGlassBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
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
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) SoftBlue.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { onSelectCar(car.id) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoftBlue.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!car.photoUri.isNullOrBlank()) {
                                        AsyncImage(
                                            model = car.photoUri,
                                            contentDescription = "Фото ${car.name}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = SoftBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = car.name,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isActive) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "✓",
                                                fontSize = 12.sp,
                                                color = SoftBlue,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${car.declaredCapacityKwh.toInt()} кВт·ч • ${car.initialOdometer.toInt()} км",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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

            // Section 2: AC Operators & Tariffs
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AC Операторы и Тарифы",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Медленные зарядки (Дом, Дача, город)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showAddOperatorDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Добавить AC оператора", tint = SoftBlue)
                        }
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

                    val acOps = operators.filter { it.type.equals("AC", true) }
                    if (acOps.isEmpty()) {
                        Text("Нет AC операторов", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        acOps.forEach { op ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    val nameDisplay = if (op.subType.isNotBlank()) "${op.name} (${op.subType})" else op.name
                                    Text(
                                        text = nameDisplay,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val nightStr = if (op.nightPriceAc != null) " | Ночь: ${String.format(Locale.US, "%.2f", op.nightPriceAc)}" else ""
                                    Text(
                                        text = "Тариф: ${String.format(Locale.US, "%.2f", op.priceAc)}$nightStr $currency",
                                        fontSize = 12.sp,
                                        color = ElectricCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (op.comment.isNotBlank()) {
                                        Text(
                                            text = op.comment,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                    IconButton(
                                        onClick = { operatorToDelete = op },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }

            // Section 3: DC Operators & Tariffs
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DC Операторы и Тарифы",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Быстрые зарядные станции",
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

                    Spacer(modifier = Modifier.height(10.dp))

                    val dcOps = operators.filter { it.type.equals("DC", true) }
                    if (dcOps.isEmpty()) {
                        Text("Нет DC операторов", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        dcOps.forEach { op ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = op.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val nightStr = if (op.nightPriceDc != null) " | Ночь: ${String.format(Locale.US, "%.2f", op.nightPriceDc)}" else ""
                                    Text(
                                        text = "Тариф: ${String.format(Locale.US, "%.2f", op.priceDc)}$nightStr $currency",
                                        fontSize = 12.sp,
                                        color = ElectricCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (op.penaltyIdlePerMin > 0) {
                                        Text(
                                            text = "Простой: ${String.format(Locale.US, "%.2f", op.penaltyIdlePerMin)}/мин после ${op.penaltyFreeMinutes} мин",
                                            fontSize = 11.sp,
                                            color = BatteryGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                    IconButton(
                                        onClick = { operatorToDelete = op },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
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

                    // Theme selector: Dark, Light, Mint, System (wrapped in FlowRow to prevent overflow)
                    Text(text = strings.theme, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            "dark" to strings.themeDark,
                            "light" to strings.themeLight,
                            "mint" to strings.themeMint,
                            "system" to strings.themeSystem
                        ).forEach { (mode, label) ->
                            val isSelected = settings.theme == mode || ((settings.theme == "wrnc" || settings.theme == "amoled") && mode == "dark")
                            FilterChip(
                                selected = isSelected,
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

                    // Achievement notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = strings.notifyAchievements, fontSize = 13.sp)
                            Text(text = strings.notifyAchievementsDesc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.notifyAchievements,
                            onCheckedChange = { onUpdateNotifyAchievements(it) },
                            enabled = settings.notificationsEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = SoftBlue, checkedTrackColor = SoftBlue.copy(alpha = 0.4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onSendTestNotification,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = strings.testNotification, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onTestDcNotification,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "⚡ " + strings.testDcNotification, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onTestAchievementUnlocked,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = strings.testAchievementUnlocked, fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onTestAchievementProgress,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = strings.testAchievementProgress, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Section 5.5: Security & PIN Lock
            item {
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf(settings.pinCode) }

                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = strings.securitySection,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = strings.pinLockEnabled, fontSize = 14.sp)
                            Text(
                                text = if (settings.pinEnabled) "PIN: ••••" else "Защита отключена",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.pinEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked && settings.pinCode.isBlank()) {
                                    showPinDialog = true
                                } else {
                                    onUpdatePinSettings(isChecked, settings.pinCode)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = ElectricCyan.copy(alpha = 0.4f))
                        )
                    }

                    if (settings.pinEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Изменить 4-значный PIN", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Biometric Unlock Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = strings.biometricLockEnabled, fontSize = 14.sp)
                            Text(
                                text = if (settings.biometricEnabled) "Биометрия активна" else "Отключено",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = settings.biometricEnabled,
                            onCheckedChange = { isChecked ->
                                onUpdateBiometricSettings(isChecked)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = ElectricCyan.copy(alpha = 0.4f))
                        )
                    }
                }

                if (showPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showPinDialog = false },
                        title = { Text(strings.setPinTitle) },
                        text = {
                            Column {
                                Text("Введите 4 цифры для защиты доступа:", fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinInput = it },
                                    label = { Text("4 цифры") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (pinInput.length == 4) {
                                        onUpdatePinSettings(true, pinInput)
                                        showPinDialog = false
                                        Toast.makeText(context, "PIN-код успешно сохранён", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = pinInput.length == 4
                            ) {
                                Text(strings.save)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPinDialog = false }) {
                                Text(strings.cancel)
                            }
                        }
                    )
                }
            }

            // Section 6: Google Drive & Cloud Backup
            item {
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Google Диск и Резервные копии",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Простое сохранение и перенос всех данных",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showPhoneTransferAdviceDialog = true }) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Инструкция по переносу", tint = SoftBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Google Drive Card (Simple 1-tap UX)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElectricCyan.copy(alpha = 0.08f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Google Диск (Рекомендуется)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ElectricCyan)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Сохраняйте базу со всеми авто, зарядками и тарифами в ваше облако Google или восстанавливайте в 1 клик без сложных настроек.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                        createDocumentLauncher.launch("voltledger_backup_$timeStamp.json")
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("На Google Диск", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        openDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftBlue)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("С Google Диска", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    val json = onExportJson()
                                    shareBackupFile(context, json, "voltledger_backup_$timeStamp.json")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Поделиться файлом (Google Диск / Мессенджер)", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Backup Section
                    val hasEmail = !userProfile?.email.isNullOrBlank()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (hasEmail) BatteryGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(1.dp, if (hasEmail) BatteryGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (hasEmail) BatteryGreen.copy(alpha = 0.2f) else SoftBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hasEmail) Icons.Default.CheckCircle else Icons.Default.Email,
                                    contentDescription = null,
                                    tint = if (hasEmail) BatteryGreen else SoftBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (hasEmail) userProfile?.email.orEmpty() else "E-mail не привязан",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (hasEmail) "Основной адрес для копий" else "Привяжите для защиты и отправки копий",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                emailInput = userProfile?.email ?: ""
                                showEmailDialog = true
                            }
                        ) {
                            Text(if (hasEmail) "Изменить" else "Привязать", fontSize = 12.sp, color = SoftBlue)
                        }
                    }

                    if (hasEmail) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                onSendEmailBackup(userProfile?.email.orEmpty())
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = SoftBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отправить копию на e-mail", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Export CSV button
                    OutlinedButton(
                        onClick = {
                            val csv = onExportCsv()
                            shareText(context, csv, "VoltLedger_export.csv")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = strings.exportCsv, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Manual JSON import button
                    OutlinedButton(
                        onClick = { showImportJsonDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Вставить текст JSON вручную", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Phone transfer advice link
                    TextButton(
                        onClick = { showPhoneTransferAdviceDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp), tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Рекомендации при смене телефона", fontSize = 12.sp, color = ElectricCyan)
                    }
                }
            }

            // Section 7: App Version & About
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VoltLedger v2.2.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EV Smart Charging & Analytics",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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
        var newPassportConsumption by remember { mutableStateOf("16.0") }

        AlertDialog(
            onDismissRequest = { showAddCarDialog = false },
            title = { Text(strings.addCar) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text(strings.carName) }, singleLine = true)
                    OutlinedTextField(value = newCapacity, onValueChange = { newCapacity = it }, label = { Text(strings.declaredCapacity) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = newPassportConsumption, onValueChange = { newPassportConsumption = it }, label = { Text("Паспортный расход (кВт·ч/100 км)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(value = newOdo, onValueChange = { newOdo = it }, label = { Text(strings.initialOdometer) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = newSoc, onValueChange = { newSoc = it }, label = { Text(strings.currentSoc) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val cap = newCapacity.toDoubleOrNull() ?: 60.0
                    val odo = newOdo.toDoubleOrNull() ?: 0.0
                    val soc = newSoc.toDoubleOrNull() ?: 50.0
                    val passport = newPassportConsumption.toDoubleOrNull() ?: 16.0
                    if (newName.isNotBlank()) {
                        onAddCar(newName.trim(), cap, odo, soc, passport)
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
        var opType by remember { mutableStateOf(target?.type ?: "AC") }
        var opSubType by remember { mutableStateOf(target?.subType ?: "") }
        var opPrice by remember {
            mutableStateOf(
                if (target != null) {
                    if (target.type.equals("DC", true)) target.priceDc.toString() else target.priceAc.toString()
                } else "0.36"
            )
        }
        var opNightPrice by remember {
            mutableStateOf(
                if (target != null) {
                    (if (target.type.equals("DC", true)) target.nightPriceDc else target.nightPriceAc)?.toString() ?: ""
                } else ""
            )
        }
        var opNightStart by remember { mutableStateOf(target?.nightStartHour?.toString() ?: "23") }
        var opNightEnd by remember { mutableStateOf(target?.nightEndHour?.toString() ?: "6") }
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
                    OutlinedTextField(value = opName, onValueChange = { opName = it }, label = { Text("Название (напр. Malanka, Дом)") }, singleLine = true)
                    
                    // Type selector AC / DC
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { opType = "AC" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (opType == "AC") SoftBlue.copy(alpha = 0.25f) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (opType == "AC") SoftBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("AC (Медленная)", fontSize = 12.sp, fontWeight = if (opType == "AC") FontWeight.Bold else FontWeight.Normal)
                        }
                        OutlinedButton(
                            onClick = { opType = "DC" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (opType == "DC") ElectricCyan.copy(alpha = 0.25f) else Color.Transparent
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (opType == "DC") ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        ) {
                            Text("DC (Быстрая)", fontSize = 12.sp, fontWeight = if (opType == "DC") FontWeight.Bold else FontWeight.Normal)
                        }
                    }

                    OutlinedTextField(value = opSubType, onValueChange = { opSubType = it }, label = { Text("Подтип / Локация (напр. Дом, Дача, Город)") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opPrice, onValueChange = { opPrice = it }, label = { Text("Тариф день ($currency)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = opNightPrice, onValueChange = { opNightPrice = it }, label = { Text("Тариф ночь ($currency)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opNightStart, onValueChange = { opNightStart = it }, label = { Text("Ночь с (ч)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                        OutlinedTextField(value = opNightEnd, onValueChange = { opNightEnd = it }, label = { Text("Ночь по (ч)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = opPenaltyPerMin, onValueChange = { opPenaltyPerMin = it }, label = { Text("Штраф/мин (опц.)") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                        OutlinedTextField(value = opPenaltyFreeMin, onValueChange = { opPenaltyFreeMin = it }, label = { Text("Бесплатно мин") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                    OutlinedTextField(value = opComment, onValueChange = { opComment = it }, label = { Text("Комментарий") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = opPrice.toDoubleOrNull() ?: 0.50
                    val nightP = opNightPrice.toDoubleOrNull()
                    val nStart = opNightStart.toIntOrNull() ?: 23
                    val nEnd = opNightEnd.toIntOrNull() ?: 6
                    val pPen = opPenaltyPerMin.toDoubleOrNull() ?: 0.0
                    val freeM = opPenaltyFreeMin.toIntOrNull() ?: 0
                    if (opName.isNotBlank()) {
                        if (isEditing && target != null) {
                            onUpdateOperator(
                                target.copy(
                                    name = opName.trim(),
                                    subType = opSubType.trim(),
                                    type = opType,
                                    priceAc = if (opType == "AC") p else target.priceAc,
                                    priceDc = if (opType == "DC") p else target.priceDc,
                                    nightPriceAc = if (opType == "AC") nightP else target.nightPriceAc,
                                    nightPriceDc = if (opType == "DC") nightP else target.nightPriceDc,
                                    nightStartHour = nStart,
                                    nightEndHour = nEnd,
                                    penaltyIdlePerMin = pPen,
                                    penaltyFreeMinutes = freeM,
                                    comment = opComment
                                )
                            )
                        } else {
                            onAddOperator(
                                Operator(
                                    name = opName.trim(),
                                    subType = opSubType.trim(),
                                    type = opType,
                                    priceAc = if (opType == "AC") p else 0.55,
                                    priceDc = if (opType == "DC") p else 0.73,
                                    nightPriceAc = if (opType == "AC") nightP else null,
                                    nightPriceDc = if (opType == "DC") nightP else null,
                                    nightStartHour = nStart,
                                    nightEndHour = nEnd,
                                    penaltyIdlePerMin = pPen,
                                    penaltyFreeMinutes = freeM,
                                    comment = opComment,
                                    isBuiltin = false
                                )
                            )
                        }
                        showAddOperatorDialog = false
                        editingOperator = null
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                Row {
                    if (isEditing && target != null) {
                        TextButton(
                            onClick = {
                                val toDelete = target
                                showAddOperatorDialog = false
                                editingOperator = null
                                operatorToDelete = toDelete
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                    TextButton(onClick = {
                        showAddOperatorDialog = false
                        editingOperator = null
                    }) { Text(strings.cancel) }
                }
            }
        )
    }

    // Dialog: Confirm Delete Operator
    if (operatorToDelete != null) {
        val op = operatorToDelete!!
        AlertDialog(
            onDismissRequest = { operatorToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить оператора?") },
            text = {
                Text("Оператор «${op.name}» будет удалён из списка. Существующие завершённые зарядки сохранят свои сохранённые данные.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOperator(op)
                        operatorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { operatorToDelete = null }) {
                    Text(strings.cancel)
                }
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

    // Dialog: Bind Email
    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Привязка E-mail") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Укажите адрес электронной почты для отправки резервных копий, инструкций и быстрого восстановления аккаунта на других устройствах.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Электронная почта") },
                        placeholder = { Text("pilot@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clean = emailInput.trim()
                    if (clean.contains("@") && clean.contains(".")) {
                        onUpdateUserEmail(clean)
                        showEmailDialog = false
                        Toast.makeText(context, "E-mail успешно сохранён", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Введите корректный E-mail", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) { Text(strings.cancel) }
            }
        )
    }



    // Dialog: Phone Transfer Recommendations
    if (showPhoneTransferAdviceDialog) {
        AlertDialog(
            onDismissRequest = { showPhoneTransferAdviceDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Перенос на новый телефон", fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "VoltLedger хранит все данные локально на вашем устройстве, обеспечивая максимальную скорость и конфиденциальность. Для быстрого переноса на новый телефон:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Step 1 Card: Google Drive
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.1f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("1. Резервная копия на Google Диск (Рекомендуется)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ElectricCyan)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "В разделе «Google Диск и Резервные копии» нажмите «На Google Диск» или «Поделиться файлом» -> выберите Google Диск. Файл с полной базой ваших авто и зарядок сохранится в вашем личном облаке.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Step 2 Card: Restore
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BatteryGreen.copy(alpha = 0.1f))
                            .border(1.dp, BatteryGreen.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("2. Восстановление на новом телефоне", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BatteryGreen)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Установите VoltLedger на новый телефон, зайдите в Настройки -> «С Google Диска» и выберите ранее сохранённый файл бэкапа. Все автомобили, зарядки, расходы и настройки мгновенно восстановятся.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Step 3 Card: Alternative methods
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(10.dp)
                    ) {
                        Column {
                            Text("3. Дополнительный способ (E-mail)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftBlue)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Вы также можете отправить резервную копию базы себе на привязанную почту в 1 клик с прикреплённым JSON-файлом.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPhoneTransferAdviceDialog = false }) {
                    Text("Понятно")
                }
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

fun shareBackupFile(context: Context, json: String, fileName: String) {
    try {
        val cacheDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val file = File(cacheDir, fileName)
        file.writeText(json, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Сохранить на Google Диск / Поделиться"))
    } catch (e: Exception) {
        shareText(context, json, fileName)
    }
}
