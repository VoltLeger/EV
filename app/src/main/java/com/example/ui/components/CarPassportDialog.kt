package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.SoftBlue
import java.io.File
import java.util.Locale

@Composable
fun CarPassportDialog(
    car: Car,
    sessions: List<ChargingSession>,
    currency: String,
    onDismiss: () -> Unit,
    onSaveCar: (Car) -> Unit,
    onSellCar: (Car) -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(false) }
    var showSellConfirmDialog by remember { mutableStateOf(false) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val photosDir = File(context.filesDir, "car_photos").apply { mkdirs() }
                val targetFile = File(photosDir, "car_${car.id}_avatar.jpg")
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    targetFile.outputStream().use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                val updatedCar = car.copy(photoUri = targetFile.absolutePath)
                onSaveCar(updatedCar)
                Toast.makeText(context, "Фото автомобиля сохранено", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Fallback to direct string URI
                val updatedCar = car.copy(photoUri = uri.toString())
                onSaveCar(updatedCar)
            }
        }
    }

    // Editable state fields
    var editName by remember(car) { mutableStateOf(car.name) }
    var editYear by remember(car) { mutableStateOf(car.manufactureYear?.toString() ?: "") }
    var editLicensePlate by remember(car) { mutableStateOf(car.licensePlate ?: "") }
    var editVin by remember(car) { mutableStateOf(car.vin ?: "") }
    var editPurchaseDate by remember(car) { mutableStateOf(car.purchaseDate ?: "") }
    var editPurchasePrice by remember(car) { mutableStateOf(car.purchasePrice?.let { String.format(Locale.US, "%.0f", it) } ?: "") }
    var editInitialOdo by remember(car) { mutableStateOf(car.initialOdometer.toInt().toString()) }
    var editCurrentOdo by remember(car) {
        val maxSessionOdo = sessions.filter { it.carId == car.id }.maxOfOrNull { it.startOdometer } ?: car.initialOdometer
        mutableStateOf(maxSessionOdo.toInt().toString())
    }
    var editDeclaredCapacity by remember(car) { mutableStateOf(car.declaredCapacityKwh.toString()) }
    var editRegistrationNumber by remember(car) { mutableStateOf(car.registrationNumber ?: "") }
    var editInsuranceNumber by remember(car) { mutableStateOf(car.insuranceNumber ?: "") }

    // Ownership stats for "Sell Car" report
    val carSessions = sessions.filter { it.carId == car.id && it.status == "completed" }
    val totalEnergyCharged = carSessions.sumOf { it.kwhDeliveredByStation }
    val totalChargingCost = carSessions.sumOf { it.totalCost }
    val maxOdo = carSessions.maxOfOrNull { it.startOdometer } ?: car.initialOdometer
    val totalDistanceDriven = (maxOdo - car.initialOdometer).coerceAtLeast(0.0)
    val avgCostPerKm = if (totalDistanceDriven > 0) totalChargingCost / totalDistanceDriven else 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditing) "Редактирование паспорта" else "Паспорт авто",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { isEditing = !isEditing },
                    modifier = Modifier.testTag("car_passport_edit_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = if (isEditing) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant
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
                // Top Car Photo Header (Avatar display & upload)
                val photoShape = RoundedCornerShape(20.dp)
                if (!car.photoUri.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(photoShape)
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(ElectricCyan, SoftBlue.copy(alpha = 0.5f), Color.White.copy(alpha = 0.3f))
                                ),
                                shape = photoShape
                            )
                    ) {
                        AsyncImage(
                            model = car.photoUri,
                            contentDescription = "Фото ${car.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Bottom Overlay Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xFF0F172A).copy(alpha = 0.9f))
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = ElectricCyan)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Сменить фото", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        File(car.photoUri).delete()
                                    } catch (_: Exception) {}
                                    val updatedCar = car.copy(photoUri = null)
                                    onSaveCar(updatedCar)
                                    Toast.makeText(context, "Фото удалено", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Удалить", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Upload Prompt Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(photoShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                            .border(
                                width = 1.2.dp,
                                brush = Brush.linearGradient(
                                    listOf(ElectricCyan.copy(alpha = 0.7f), SoftBlue.copy(alpha = 0.4f))
                                ),
                                shape = photoShape
                            )
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(14.dp)
                            .testTag("upload_car_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Загрузить фото",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Загрузить фото машины",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Будет аватаром пилота и паспорта",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isEditing) {
                    // Edit Mode Fields
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Марка и модель") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editYear,
                            onValueChange = { editYear = it },
                            label = { Text("Год выпуска") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editDeclaredCapacity,
                            onValueChange = { editDeclaredCapacity = it },
                            label = { Text("Батарея (кВт·ч)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editLicensePlate,
                            onValueChange = { editLicensePlate = it },
                            label = { Text("Гос. номер") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editVin,
                            onValueChange = { editVin = it },
                            label = { Text("VIN") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editPurchaseDate,
                            onValueChange = { editPurchaseDate = it },
                            label = { Text("Дата покупки") },
                            placeholder = { Text("ГГГГ-ММ-ДД") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editPurchasePrice,
                            onValueChange = { editPurchasePrice = it },
                            label = { Text("Цена покупки ($currency)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editInitialOdo,
                            onValueChange = { editInitialOdo = it },
                            label = { Text("Начальный одометр") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editCurrentOdo,
                            onValueChange = { editCurrentOdo = it },
                            label = { Text("Текущий одометр") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editRegistrationNumber,
                        onValueChange = { editRegistrationNumber = it },
                        label = { Text("Свидетельство о регистрации (СТС)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editInsuranceNumber,
                        onValueChange = { editInsuranceNumber = it },
                        label = { Text("Номер страховки") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // View Mode: Structured passport cards with strictly aligned data rows

                    // 1. Vehicle Identity Header Card
                    VoltCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = ElectricCyan.copy(alpha = 0.4f),
                        cornerRadius = 20.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                    Text(
                                        text = car.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (car.manufactureYear != null) {
                                        Text(
                                            text = "Год выпуска: ${car.manufactureYear}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!car.licensePlate.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White)
                                            .border(1.5.dp, Color.Black, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = car.licensePlate,
                                            color = Color.Black,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            PassportDataRow(
                                label = "Ёмкость батареи:",
                                value = "${car.declaredCapacityKwh.toInt()} кВт·ч (полезная ${String.format(Locale.US, "%.1f", car.usableCapacityKwh)})",
                                valueColor = SoftBlue
                            )

                            PassportDataRow(
                                label = "Текущий пробег:",
                                value = "${maxOdo.toInt()} км",
                                valueColor = ElectricCyan,
                                valueFontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // 2. Acquisition & Initial Parameters Card
                    VoltCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            text = "Параметры приобретения",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PassportDataRow(
                            label = "Дата покупки:",
                            value = car.purchaseDate.takeIf { !it.isNullOrBlank() } ?: "Не указана"
                        )

                        PassportDataRow(
                            label = "Стоимость покупки:",
                            value = if (car.purchasePrice != null && car.purchasePrice > 0) {
                                "${String.format(Locale.US, "%,.0f", car.purchasePrice)} $currency"
                            } else "Не указана",
                            valueColor = BatteryGreen
                        )

                        PassportDataRow(
                            label = "Начальный пробег:",
                            value = "${car.initialOdometer.toInt()} км"
                        )
                    }

                    // 3. Legal & Documents Card
                    VoltCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            text = "Документы и идентификация",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PassportDataRow(
                            label = "VIN:",
                            value = car.vin.takeIf { !it.isNullOrBlank() } ?: "Не указан"
                        )

                        PassportDataRow(
                            label = "СТС / Регистрация:",
                            value = car.registrationNumber.takeIf { !it.isNullOrBlank() } ?: "Не указан"
                        )

                        PassportDataRow(
                            label = "Страховка:",
                            value = car.insuranceNumber.takeIf { !it.isNullOrBlank() } ?: "Не указана"
                        )
                    }

                    // 4. Lifetime Usage Summary Card
                    VoltCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = BatteryGreen.copy(alpha = 0.35f),
                        cornerRadius = 20.dp
                    ) {
                        Text(
                            text = "Статистика владения",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        PassportDataRow(
                            label = "Пройдено вами:",
                            value = "${totalDistanceDriven.toInt()} км",
                            valueColor = ElectricCyan,
                            valueFontWeight = FontWeight.Bold
                        )

                        PassportDataRow(
                            label = "Всего заряжено:",
                            value = "${String.format(Locale.US, "%.1f", totalEnergyCharged)} кВт·ч (${carSessions.size} зарядок)"
                        )

                        PassportDataRow(
                            label = "Затраты на зарядку:",
                            value = "${String.format(Locale.US, "%.2f", totalChargingCost)} $currency",
                            valueColor = BatteryOrange,
                            valueFontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // "Продать авто" button
                    Button(
                        onClick = { showSellConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sell,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Продать авто (Итоги владения)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(
                    onClick = {
                        val updatedCar = car.copy(
                            name = editName.trim().ifEmpty { car.name },
                            manufactureYear = editYear.toIntOrNull(),
                            licensePlate = editLicensePlate.trim().ifEmpty { null },
                            vin = editVin.trim().ifEmpty { null },
                            purchaseDate = editPurchaseDate.trim().ifEmpty { null },
                            purchasePrice = editPurchasePrice.toDoubleOrNull(),
                            initialOdometer = editInitialOdo.toDoubleOrNull() ?: car.initialOdometer,
                            declaredCapacityKwh = editDeclaredCapacity.toDoubleOrNull() ?: car.declaredCapacityKwh,
                            registrationNumber = editRegistrationNumber.trim().ifEmpty { null },
                            insuranceNumber = editInsuranceNumber.trim().ifEmpty { null }
                        )
                        onSaveCar(updatedCar)
                        isEditing = false
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Сохранить", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Закрыть")
                }
            }
        },
        dismissButton = {
            if (isEditing) {
                TextButton(
                    onClick = { isEditing = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Отмена")
                }
            }
        }
    )

    // Sell Car Report & Confirmation Dialog
    if (showSellConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSellConfirmDialog = false },
            shape = RoundedCornerShape(24.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Sell,
                    contentDescription = null,
                    tint = BatteryOrange,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Итоги владения: ${car.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Сводка перед продажей или архивацией автомобиля:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    VoltCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PassportDataRow("Начальный пробег:", "${car.initialOdometer.toInt()} км")
                            PassportDataRow("Конечный пробег:", "${maxOdo.toInt()} км")
                            PassportDataRow("Пройденная дистанция:", "${totalDistanceDriven.toInt()} км", valueColor = ElectricCyan, valueFontWeight = FontWeight.Bold)
                            PassportDataRow("Всего зарядок:", "${carSessions.size}")
                            PassportDataRow("Всего кВт·ч заряжено:", "${String.format(Locale.US, "%.1f", totalEnergyCharged)} кВт·ч")
                            PassportDataRow("Всего затрат на зарядки:", "${String.format(Locale.US, "%.2f", totalChargingCost)} $currency", valueColor = BatteryGreen, valueFontWeight = FontWeight.Bold)
                            if (totalDistanceDriven > 0) {
                                PassportDataRow("Стоимость 1 км:", "${String.format(Locale.US, "%.3f", avgCostPerKm)} $currency/км", valueFontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Text(
                        text = "Вы хотите архивировать автомобиль? Он останется в истории, но перестанет быть активным.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSellCar(car)
                        showSellConfirmDialog = false
                        onDismiss()
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Подтвердить продажу")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSellConfirmDialog = false },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}

/**
 * Standardized data row that guarantees absolute vertical and horizontal alignment
 * across all cards with zero layout ruptures or line breaks.
 */
@Composable
fun PassportDataRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.SemiBold
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = valueFontWeight,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
