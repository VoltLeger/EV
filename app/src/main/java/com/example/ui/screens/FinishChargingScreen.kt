package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.VoltCard
import com.example.ui.components.formatCurrency
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.BatteryOrange
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.LocalCurrency
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishChargingScreen(
    session: ChargingSession,
    operator: Operator?,
    onBack: () -> Unit,
    onComplete: (
        endSoc: Double,
        kwhDelivered: Double,
        kwhReceived: Double?,
        penaltyCost: Double,
        fixedAmount: Double,
        endTime: Long
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val currency = LocalCurrency.current

    val now = remember { System.currentTimeMillis() }
    var isSubmitting by remember { mutableStateOf(false) }

    var endSocText by remember { mutableStateOf("90") }
    var kwhDeliveredText by remember { mutableStateOf("35.0") }
    var kwhReceivedText by remember { mutableStateOf("") }
    var energyCostText by remember { mutableStateOf("") }
    var penaltyCostText by remember { mutableStateOf("0.00") }
    var fixedAmountText by remember { mutableStateOf("0.00") }
    var totalCostText by remember { mutableStateOf("") }

    // Check suggested penalty from operator rules
    val suggestedPenalty = remember {
        EVCalculator.calculateSuggestedPenalty(operator, session.startTime, now)
    }

    LaunchedEffect(Unit) {
        if (suggestedPenalty > 0.0) {
            penaltyCostText = String.format(Locale.US, "%.2f", suggestedPenalty)
        }
    }

    // Auto-compute energy cost and total cost
    val deliveredVal = kwhDeliveredText.toDoubleOrNull() ?: 0.0
    val receivedVal = kwhReceivedText.toDoubleOrNull()
    val penaltyVal = penaltyCostText.toDoubleOrNull() ?: 0.0
    val fixedVal = fixedAmountText.toDoubleOrNull() ?: 0.0
    val endSocVal = endSocText.toDoubleOrNull() ?: 0.0

    LaunchedEffect(deliveredVal, session.pricePerKwh) {
        val calcEnergy = deliveredVal * session.pricePerKwh
        energyCostText = String.format(Locale.US, "%.2f", calcEnergy)
    }

    val energyVal = energyCostText.toDoubleOrNull() ?: (deliveredVal * session.pricePerKwh)
    val computedTotal = energyVal + penaltyVal + fixedVal

    // Losses calculation if receivedByCar is provided
    val hasLosses = receivedVal != null && receivedVal > 0 && deliveredVal > receivedVal
    val lossPercentage = if (hasLosses && deliveredVal > 0) {
        ((deliveredVal - receivedVal!!) / deliveredVal) * 100.0
    } else 0.0
    val lossOverpayment = if (hasLosses) {
        (deliveredVal - receivedVal!!) * session.pricePerKwh
    } else 0.0

    val isEndSocValid = endSocVal in 0.0..100.0
    val isDeliveredValid = deliveredVal > 0.0
    val isFormValid = isEndSocValid && isDeliveredValid

    LiquidGlassBackground(modifier = modifier) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.finishChargeTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Summary banner of active session
            VoltCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = session.operatorName.ifBlank { "EV Station" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Начало: ${session.startSoc.toInt()}% • ${session.stationType}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.2f", session.pricePerKwh)} $currency/кВт·ч",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // End SoC & Delivered kWh Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = endSocText,
                    onValueChange = { endSocText = it },
                    label = { Text(strings.endSoc) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("end_soc_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = kwhDeliveredText,
                    onValueChange = { kwhDeliveredText = it },
                    label = { Text(strings.deliveredByStation) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("kwh_delivered_input"),
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Received by car (optional)
            OutlinedTextField(
                value = kwhReceivedText,
                onValueChange = { kwhReceivedText = it },
                label = { Text(strings.receivedByCar) },
                placeholder = { Text("кВт·ч (необязательно)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            // Losses & Overpayment Card (if car kWh is filled)
            if (hasLosses) {
                Spacer(modifier = Modifier.height(10.dp))
                VoltCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = BatteryOrange.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.chargingLosses,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f%%", lossPercentage),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BatteryOrange
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = strings.overpaymentFromLosses,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(lossOverpayment, currency),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Energy Cost Field
            OutlinedTextField(
                value = energyCostText,
                onValueChange = { energyCostText = it },
                label = { Text("${strings.energyCost} ($currency)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            val isHomeCharging = session.operatorName.contains("Дом", ignoreCase = true) ||
                    session.stationType.equals("Home", ignoreCase = true)

            if (!isHomeCharging) {
                Spacer(modifier = Modifier.height(16.dp))

                // Penalty & Fixed Amount Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = penaltyCostText,
                        onValueChange = { penaltyCostText = it },
                        label = { Text("Штраф / простой") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = fixedAmountText,
                        onValueChange = { fixedAmountText = it },
                        label = { Text("Фикс. сумма") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (suggestedPenalty > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = BatteryOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${strings.penaltyRuleHint}: ${String.format(Locale.US, "%.2f", suggestedPenalty)} $currency",
                            fontSize = 11.sp,
                            color = BatteryOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Total Cost Banner
            VoltCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = ElectricCyan.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.totalCost,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(computedTotal, currency),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ElectricCyan
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Энергия: ${String.format(Locale.US, "%.2f", energyVal)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (penaltyVal > 0.0) {
                            Text(
                                text = "Штраф: ${String.format(Locale.US, "%.2f", penaltyVal)}",
                                fontSize = 11.sp,
                                color = BatteryOrange
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Complete Button
            Button(
                onClick = {
                    if (isFormValid && !isSubmitting) {
                        isSubmitting = true
                        onComplete(
                            endSocVal,
                            deliveredVal,
                            receivedVal,
                            penaltyVal,
                            fixedVal,
                            now
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
                            listOf(BatteryGreen, ElectricCyan)
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.15f))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .testTag("save_and_complete_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.completeButton,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    }
}
