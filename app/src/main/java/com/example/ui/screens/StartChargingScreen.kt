package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Car
import com.example.data.model.Operator
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.TagBadge
import com.example.ui.components.VoltCard
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

    var odometerText by remember {
        mutableStateOf(activeCar?.initialOdometer?.toInt()?.toString() ?: "0")
    }
    var socText by remember {
        mutableStateOf(activeCar?.currentSoc?.toInt()?.toString() ?: "30")
    }
    var selectedStationType by remember { mutableStateOf("AC") } // "AC" or "DC"
    var selectedOperator by remember { mutableStateOf<Operator?>(null) }
    var operatorMenuExpanded by remember { mutableStateOf(false) }
    var customOperatorName by remember { mutableStateOf("") }
    var avgPowerText by remember { mutableStateOf("") }
    var pricePerKwhText by remember { mutableStateOf("0.55") }
    var nightTariffApplied by remember { mutableStateOf(false) }
    var startTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filteredOperators = remember(operators, selectedStationType) {
        operators.filter {
            it.type.equals(selectedStationType, ignoreCase = true) || it.type.equals("both", ignoreCase = true)
        }
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

    // Update price when operator, station type, or night tariff changes
    fun recalculatePrice(op: Operator?, type: String, applyNight: Boolean) {
        if (op == null) return
        if (op.isFree) {
            pricePerKwhText = "0.00"
            nightTariffApplied = false
            return
        }

        val isNight = applyNight && autoNightTariffEnabled &&
                ((type == "DC" && op.nightPriceDc != null) || (type == "AC" && op.nightPriceAc != null)) &&
                EVCalculator.isNightTariffTime(op.nightStartHour, op.nightEndHour)

        nightTariffApplied = isNight

        val price = if (type == "DC") {
            if (isNight && op.nightPriceDc != null) op.nightPriceDc else op.priceDc
        } else {
            if (isNight && op.nightPriceAc != null) op.nightPriceAc else op.priceAc
        }
        pricePerKwhText = String.format(Locale.US, "%.2f", price)
    }

    LaunchedEffect(selectedOperator, selectedStationType) {
        recalculatePrice(selectedOperator, selectedStationType, applyNight = true)
    }

    val odoVal = odometerText.toDoubleOrNull() ?: 0.0
    val socVal = socText.toDoubleOrNull() ?: 0.0
    val priceVal = pricePerKwhText.toDoubleOrNull() ?: 0.0
    val avgPowerVal = avgPowerText.toDoubleOrNull()

    val isOdoValid = odoVal >= 0.0
    val isSocValid = socVal in 0.0..100.0
    val isFormValid = isOdoValid && isSocValid

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
                // Odometer & SoC Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = odometerText,
                        onValueChange = { odometerText = it },
                        label = { Text(strings.startOdometer) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_odometer_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = socText,
                        onValueChange = { socText = it },
                        label = { Text("SoC %") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("start_soc_input"),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Station Type Selector (AC / DC)
                Text(
                    text = strings.stationType,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedStationType == "AC",
                        onClick = { selectedStationType = "AC" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("AC (Медленная)")
                    }
                    SegmentedButton(
                        selected = selectedStationType == "DC",
                        onClick = { selectedStationType = "DC" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("DC (Быстрая)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Operator Selection Dropdown (filtered by AC/DC and deduplicated)
                Text(
                    text = strings.operator,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    VoltCard(
                        onClick = { operatorMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (selectedOperator?.comment?.isNotBlank() == true) {
                                    TagBadge(name = selectedOperator?.comment ?: "", color = 0xFF38BDF8)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = selectedOperator?.name ?: "Выберите оператора",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (op.comment.isNotBlank()) {
                                                TagBadge(name = op.comment, color = 0xFF38BDF8)
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(op.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        val priceVal = if (selectedStationType == "DC") op.priceDc else op.priceAc
                                        Text(
                                            text = "${String.format(Locale.US, "%.2f", priceVal)} $currency",
                                            fontSize = 12.sp,
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    selectedOperator = op
                                    operatorMenuExpanded = false
                                }
                            )
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

                // Night Tariff Badge with one-click cancel
                if (nightTariffApplied) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SoftBlue.copy(alpha = 0.15f))
                            .border(1.dp, SoftBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
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
                                    text = strings.nightTariffApplied,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SoftBlue
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

                // Optional: Preliminary Cost Estimation
                var showCostEstimate by remember { mutableStateOf(false) }
                val targetEstimateSoc = 80.0
                val usableCap = activeCar?.usableCapacityKwh ?: 55.0
                val deltaSoc = (targetEstimateSoc - socVal).coerceAtLeast(0.0)
                val estimatedKwh = usableCap * (deltaSoc / 100.0)
                val estimatedCost = estimatedKwh * priceVal

                TextButton(
                    onClick = { showCostEstimate = !showCostEstimate },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showCostEstimate) "Скрыть оценку стоимости" else "💡 Предварительная оценка стоимости",
                        fontSize = 13.sp,
                        color = ElectricCyan
                    )
                }

                if (showCostEstimate) {
                    VoltCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = ElectricCyan.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Оценка до 80% (+$deltaSoc%):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "~${String.format(Locale.US, "%.1f", estimatedKwh)} кВт·ч",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "~${String.format(Locale.US, "%.2f", estimatedCost)} $currency",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = ElectricCyan
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Start Date & Time
                Text(
                    text = strings.startTime,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                VoltCard(modifier = Modifier.fillMaxWidth()) {
                    val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
                    Text(
                        text = sdf.format(Date(startTimestamp)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

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
}
