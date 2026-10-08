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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AppSettings
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.util.EVCalculator
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
    onUpdateCar: (Car) -> Unit = {},
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
    onExportCsv: () -> String,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit = {},
    onImportBackup: ((content: String, replace: Boolean, onResult: (com.example.data.repository.VoltRepository.RestoreResult) -> Unit) -> Unit)? = null,
    onParsePreview: ((content: String) -> com.example.util.BackupData?)? = null,
    onReassignAllSessionsToActiveCar: (((Int) -> Unit) -> Unit)? = null,
    totalSessionsCount: Int = 0,
    activeCarSessionsCount: Int = 0,
    onRefreshTariffs: () -> Unit = {},
    userProfile: UserProfile? = null,
    onOpenProfile: (() -> Unit)? = null,
    onUpdateUserEmail: (String) -> Unit = {},
    onSendEmailBackup: (String) -> Unit = {},
    onSaveHomeChargingSettings: ((
        standardPrice: Double,
        nightTariffEnabled: Boolean,
        nightPrice: Double,
        nightStartHour: Int,
        nightEndHour: Int,
        threeTariffEnabled: Boolean,
        peakPrice: Double,
        peakStartHour: Int,
        peakEndHour: Int,
        semiPeakPrice: Double,
        semiPeakStartHour: Int,
        semiPeakEndHour: Int
    ) -> Unit)? = null,
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
    var pendingBackupContent by remember { mutableStateOf<String?>(null) }
    var pendingBackupData by remember { mutableStateOf<com.example.util.BackupData?>(null) }
    var restoreReplaceMode by remember { mutableStateOf(true) }
    var restoreResultMessage by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember(userProfile?.email) { mutableStateOf(userProfile?.email ?: "") }
    var showPhoneTransferAdviceDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

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
                    val preview = onParsePreview?.invoke(content) ?: com.example.util.CsvJsonBackupHelper.parseAnyBackup(content)
                    if (preview != null && (preview.sessions.isNotEmpty() || preview.cars.isNotEmpty() || preview.expenses.isNotEmpty())) {
                        pendingBackupContent = content
                        pendingBackupData = preview
                        restoreReplaceMode = true
                    } else {
                        restoreResultMessage = "Не удалось распознать данные в выбранном файле. Убедитесь, что это файл резервной копии VoltLedger (JSON или CSV)."
                    }
                } else {
                    Toast.makeText(context, "Выбранный файл пуст", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                restoreResultMessage = "Ошибка чтения файла: ${e.message}"
            }
        }
    }

    LiquidGlassBackground(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            val tabs = listOf(
                Triple(0, strings.settingsTabGarage, "🚗"),
                Triple(1, strings.settingsTabTariffs, "⚡"),
                Triple(2, strings.settingsTabSystem, "⚙️"),
                Triple(3, strings.settingsTabData, "💾")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (index, title, iconStr) ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) ElectricCyan.copy(alpha = 0.22f) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isSelected) ElectricCyan.copy(alpha = 0.6f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = iconStr,
                                fontSize = 17.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            var selectedCarForEditingId by remember(activeCar?.id, allCars) {
                mutableStateOf(activeCar?.id ?: allCars.firstOrNull()?.id)
            }
            val carBeingEdited = allCars.firstOrNull { it.id == selectedCarForEditingId } ?: activeCar ?: allCars.firstOrNull()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Vehicles (Tab 0: Garage)
                if (selectedTabIndex == 0) {
                    if (allCars.isEmpty()) {
                        item {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(ElectricCyan.copy(alpha = 0.15f))
                                            .border(1.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.DirectionsCar,
                                            contentDescription = null,
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Гараж пуст",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Добавьте ваш электромобиль, чтобы вести учет зарядок, расходов и запаса хода.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Button(
                                        onClick = { showAddCarDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Добавить электромобиль", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            VoltCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = strings.carsSection,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Нажмите на авто для выбора и редактирования",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { showAddCarDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = strings.addCar, tint = SoftBlue)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                allCars.forEach { car ->
                                    val isSelectedForEdit = car.id == carBeingEdited?.id
                                    val isActive = car.id == activeCar?.id
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelectedForEdit) ElectricCyan.copy(alpha = 0.15f)
                                                else if (isActive) SoftBlue.copy(alpha = 0.1f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelectedForEdit) ElectricCyan.copy(alpha = 0.5f) else Color.Transparent,
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable {
                                                selectedCarForEditingId = car.id
                                                onSelectCar(car.id)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isActive) ElectricCyan.copy(alpha = 0.2f) else SoftBlue.copy(alpha = 0.15f)),
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
                                                        tint = if (isActive) ElectricCyan else SoftBlue,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = car.name,
                                                        fontWeight = if (isActive || isSelectedForEdit) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 15.sp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isActive) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(6.dp))
                                                                .background(BatteryGreen.copy(alpha = 0.2f))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "Основной",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = BatteryGreen
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${car.declaredCapacityKwh.toInt()} кВт·ч • ${car.initialOdometer.toInt()} км • Заряд: ${car.currentSoc.toInt()}%",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteCar(car) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }

                        // Detailed Edit Form for Selected Car
                        if (carBeingEdited != null) {
                            item(key = "edit_car_${carBeingEdited.id}") {
                                var editName by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.name) }
                                var editDeclaredCap by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.declaredCapacityKwh.toString()) }
                                var editUsableCap by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.usableCapacityKwh.toString()) }
                                var editOdometer by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.initialOdometer.toInt().toString()) }
                                var editSoc by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.currentSoc.toInt().toString()) }
                                var editConsumption by remember(carBeingEdited.id) { mutableStateOf(carBeingEdited.passportConsumption.toString()) }

                                VoltCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = null,
                                                tint = ElectricCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Параметры авто",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        if (carBeingEdited.id != activeCar?.id) {
                                            TextButton(
                                                onClick = { onSelectCar(carBeingEdited.id) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text("Сделать основным", fontSize = 12.sp, color = ElectricCyan)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Car name
                                    OutlinedTextField(
                                        value = editName,
                                        onValueChange = { editName = it },
                                        label = { Text("Марка и модель авто") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Battery row: Declared & Usable
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editDeclaredCap,
                                            onValueChange = {
                                                editDeclaredCap = it
                                                val declared = it.toDoubleOrNull()
                                                if (declared != null && declared > 0) {
                                                    editUsableCap = String.format(Locale.US, "%.1f", EVCalculator.calculateUsableCapacity(declared))
                                                }
                                            },
                                            label = { Text("Номинал (кВт·ч)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editUsableCap,
                                            onValueChange = { editUsableCap = it },
                                            label = { Text("Полезная (кВт·ч)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Odometer & Consumption row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = editOdometer,
                                            onValueChange = { editOdometer = it },
                                            label = { Text("Пробег (км)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = editConsumption,
                                            onValueChange = { editConsumption = it },
                                            label = { Text("Расход (кВт·ч/100)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // SOC Slider & input
                                    val currentSocFloat = editSoc.toFloatOrNull()?.coerceIn(0f, 100f) ?: 50f
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Текущий уровень заряда",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${currentSocFloat.toInt()}%",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ElectricCyan
                                            )
                                        }

                                        Slider(
                                            value = currentSocFloat,
                                            onValueChange = { editSoc = it.toInt().toString() },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(
                                                thumbColor = ElectricCyan,
                                                activeTrackColor = ElectricCyan,
                                                inactiveTrackColor = ElectricCyan.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Presets
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(20, 50, 80, 100).forEach { pct ->
                                                val isPresSelected = currentSocFloat.toInt() == pct
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isPresSelected) ElectricCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface)
                                                        .border(1.dp, if (isPresSelected) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                        .clickable { editSoc = pct.toString() }
                                                        .padding(vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "$pct%",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isPresSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isPresSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Save Button
                                    Button(
                                        onClick = {
                                            val declared = editDeclaredCap.toDoubleOrNull() ?: carBeingEdited.declaredCapacityKwh
                                            val usable = editUsableCap.toDoubleOrNull() ?: EVCalculator.calculateUsableCapacity(declared)
                                            val odo = editOdometer.toDoubleOrNull() ?: carBeingEdited.initialOdometer
                                            val soc = (editSoc.toDoubleOrNull() ?: carBeingEdited.currentSoc).coerceIn(0.0, 100.0)
                                            val cons = editConsumption.toDoubleOrNull() ?: carBeingEdited.passportConsumption

                                            val updated = carBeingEdited.copy(
                                                name = editName.trim().ifEmpty { carBeingEdited.name },
                                                declaredCapacityKwh = declared,
                                                usableCapacityKwh = usable,
                                                initialOdometer = odo,
                                                currentSoc = soc,
                                                passportConsumption = cons
                                            )
                                            onUpdateCar(updated)
                                            Toast.makeText(context, "Параметры ${updated.name} сохранены!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Сохранить параметры авто", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

        // Tab 1: Tariffs (Section 2: AC, Section 3: DC, Section 4.5: Home Charging)
        if (selectedTabIndex == 1) {
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

            // Section 4.5: Home Charging Tariffs & Schedule (Домашняя зарядка)
            item {
                var standardPriceText by remember(settings.homeStandardPrice) {
                    mutableStateOf(String.format(Locale.US, "%.4f", settings.homeStandardPrice).trimEnd('0').trimEnd('.'))
                }
                var nightTariffEnabled by remember(settings.homeNightTariffEnabled) {
                    mutableStateOf(settings.homeNightTariffEnabled)
                }
                var nightPriceText by remember(settings.homeNightPrice) {
                    mutableStateOf(String.format(Locale.US, "%.4f", settings.homeNightPrice).trimEnd('0').trimEnd('.'))
                }
                var threeTariffEnabled by remember(settings.homeThreeTariffEnabled) {
                    mutableStateOf(settings.homeThreeTariffEnabled)
                }
                var peakPriceText by remember(settings.homePeakPrice) {
                    mutableStateOf(String.format(Locale.US, "%.4f", settings.homePeakPrice).trimEnd('0').trimEnd('.'))
                }
                var semiPeakPriceText by remember(settings.homeSemiPeakPrice) {
                    mutableStateOf(String.format(Locale.US, "%.4f", settings.homeSemiPeakPrice).trimEnd('0').trimEnd('.'))
                }

                fun saveHomeSettings(three: Boolean = threeTariffEnabled, night: Boolean = nightTariffEnabled) {
                    val std = standardPriceText.toDoubleOrNull() ?: 0.36
                    val nPrice = nightPriceText.toDoubleOrNull() ?: 0.1822
                    val pPrice = peakPriceText.toDoubleOrNull() ?: 0.5467
                    val sPrice = semiPeakPriceText.toDoubleOrNull() ?: 0.2126
                    onSaveHomeChargingSettings?.invoke(
                        std,
                        night,
                        nPrice,
                        settings.homeNightStartHour,
                        settings.homeNightEndHour,
                        three,
                        pPrice,
                        settings.homePeakStartHour,
                        settings.homePeakEndHour,
                        sPrice,
                        settings.homeSemiPeakStartHour,
                        settings.homeSemiPeakEndHour
                    )
                }

                VoltCard(modifier = Modifier.fillMaxWidth().testTag("home_charging_settings_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Тарифы домашней зарядки",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Настройте расчёт стоимости зарядки от домашней сети",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = BatteryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Standard 1-rate price input
                    OutlinedTextField(
                        value = standardPriceText,
                        onValueChange = {
                            standardPriceText = it
                            saveHomeSettings()
                        },
                        label = { Text("Стандартная цена ($currency / кВт·ч)") },
                        placeholder = { Text("0.36") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("home_standard_price_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode 1: Day / Night 2-rate toggle
                    if (!threeTariffEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Двухтарифный учёт (день / ночь)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ночной тариф: 23:00 - 06:00",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = nightTariffEnabled,
                                onCheckedChange = {
                                    nightTariffEnabled = it
                                    saveHomeSettings(three = false, night = it)
                                }
                            )
                        }

                        if (nightTariffEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = nightPriceText,
                                onValueChange = {
                                    nightPriceText = it
                                    saveHomeSettings()
                                },
                                label = { Text("Ночной тариф 23:00-06:00 ($currency / кВт·ч)") },
                                placeholder = { Text("0.1822") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mode 2: Three-tariff toggle (Пик, Полупик, Ночь)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Трёхзонный тариф (дифференцированный)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Пик (17-23), Ночь (23-06), Полупик (06-17)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = threeTariffEnabled,
                            onCheckedChange = {
                                threeTariffEnabled = it
                                saveHomeSettings(three = it, night = if (it) false else nightTariffEnabled)
                            },
                            modifier = Modifier.testTag("three_tariff_switch")
                        )
                    }

                    if (threeTariffEnabled) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Peak rate input
                        OutlinedTextField(
                            value = peakPriceText,
                            onValueChange = {
                                peakPriceText = it
                                saveHomeSettings()
                            },
                            label = { Text("Пиковый: 17:00 - 23:00 ($currency / кВт·ч)") },
                            placeholder = { Text("0.5467") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Night rate input
                        OutlinedTextField(
                            value = nightPriceText,
                            onValueChange = {
                                nightPriceText = it
                                saveHomeSettings()
                            },
                            label = { Text("Ночной (миним.): 23:00 - 06:00 ($currency / кВт·ч)") },
                            placeholder = { Text("0.1822") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Semi-peak rate input
                        OutlinedTextField(
                            value = semiPeakPriceText,
                            onValueChange = {
                                semiPeakPriceText = it
                                saveHomeSettings()
                            },
                            label = { Text("Полупиковый: 06:00 - 17:00 ($currency / кВт·ч)") },
                            placeholder = { Text("0.2126") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "💡 Время определяется автоматически по системным часам устройства при начале или завершении зарядки.",
                        fontSize = 11.sp,
                        color = ElectricCyan,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Tab 2: System (Section 4: Currency, Language & Theme, Section 5: Smart Notifications)
        if (selectedTabIndex == 2) {
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
                    Text(text = "Основная валюта отчётов и сводок", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "В этой валюте отображается общая статистика и средние показатели. Каждая запись (зарядка/расход) сохраняет свою реальную валюту чека (BYN, PLN и др.).",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("BYN", "RUB", "PLN", "USD", "EUR", "KZT").forEach { curr ->
                            FilterChip(
                                selected = settings.currency == curr,
                                onClick = { onUpdateCurrency(curr) },
                                label = { Text(curr) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricCyan,
                                    selectedLabelColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Multi-currency live rates reference card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ElectricCyan.copy(alpha = 0.08f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💱 ", fontSize = 13.sp)
                                Text(
                                    text = "Справедливый мультивалютный пересчёт",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "При поездке за границу (например, в Польшу за PLN) VoltLedger не приравнивает 1 PLN к 1 BYN. В чеке сохраняется PLN, а в статистике сумма пересчитывается по справедливому кросс-курсу:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• 1 PLN ≈ 0.82 BYN (1 BYN ≈ 1.22 PLN)\n• 1 EUR ≈ 3.60 BYN  • 1 USD ≈ 3.30 BYN\n• 100 RUB ≈ 3.50 BYN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftBlue,
                                lineHeight = 16.sp
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

                    // Theme selector: Light theme, Cyber Neon Dark, and colorful Postcard themes
                    Text(text = strings.theme, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    val themeOptions = listOf(
                        Triple("dark", "⚡ Кибер Неон (Тёмная)", "Фирменная тёмная неоновая тема"),
                        Triple("light", "☀️ Светлая тема", "Чистая и контрастная стеклянная тема"),
                        Triple("eco", "🌿 Эко Изумруд (Визитка)", "Изумрудно-мятные цвета визитки"),
                        Triple("sunset", "🌅 Закатный Драйв (Визитка)", "Кораллово-янтарные тёплые цвета визитки"),
                        Triple("cosmic", "🌌 Космос (Визитка)", "Глубокий индиго и небесный лазурный")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themeOptions.forEach { (key, title, subtitle) ->
                            val isSelected = settings.theme.lowercase().trim().let { current ->
                                if (key == "dark") current == "dark" || current == "cyber_neon" || current == "system" || current.isBlank()
                                else current == key || (key == "eco" && (current == "emerald" || current == "postcard_emerald"))
                                        || (key == "sunset" && (current == "postcard_sunset"))
                                        || (key == "cosmic" && (current == "space" || current == "postcard_space"))
                            }

                            val themeBorderColor = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            val themeBgColor = if (isSelected) ElectricCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(themeBgColor)
                                    .border(if (isSelected) 1.5.dp else 1.dp, themeBorderColor, RoundedCornerShape(14.dp))
                                    .clickable { onUpdateTheme(key) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val iconChar = when (key) {
                                                "light" -> "☀️"
                                                "eco" -> "🌿"
                                                "sunset" -> "🌅"
                                                "cosmic" -> "🌌"
                                                else -> "⚡"
                                            }
                                            Text(iconChar, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = subtitle,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Выбрано",
                                            tint = ElectricCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
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
        }

        // Tab 3: Data & About (Section 6: Backup, Section 7: About Developer, Section 8: Footer)
        if (selectedTabIndex == 3) {
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
                        Text("Вставить текст JSON / CSV вручную", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Diagnostic and History Repair card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = SoftBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Диагностика истории зарядок", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Текущий авто: ${activeCar?.name ?: "Не выбран"}\nЗарядок для этого авто: $activeCarSessionsCount (всего в базе: $totalSessionsCount)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            if (totalSessionsCount > activeCarSessionsCount) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SoftBlue.copy(alpha = 0.15f))
                                        .border(1.dp, SoftBlue.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "⚠️ В базе найдено ${totalSessionsCount - activeCarSessionsCount} зарядок от другого авто или бэкапа, которые не отображаются для текущего авто!",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                onReassignAllSessionsToActiveCar?.invoke { count ->
                                                    Toast.makeText(context, "Все $count зарядок привязаны к ${activeCar?.name}!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("⚡ Привязать все $totalSessionsCount зарядок к ${activeCar?.name}", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else if (totalSessionsCount > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onReassignAllSessionsToActiveCar?.invoke { count ->
                                            Toast.makeText(context, "Проверка завершена. Все $count зарядок привязаны к ${activeCar?.name}!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = SoftBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Обновить привязку истории", fontSize = 11.sp)
                                }
                            }
                        }
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

            // Section 7: About Developer (Gunmetal & EV Community)
            item {
                var showAboutDeveloperDialog by remember { mutableStateOf(false) }

                VoltCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAboutDeveloperDialog = true }
                        .testTag("about_developer_card")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.15f))
                                .border(1.5.dp, ElectricCyan.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⚡", fontSize = 22.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = strings.aboutDeveloper,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ElectricCyan.copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                 ) {
                                     Text(
                                         text = com.example.util.APP_VERSION_NAME,
                                         fontSize = 10.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = ElectricCyan
                                     )
                                 }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.developerCreatedForEv,
                                fontSize = 12.sp,
                                color = ElectricCyan,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Warm community message box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🤝 ", fontSize = 14.sp)
                                Text(
                                    text = "Спасибо за установку!",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BatteryGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "VoltLedger создан для EV общества разработчиком Gunmetal. Приложение создано с заботой для каждого владельца электромобиля — без рекламы, без сбора данных, с прозрачными расчётами и поддержкой сообщества.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Badges row
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricCyan.copy(alpha = 0.12f))
                                .border(0.8.dp, ElectricCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🛠️ Автор: Gunmetal", fontSize = 11.sp, color = ElectricCyan, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SoftBlue.copy(alpha = 0.12f))
                                .border(0.8.dp, SoftBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🚗 EV Community Edition", fontSize = 11.sp, color = SoftBlue, fontWeight = FontWeight.SemiBold)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BatteryGreen.copy(alpha = 0.12f))
                                .border(0.8.dp, BatteryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("🛡️ 100% Offline & Private", fontSize = 11.sp, color = BatteryGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Modal dialog if user taps card
                if (showAboutDeveloperDialog) {
                    AlertDialog(
                        onDismissRequest = { showAboutDeveloperDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("О разработчике", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "VoltLedger ${com.example.util.APP_VERSION_NAME}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Приложение создано для сообщества владельцев электромобилей (EV) разработчиком Gunmetal.\n\nСпасибо за установку и доверие! Цель проекта — дать каждому владельцу EV полный контроль над расходами, реальным запасом хода и эффективностью зарядок.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ElectricCyan.copy(alpha = 0.08f))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = "💚 Приятных и выгодных поездок на электротяге!\nС уважением, Gunmetal.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://t.me/VoltLeger")
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF229ED9))
                                ) {
                                    Text("📢 Telegram-канал проекта: @VoltLeger", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showAboutDeveloperDialog = false }) {
                                Text("Отлично")
                            }
                        }
                    )
                }
            }

            // Section 8: App Version & About Footer
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "VoltLedger ${com.example.util.APP_VERSION_NAME}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "EV Smart Charging & Analytics • Gunmetal Edition",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://t.me/VoltLeger")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .background(Color(0xFF229ED9).copy(alpha = 0.12f))
                            .border(0.8.dp, Color(0xFF229ED9).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📢 Telegram-канал: @VoltLeger", fontSize = 12.sp, color = Color(0xFF229ED9), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
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
                    Text("Вставьте JSON или текст CSV резервной копии:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("{ ... } или id,car_id,...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonText.isNotBlank()) {
                        val preview = onParsePreview?.invoke(importJsonText) ?: com.example.util.CsvJsonBackupHelper.parseAnyBackup(importJsonText)
                        if (preview != null && (preview.sessions.isNotEmpty() || preview.cars.isNotEmpty() || preview.expenses.isNotEmpty())) {
                            pendingBackupContent = importJsonText
                            pendingBackupData = preview
                            restoreReplaceMode = true
                            showImportJsonDialog = false
                        } else {
                            restoreResultMessage = "Не удалось распознать формат данных в тексте. Убедитесь, что вставлен корректный JSON или CSV бэкап."
                        }
                    }
                }) { Text("Проверить и восстановить") }
            },
            dismissButton = {
                TextButton(onClick = { showImportJsonDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Dialog: Confirm & Preview Backup Restore
    if (pendingBackupData != null) {
        val data = pendingBackupData!!
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) {
                    pendingBackupData = null
                    pendingBackupContent = null
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Восстановление данных")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "В резервной копии найдено:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ElectricCyan.copy(alpha = 0.08f))
                            .border(1.dp, ElectricCyan.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("⚡ Зарядок: ${data.sessions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ElectricCyan)
                        if (data.cars.isNotEmpty()) {
                            Text("🚗 Автомобилей: ${data.cars.size} (${data.cars.joinToString { it.name }})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        } else {
                            Text("🚗 Привязка: к текущему авто (${activeCar?.name ?: "Мой Электромобиль"})", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (data.expenses.isNotEmpty()) {
                            Text("💰 Трат: ${data.expenses.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (data.operators.isNotEmpty()) {
                            Text("🏢 Операторов/тарифов: ${data.operators.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Text(
                        text = "Выберите режим восстановления:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Mode 1: Replace (Clean restore)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { restoreReplaceMode = true }
                            .background(if (restoreReplaceMode) ElectricCyan.copy(alpha = 0.12f) else Color.Transparent)
                            .border(1.dp, if (restoreReplaceMode) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = restoreReplaceMode,
                            onClick = { restoreReplaceMode = true },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Заменить все данные (Чистый бэкап)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Рекомендуется при переносе на новый телефон или переустановке.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Mode 2: Merge (Append)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { restoreReplaceMode = false }
                            .background(if (!restoreReplaceMode) ElectricCyan.copy(alpha = 0.12f) else Color.Transparent)
                            .border(1.dp, if (!restoreReplaceMode) ElectricCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !restoreReplaceMode,
                            onClick = { restoreReplaceMode = false },
                            colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Объединить с текущими", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Добавит историю к уже имеющимся данным без удаления.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val text = pendingBackupContent ?: return@Button
                        val isReplace = restoreReplaceMode
                        isRestoring = true
                        if (onImportBackup != null) {
                            onImportBackup.invoke(text, isReplace) { result ->
                                isRestoring = false
                                pendingBackupData = null
                                pendingBackupContent = null
                                restoreResultMessage = if (result.success) {
                                    "✅ ${result.message}\n\nИстория и данные успешно восстановлены и отображаются в приложении!"
                                } else {
                                    "❌ Ошибка восстановления: ${result.message}"
                                }
                            }
                        } else {
                            onImportJson(text)
                            isRestoring = false
                            pendingBackupData = null
                            pendingBackupContent = null
                            restoreResultMessage = "Резервная копия отправлена на импорт."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    enabled = !isRestoring
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Восстановление...", color = Color.Black, fontSize = 12.sp)
                    } else {
                        Text("Восстановить", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingBackupData = null
                        pendingBackupContent = null
                    },
                    enabled = !isRestoring
                ) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (restoreResultMessage != null) {
        AlertDialog(
            onDismissRequest = { restoreResultMessage = null },
            title = { Text("Восстановление данных") },
            text = { Text(restoreResultMessage ?: "") },
            confirmButton = {
                Button(onClick = { restoreResultMessage = null }) {
                    Text("OK")
                }
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
