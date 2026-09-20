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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.ui.components.VoltCard
import com.example.ui.theme.BatteryGreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftBlue
import com.example.util.EVCalculator
import java.util.Locale

@Composable
fun OnboardingScreen(
    onFinish: (name: String, capacity: Double, odo: Double, soc: Double, passport: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    var carName by remember { mutableStateOf("Мой Электромобиль") }
    var capacityText by remember { mutableStateOf("70.0") }
    var passportText by remember { mutableStateOf("16.0") }
    var odometerText by remember { mutableStateOf("15000") }
    var socText by remember { mutableStateOf("80") }

    val capacityVal = capacityText.toDoubleOrNull() ?: 0.0
    val passportVal = passportText.toDoubleOrNull() ?: 16.0
    val odoVal = odometerText.toDoubleOrNull() ?: 0.0
    val socVal = socText.toDoubleOrNull() ?: 0.0

    val isCapacityValid = capacityVal in 20.0..240.0
    val isOdoValid = odoVal >= 0.0
    val isSocValid = socVal in 0.0..100.0
    val isValid = carName.isNotBlank() && isCapacityValid && isOdoValid && isSocValid

    val bufferKwh = if (isCapacityValid) EVCalculator.calculateBuffer(capacityVal) else 0.0
    val usableKwh = if (isCapacityValid) EVCalculator.calculateUsableCapacity(capacityVal) else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Icon(
            imageVector = Icons.Default.DirectionsCar,
            contentDescription = null,
            tint = SoftBlue,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = strings.welcomeTitle,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = strings.welcomeSubtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Step 1: Car Name
        OutlinedTextField(
            value = carName,
            onValueChange = { carName = it },
            label = { Text(strings.carName) },
            placeholder = { Text(strings.carNameHint) },
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = SoftBlue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("car_name_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Declared Capacity (20-240 kWh)
        OutlinedTextField(
            value = capacityText,
            onValueChange = { capacityText = it },
            label = { Text(strings.declaredCapacity) },
            placeholder = { Text(strings.declaredCapacityHint) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = BatteryGreen)
            },
            isError = capacityText.isNotBlank() && !isCapacityValid,
            supportingText = {
                if (capacityText.isNotBlank() && !isCapacityValid) {
                    Text(
                        text = "Введите ёмкость от 20 до 240 кВт·ч",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(strings.declaredCapacityHint)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("capacity_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        // Live Usable Capacity Display Card
        if (isCapacityValid) {
            Spacer(modifier = Modifier.height(8.dp))
            VoltCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = BatteryGreen.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.usableCapacity,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f кВт·ч", usableKwh),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BatteryGreen
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = strings.calculatedBuffer,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.2f кВт·ч", bufferKwh),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2.5: Passport Consumption (WLTP)
        OutlinedTextField(
            value = passportText,
            onValueChange = { passportText = it },
            label = { Text("Паспортный расход WLTP (кВт·ч/100 км)") },
            placeholder = { Text("напр. 15.5") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            leadingIcon = {
                Icon(Icons.Default.Speed, contentDescription = null, tint = SoftBlue)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("passport_consumption_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Current Odometer
        OutlinedTextField(
            value = odometerText,
            onValueChange = { odometerText = it },
            label = { Text(strings.initialOdometer) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan)
            },
            isError = odometerText.isNotBlank() && !isOdoValid,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("odometer_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: Current SoC
        OutlinedTextField(
            value = socText,
            onValueChange = { socText = it },
            label = { Text(strings.currentSoc) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = SoftBlue)
            },
            isError = socText.isNotBlank() && !isSocValid,
            supportingText = {
                if (socText.isNotBlank() && !isSocValid) {
                    Text(
                        text = "От 0 до 100%",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("soc_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SoftBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Start Button
        Button(
            onClick = {
                if (isValid) {
                    onFinish(carName.trim(), capacityVal, odoVal, socVal, passportVal)
                }
            },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_start_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SoftBlue,
                contentColor = Color.White
            )
        ) {
            Text(
                text = strings.startApp,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
