package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.VoltCard
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartChargingScreen(
    activeCar: Car?,
    operators: List<Operator>,
    autoNightTariffEnabled: Boolean,
    onBack: () -> Unit,
    onStartCharging: (
        odometer: Double,
        startSoc: Double,
        stationType: String,
        operator: Operator?,
        customName: String?,
        avgPowerKw: Double?,
        pricePerKwh: Double,
        startTime: Long,
        nightTariffApplied: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    val prevOdometer = activeCar?.initialOdometer ?: 0.0
    val prevSoc = activeCar?.currentSoc ?: 80.0
    val usableCapacity = activeCar?.usableCapacityKwh ?: 57.0
    val avgConsumption = 17.5 // baseline consumption

    // Swap % and Odometer: First SoC, then Odometer
    var socText by remember {
        mutableStateOf(activeCar?.currentSoc?.toInt()?.toString() ?: "25")
    }
    var odometerText by remember {
        mutableStateOf(prevOdometer.toInt().toString())
    }

    // Default flag is DC
    var selectedStationType by remember { mutableStateOf("DC") }
    var selectedOperator by remember { mutableStateOf<Operator?>(null) }
    var operatorMenuExpanded by remember { mutableStateOf(false) }
    var customOperatorName by remember { mutableStateOf("") }
    var avgPowerText by remember { mutableStateOf("") }
    var pricePerKwhText by remember { mutableStateOf("0.55") }
    var nightTariffApplied by remember { mutableStateOf(false) }
    var startTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showTimeAdjustDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Filter out Home charging stations from the public AC/DC list
    val filteredOperators = remember(operators, selectedStationType) {
        operators.filter {
            !it.name.contains("Дом", ignoreCase = true) &&
                    (it.type.equals(selectedStationType, ignoreCase = true) || it.type.equals("both", ignoreCase = true))
        }
    }

    // Auto-calculate estimated odometer based on SoC change and last trip
    fun updateEstimatedOdometer(enteredSoc: Double) {
        val estimatedOdo = EVCalculator.estimateOdometerFromSoc(
            currentSoc = enteredSoc,
            carOdometer = prevOdometer,
            carSoc = prevSoc,
            usableCapacityKwh = usableCapacity,
            avgConsumption = avgConsumption
        )
        odometerText = estimatedOdo.toInt().toString()
    }

    // Initialize or reset selected operator when station type changes
    LaunchedEffect(filteredOperators, selectedStationType) {
        if (filteredOperators.isNotEmpty()) {
            if (selectedOperator == null || !filteredOperators.contains(selectedOperator)) {
                selectedOperator = filteredOperators.first()
            }
        } else {
            selectedOperator = null
        }
    }

    // Update price when operator, station type, or manual tariff selection changes
    fun recalculatePrice(op: Operator?, type: String, applyNight: Boolean) {
        if (op == null) return
        if (op.isFree) {
            pricePerKwhText = "0.00"
            nightTariffApplied = false
            return
        }

        nightTariffApplied = applyNight

        val price = if (type == "DC") {
            if (applyNight && op.nightPriceDc != null) op.nightPriceDc else op.priceDc
        } else {
            if (applyNight && op.nightPriceAc != null) op.nightPriceAc else op.priceAc
        }
        pricePerKwhText = String.format(Locale.US, "%.2f", price)
    }

    LaunchedEffect(selectedOperator, selectedStationType) {
        recalculatePrice(selectedOperator, selectedStationType, applyNight = nightTariffApplied)
    }

    val odoVal = odometerText.toDoubleOrNull() ?: 0.0
    val socVal = socText.toDoubleOrNull() ?: 0.0
    val priceVal = pricePerKwhText.toDoubleOrNull() ?: 0.0
    val avgPowerVal = avgPowerText.toDoubleOrNull()

    // Odometer validation: cannot be less than previous odometer
    val isOdoValid = odoVal >= prevOdometer
    val isSocValid = socVal in 0.0..100.0
    val isFormValid = isOdoValid && isSocValid

    // Price to 100% calculation
    val neededKwhTo100 = (usableCapacity * ((100.0 - socVal).coerceAtLeast(0.0) / 100.0))
    val priceTo100 = neededKwhTo100 * priceVal

    LiquidGlassBackground(modifier = modifier) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.startChargeTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // 1. Swapped Inputs: FIRST SoC %, SECOND Odometer (km)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Input 1: SoC %
                    OutlinedTextField(
                        value = socText,
                        onValueChange = {
                            socText = it
                            val parsed = it.toDoubleOrNull()
                            if (parsed != null && parsed in 0.0..100.0) {
                                updateEstimatedOdometer(parsed)
                            }
                        },
                        label = { Text("Остаток заряда (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("start_soc_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Input 2: Odometer (km) with validation (cannot be less than previous)
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        label = { Text(strings.startOdometer) },
                        isError = !isOdoValid && odometerText.isNotBlank(),
                        supportingText = if (!isOdoValid && odometerText.isNotBlank()) {
                            { Text("Не может быть меньше ${prevOdometer.toInt()} км", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("start_odometer_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Station Type Selector (DC is default)
                Text(
                    text = strings.stationType,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedStationType == "DC",
                        onClick = { selectedStationType = "DC" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("DC (Быстрая • Фиолетовый)")
                    }
                    SegmentedButton(
                        selected = selectedStationType == "AC",
                        onClick = { selectedStationType = "AC" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("AC (Медленная • Голубой)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Operator Selection Dropdown with simplified color tags
                Text(
                    text = strings.operator,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    val isDcOp = selectedStationType == "DC"
                    val tagColor = if (isDcOp) Color(0xFFC084FC) else Color(0xFF38BDF8)

                    VoltCard(
                        onClick = { operatorMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = if (nightTariffApplied) Color.White else tagColor.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            ) {
                                // Simplified Color Tag
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(tagColor.copy(alpha = 0.25f))
                                        .border(
                                            width = if (nightTariffApplied) 1.5.dp else 1.dp,
                                            color = if (nightTariffApplied) Color.White else tagColor,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = selectedStationType,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (nightTariffApplied) Color.White else tagColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = selectedOperator?.name ?: "Выберите оператора",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (nightTariffApplied) {
                                        Text(
                                            text = "🌙 Ночной тариф активен",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ElectricCyan,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = SoftBlue
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = operatorMenuExpanded,
                        onDismissRequest = { operatorMenuExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val distinctOps = filteredOperators.distinctBy { it.name.trim() }
                        distinctOps.forEach { op ->
                            val opColor = if (selectedStationType == "DC") Color(0xFFC084FC) else Color(0xFF38BDF8)
                            val standardPrice = if (selectedStationType == "DC") op.priceDc else op.priceAc
                            val nightPrice = if (selectedStationType == "DC") op.nightPriceDc else op.nightPriceAc

                            // Standard tariff option
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                                    .background(opColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = op.name,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (nightPrice != null) {
                                                    Text(
                                                        text = "Стандартный тариф (день)",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", standardPrice)} $currency",
                                            fontSize = 13.sp,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                },
                                onClick = {
                                    selectedOperator = op
                                    recalculatePrice(op, selectedStationType, applyNight = false)
                                    operatorMenuExpanded = false
                                }
                            )

                            // Night tariff option with extra white border / badge if operator supports night tariff
                            if (nightPrice != null) {
                                DropdownMenuItem(
                                    text = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.08f))
                                                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text("🌙", fontSize = 13.sp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Column {
                                                        Text(
                                                            text = "${op.name} • Ночной тариф",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "Льготный период (${op.nightStartHour}:00 - ${op.nightEndHour}:00)",
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.75f)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "${String.format(Locale.US, "%.2f", nightPrice)} $currency",
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedOperator = op
                                        recalculatePrice(op, selectedStationType, applyNight = true)
                                        operatorMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedOperator?.name?.contains("Другое", ignoreCase = true) == true) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customOperatorName,
                        onValueChange = { customOperatorName = it },
                        label = { Text(strings.customOperator) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price per kWh & Power
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pricePerKwhText,
                        onValueChange = { pricePerKwhText = it },
                        label = { Text("${strings.pricePerKwh} ($currency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("price_per_kwh_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = avgPowerText,
                        onValueChange = { avgPowerText = it },
                        label = { Text("Мощность, кВт") },
                        placeholder = { Text("напр. 60") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Night Tariff Badge with white border
                if (nightTariffApplied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftBlue.copy(alpha = 0.15f))
                            .border(1.5.dp, Color.White, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🌙", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${strings.nightTariffApplied} (Ночной тариф • Белая рамка)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            IconButton(
                                onClick = {
                                    nightTariffApplied = false
                                    val op = selectedOperator
                                    if (op != null) {
                                        val stdPrice = if (selectedStationType == "DC") op.priceDc else op.priceAc
                                        pricePerKwhText = String.format(Locale.US, "%.2f", stdPrice)
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = strings.cancelNightTariff,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date & Time Adjustment
                Text(
                    text = "Дата и время начала зарядки",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                VoltCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showTimeAdjustDialog = true }
                ) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = SoftBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sdf.format(Date(startTimestamp)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(Icons.Default.Edit, contentDescription = "Изменить время", tint = SoftBlue, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick shift chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { startTimestamp = System.currentTimeMillis() },
                            label = { Text("Сейчас", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { startTimestamp -= 30 * 60 * 1000L },
                            label = { Text("-30 мин", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { startTimestamp -= 60 * 60 * 1000L },
                            label = { Text("-1 час", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { startTimestamp -= 24 * 60 * 60 * 1000L },
                            label = { Text("Вчера", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Clean Bottom Hint: Price to 100%
                VoltCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricCyan.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Стоимость до 100%:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "+${String.format(Locale.US, "%.1f", neededKwhTo100)} кВт·ч энергии",
                                fontSize = 11.sp,
                                color = SoftBlue
                            )
                        }
                        Text(
                            text = "~${String.format(Locale.US, "%.2f", priceTo100)} $currency",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = BatteryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Launch Charging Button
                Button(
                    onClick = {
                        if (isFormValid && !isSubmitting) {
                            isSubmitting = true
                            onStartCharging(
                                odoVal,
                                socVal,
                                selectedStationType,
                                selectedOperator,
                                customOperatorName.ifBlank { null },
                                avgPowerVal,
                                priceVal,
                                startTimestamp,
                                nightTariffApplied
                            )
                        }
                    },
                    enabled = isFormValid && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(ElectricCyan, SoftBlue)
                            )
                        )
                        .border(
                            width = 1.2.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .testTag("launch_charging_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.startChargeButton,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Date & Time Adjustment Dialog
    if (showTimeAdjustDialog) {
        val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
        val sdfTime = remember { SimpleDateFormat("HH:mm", Locale.US) }
        var dateStr by remember { mutableStateOf(sdfDate.format(Date(startTimestamp))) }
        var timeStr by remember { mutableStateOf(sdfTime.format(Date(startTimestamp))) }

        AlertDialog(
            onDismissRequest = { showTimeAdjustDialog = false },
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = ElectricCyan) },
            title = { Text("Установить время зарядки") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Дата (ГГГГ-ММ-ДД)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = { timeStr = it },
                        label = { Text("Время (ЧЧ:ММ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val fullSdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                            val parsedDate = fullSdf.parse("$dateStr $timeStr")
                            if (parsedDate != null) {
                                startTimestamp = parsedDate.time
                            }
                        } catch (_: Exception) {}
                        showTimeAdjustDialog = false
                    }
                ) {
                    Text("Готово")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeAdjustDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
