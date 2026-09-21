package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.theme.ElectricCyan
import kotlinx.coroutines.delay

@Composable
fun AppLockScreen(
    correctPin: String,
    biometricEnabled: Boolean,
    onUnlockSuccess: () -> Unit,
    onTriggerBiometric: () -> Unit,
    biometricErrorMessage: String?,
    onClearBiometricError: () -> Unit,
    onResetPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }

    // Auto-trigger biometric on screen appear if enabled
    LaunchedEffect(biometricEnabled) {
        if (biometricEnabled) {
            delay(350L) // Small delay to guarantee Window focus
            onTriggerBiometric()
        }
    }

    // Function to check and submit PIN
    val checkPinAndUnlock = {
        val input = enteredPin.trim()
        val expected = correctPin.trim()
        if (input.isNotEmpty() && (expected.isEmpty() || input == expected)) {
            pinError = false
            onUnlockSuccess()
        } else {
            pinError = true
        }
    }

    LiquidGlassBackground(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo Icon with Cyan Aura
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                ElectricCyan.copy(alpha = 0.35f),
                                Color(0xFF0F172A).copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                ElectricCyan,
                                ElectricCyan.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Защита",
                    tint = ElectricCyan,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Volt",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Ledger",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Приложение защищено",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Введите PIN-код для доступа к функциям",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // PIN / Password Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                    .border(
                        width = 1.dp,
                        color = if (pinError) MaterialTheme.colorScheme.error else Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { input ->
                            if (input.length <= 16) {
                                enteredPin = input
                                pinError = false
                                onClearBiometricError()
                                // Auto-unlock if length matches and equals correct PIN
                                if (correctPin.isNotBlank() && input.trim() == correctPin.trim()) {
                                    onUnlockSuccess()
                                }
                            }
                        },
                        label = { Text("PIN-код / Пароль") },
                        placeholder = { Text("••••") },
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { checkPinAndUnlock() }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lock_pin_input")
                    )

                    if (pinError) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Неверный PIN-код или пароль",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Unlock Button
                    Button(
                        onClick = { checkPinAndUnlock() },
                        enabled = enteredPin.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("lock_unlock_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF070C18)
                        )
                    ) {
                        Text(
                            text = "Разблокировать",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Biometric Option
            if (biometricEnabled) {
                OutlinedButton(
                    onClick = {
                        onClearBiometricError()
                        onTriggerBiometric()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .border(
                            1.dp,
                            ElectricCyan.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("lock_biometric_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Отпечаток пальца",
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Войти по отпечатку пальца",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Biometric message or error
            AnimatedVisibility(
                visible = !biometricErrorMessage.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (!biometricErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = biometricErrorMessage,
                        fontSize = 12.sp,
                        color = if (biometricErrorMessage.contains("не распознан", ignoreCase = true)) {
                            MaterialTheme.colorScheme.error
                        } else {
                            ElectricCyan
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Forgot PIN / Reset Protection
            TextButton(
                onClick = { showForgotDialog = true },
                modifier = Modifier.testTag("lock_forgot_pin_button")
            ) {
                Text(
                    text = "Забыли PIN-код? Восстановление",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Reset PIN Confirmation Dialog
        if (showForgotDialog) {
            AlertDialog(
                onDismissRequest = { showForgotDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                },
                title = {
                    Text(
                        text = "Сброс защиты PIN-кодом",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Вы хотите сбросить PIN-код и разблокировать приложение?",
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Все ваши автомобили, журнал зарядок, расходы и статистика сохранятся полностью. Защита экрана блокировки будет отключена, и вы сможете установить новый пароль в Настройках.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showForgotDialog = false
                            onResetPin()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF070C18)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Сбросить и войти", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotDialog = false }) {
                        Text("Отмена")
                    }
                }
            )
        }
    }
}
