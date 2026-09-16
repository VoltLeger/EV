package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ChargingSession
import com.example.ui.screens.FinishChargingScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileAwardsDialog
import com.example.ui.screens.RangeForecastDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.StartChargingScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LocalAppStrings
import com.example.ui.theme.SoftBlue
import com.example.ui.theme.VoltLedgerTheme
import com.example.ui.viewmodel.VoltViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VoltViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            val allCars by viewModel.allCars.collectAsState()
            val activeCar by viewModel.activeCar.collectAsState()
            val allSessions by viewModel.allSessions.collectAsState()
            val activeSession by viewModel.activeSession.collectAsState()
            val monthAvgConsumption by viewModel.monthAvgConsumption.collectAsState()
            val operators by viewModel.allOperators.collectAsState()
            val tags by viewModel.allTags.collectAsState()

            VoltLedgerTheme(
                themeSetting = settings.theme,
                languageSetting = settings.language,
                currencySetting = settings.currency
            ) {
                // Request Notification permission if Android 13+
                val context = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val navController = rememberNavController()
                var showRangeForecastDialog by remember { mutableStateOf(false) }
                var sessionToFinish by remember { mutableStateOf<ChargingSession?>(null) }

                // Periodic active session notification check (e.g. DC charging > 2 hours)
                LaunchedEffect(Unit) {
                    while (true) {
                        viewModel.checkActiveSessionNotifications()
                        delay(60_000L)
                    }
                }

                // PIN Protection Lock State
                var isPinUnlocked by rememberSaveable { mutableStateOf(!settings.pinEnabled || settings.pinCode.isBlank()) }
                var enteredPin by remember { mutableStateOf("") }
                var pinError by remember { mutableStateOf(false) }

                if (settings.pinEnabled && settings.pinCode.isNotBlank() && !isPinUnlocked) {
                    AlertDialog(
                        onDismissRequest = { /* Modal lock */ },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = ElectricCyan) },
                        title = { Text("Введите PIN-код") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Приложение защищено PIN-кодом", fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = enteredPin,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { ch -> ch.isDigit() }) {
                                            enteredPin = it
                                            pinError = false
                                        }
                                    },
                                    label = { Text("4 цифры") },
                                    isError = pinError,
                                    supportingText = { if (pinError) Text("Неверный PIN-код", color = MaterialTheme.colorScheme.error) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (enteredPin == settings.pinCode) {
                                        isPinUnlocked = true
                                        pinError = false
                                    } else {
                                        pinError = true
                                    }
                                }
                            ) {
                                Text("Разблокировать")
                            }
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = "splash",
                        modifier = Modifier.fillMaxSize()
                    ) {
                    // 1. Splash Screen
                    composable("splash") {
                        SplashScreen(
                            onNavigateNext = {
                                if (settings.onboardingCompleted && allCars.isNotEmpty()) {
                                    navController.navigate("main") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                } else {
                                    navController.navigate("onboarding") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }

                    // 2. Onboarding Screen
                    composable("onboarding") {
                        OnboardingScreen(
                            onFinish = { name, capacity, odo, soc, passport ->
                                viewModel.completeOnboarding(name, capacity, odo, soc, passport)
                                navController.navigate("main") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    // 3. Main Container (Tabs: Home, Statistics, Settings)
                    composable("main") {
                        var selectedTab by remember { mutableIntStateOf(0) }
                        val strings = LocalAppStrings.current

                        Scaffold(
                            bottomBar = {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                                    tonalElevation = 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.verticalGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.28f),
                                                    Color.White.copy(alpha = 0.05f)
                                                )
                                            ),
                                            shape = androidx.compose.ui.graphics.RectangleShape
                                        )
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                        .testTag("bottom_nav_bar")
                                ) {
                                    NavigationBarItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.ElectricBolt,
                                                contentDescription = strings.homeNav
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.homeNav,
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = ElectricCyan.copy(alpha = 0.22f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.BarChart,
                                                contentDescription = strings.statsNav
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.statsNav,
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = ElectricCyan.copy(alpha = 0.22f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )

                                    NavigationBarItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2 },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.Settings,
                                                contentDescription = strings.settingsNav
                                            )
                                        },
                                        label = {
                                            Text(
                                                strings.settingsNav,
                                                fontSize = 12.sp,
                                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ElectricCyan,
                                            selectedTextColor = ElectricCyan,
                                            indicatorColor = ElectricCyan.copy(alpha = 0.22f),
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "tab_transition"
                                ) { tab ->
                                    when (tab) {
                                        0 -> HomeScreen(
                                            activeCar = activeCar,
                                            allCars = allCars,
                                            activeSession = activeSession,
                                            monthConsumption = monthAvgConsumption,
                                            recentSessions = allSessions.filter { it.carId == (activeCar?.id ?: 0L) },
                                            onSelectCar = { viewModel.selectCar(it) },
                                            onAddChargeClick = { navController.navigate("start_charging") },
                                            onCalculateRangeClick = { showRangeForecastDialog = true },
                                            onQuickHomeCharge = { targetSoc -> viewModel.startQuickHomeCharge(targetSoc) },
                                            onCompleteChargeClick = { session ->
                                                sessionToFinish = session
                                                navController.navigate("finish_charging")
                                            },
                                            onCancelActiveCharge = { session ->
                                                viewModel.cancelActiveSession(session)
                                            }
                                        )
                                        1 -> StatisticsScreen(
                                            activeCar = activeCar,
                                            allSessions = allSessions
                                        )
                                        2 -> SettingsScreen(
                                            settings = settings,
                                            allCars = allCars,
                                            activeCar = activeCar,
                                            operators = operators,
                                            tags = tags,
                                            onSelectCar = { viewModel.selectCar(it) },
                                            onAddCar = { name, cap, odo, soc, passport -> viewModel.addCar(name, cap, odo, soc, passport) },
                                            onDeleteCar = { viewModel.deleteCar(it) },
                                            onAddOperator = { viewModel.addOperator(it) },
                                            onUpdateOperator = { viewModel.updateOperator(it) },
                                            onDeleteOperator = { viewModel.deleteOperator(it) },
                                            onAddTag = { name, color -> viewModel.addTag(name, color) },
                                            onDeleteTag = { viewModel.deleteTag(it) },
                                            onUpdateLanguage = { viewModel.updateLanguage(it) },
                                            onUpdateTheme = { viewModel.updateTheme(it) },
                                            onUpdateCurrency = { viewModel.updateCurrency(it) },
                                            onUpdateAutoNightTariff = { viewModel.updateAutoNightTariff(it) },
                                            onUpdateNotificationsEnabled = { viewModel.updateNotificationsEnabled(it) },
                                            onUpdateNotifyUnfinished = { viewModel.updateNotifyUnfinished(it) },
                                            onUpdateUnfinishedHours = { viewModel.updateUnfinishedHours(it) },
                                            onUpdateNotifyWeekly = { viewModel.updateNotifyWeekly(it) },
                                            onUpdateNotifyMonthly = { viewModel.updateNotifyMonthly(it) },
                                            onSendTestNotification = { viewModel.sendTestNotification() },
                                            onTestDcNotification = { viewModel.testDcNotification() },
                                            onUpdatePinSettings = { enabled, pin -> viewModel.updatePinSettings(enabled, pin) },
                                            onExportCsv = { viewModel.exportCsvData() },
                                            onExportJson = { viewModel.exportJsonBackup() },
                                            onImportJson = { viewModel.importBackupJson(it, replace = false) },
                                            onRefreshTariffs = { viewModel.refreshTariffsFromFile() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. Start Charging (Stage 1)
                    composable("start_charging") {
                        StartChargingScreen(
                            activeCar = activeCar,
                            operators = operators,
                            autoNightTariffEnabled = settings.autoNightTariff,
                            onBack = { navController.popBackStack() },
                            onStartCharging = { odo, soc, type, op, customName, power, price, time, night ->
                                navController.popBackStack()
                                viewModel.startCharging(
                                    odometer = odo,
                                    startSoc = soc,
                                    stationType = type,
                                    operator = op,
                                    customOperatorName = customName,
                                    avgPowerKw = power,
                                    pricePerKwh = price,
                                    startTime = time,
                                    nightTariffApplied = night
                                )
                            }
                        )
                    }

                    // 5. Finish Charging (Stage 2)
                    composable("finish_charging") {
                        val currentSession = remember { sessionToFinish ?: activeSession }
                        if (currentSession != null) {
                            val op = operators.find { it.id == currentSession.operatorId }
                            FinishChargingScreen(
                                session = currentSession,
                                operator = op,
                                onBack = {
                                    sessionToFinish = null
                                    navController.popBackStack()
                                },
                                onComplete = { endSoc, kwhDelivered, kwhReceived, penalty, fixed, endTime ->
                                    navController.popBackStack()
                                    sessionToFinish = null
                                    viewModel.finishCharging(
                                        session = currentSession,
                                        endSoc = endSoc,
                                        kwhDelivered = kwhDelivered,
                                        kwhReceived = kwhReceived,
                                        penaltyCost = penalty,
                                        fixedAmount = fixed,
                                        endTime = endTime
                                    )
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                sessionToFinish = null
                                navController.popBackStack()
                            }
                        }
                    }
                }
                }

                // Range Forecast Dialog
                if (showRangeForecastDialog) {
                    val carSessions = allSessions.filter { it.carId == (activeCar?.id ?: 0L) }
                    RangeForecastDialog(
                        car = activeCar,
                        completedSessions = carSessions,
                        onDismiss = { showRangeForecastDialog = false }
                    )
                }
            }
        }
    }
}
