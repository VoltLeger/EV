package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.ChargingSession
import com.example.ui.components.ShareSessionCardDialog
import com.example.ui.screens.FinishChargingScreen
import com.example.ui.screens.HistoryScreen
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
import kotlinx.coroutines.delay

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
            val userProfile by viewModel.userProfile.collectAsState()
            val carExpenses by viewModel.carExpenses.collectAsState()
            val topExpenseCategories by viewModel.topExpenseCategories.collectAsState()

            VoltLedgerTheme(
                themeSetting = settings.theme,
                languageSetting = settings.language,
                currencySetting = settings.currency
            ) {
                // Request Notification permission if Android 13+ safely with 16-bit requestCode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            try {
                                ActivityCompat.requestPermissions(
                                    this@MainActivity,
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                    1001
                                )
                            } catch (e: Exception) {
                                // Silently handle if host OS or device blocks permission requests
                            }
                        }
                    }
                }

                val navController = rememberNavController()
                var showRangeForecastDialog by remember { mutableStateOf(false) }
                var showProfileAwardsDialog by remember { mutableStateOf(false) }
                var sessionToFinish by remember { mutableStateOf<ChargingSession?>(null) }
                var sessionToSharePostcard by remember { mutableStateOf<ChargingSession?>(null) }

                // Periodic active session notification check (e.g. DC charging > 2 hours)
                LaunchedEffect(Unit) {
                    while (true) {
                        viewModel.checkActiveSessionNotifications()
                        delay(60_000L)
                    }
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
                                    if (allCars.isNotEmpty() || settings.onboardingCompleted) {
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

                        // 3. Main Container (4 Tabs: Home, History, Statistics, Settings)
                        composable("main") {
                            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                            val strings = LocalAppStrings.current

                            Scaffold(
                                bottomBar = {
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                        tonalElevation = 0.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                            .border(
                                                width = 1.dp,
                                                brush = Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.28f),
                                                        Color.White.copy(alpha = 0.05f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                            )
                                            .testTag("bottom_nav_bar")
                                    ) {
                                        // Tab 0: Home
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
                                                    fontSize = 11.sp,
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

                                        // Tab 1: History
                                        NavigationBarItem(
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = strings.historyNav
                                                )
                                            },
                                            label = {
                                                Text(
                                                    strings.historyNav,
                                                    fontSize = 11.sp,
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

                                        // Tab 2: Statistics
                                        NavigationBarItem(
                                            selected = selectedTab == 2,
                                            onClick = { selectedTab = 2 },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.BarChart,
                                                    contentDescription = strings.statsNav
                                                )
                                            },
                                            label = {
                                                Text(
                                                    strings.statsNav,
                                                    fontSize = 11.sp,
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

                                        // Tab 3: Settings
                                        NavigationBarItem(
                                            selected = selectedTab == 3,
                                            onClick = { selectedTab = 3 },
                                            icon = {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = strings.settingsNav
                                                )
                                            },
                                            label = {
                                                Text(
                                                    strings.settingsNav,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal
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
                                val activeCarId = activeCar?.id ?: 0L
                                val activeCarSessions = remember(allSessions, activeCarId) {
                                    allSessions.filter { it.carId == activeCarId }
                                }
                                val activeCarExpenses = remember(carExpenses, activeCarId) {
                                    carExpenses.filter { it.carId == activeCarId }
                                }

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
                                                recentSessions = activeCarSessions,
                                                onSelectCar = { viewModel.selectCar(it) },
                                                onAddChargeClick = { navController.navigate("start_charging") },
                                                onCalculateRangeClick = { showRangeForecastDialog = true },
                                                onQuickHomeCharge = { currentSoc, meterKwh, customPrice, tariffName, isNight ->
                                                    viewModel.startQuickHomeCharge(currentSoc, meterKwh, customPrice, tariffName, isNight)
                                                },
                                                onNavigateToHistory = { selectedTab = 1 },
                                                onCompleteChargeClick = { session ->
                                                    sessionToFinish = session
                                                    navController.navigate("finish_charging")
                                                },
                                                onCancelActiveCharge = { session ->
                                                    viewModel.cancelActiveSession(session)
                                                },
                                                userProfile = userProfile,
                                                onOpenProfile = { showProfileAwardsDialog = true },
                                                onUpdateCar = { viewModel.updateCar(it) },
                                                onDeleteCar = { viewModel.deleteCar(it) },
                                                onUpdateSession = { viewModel.updateSession(it) },
                                                onDeleteSession = { viewModel.deleteSession(it) },
                                                onAddExpense = { category, amount, odometer, comment ->
                                                    viewModel.addExpense(category, amount, odometer, comment)
                                                },
                                                topExpenseCategories = topExpenseCategories,
                                                appSettings = settings,
                                                onNavigateToSettings = { selectedTab = 3 }
                                            )
                                            1 -> HistoryScreen(
                                                sessions = activeCarSessions,
                                                onUpdateSession = { viewModel.updateSession(it) },
                                                onDeleteSession = { viewModel.deleteSession(it) },
                                                expenses = activeCarExpenses,
                                                onAddExpense = { category, amount, odometer, comment ->
                                                    viewModel.addExpense(category, amount, odometer, comment)
                                                },
                                                onUpdateExpense = { viewModel.updateExpense(it) },
                                                onDeleteExpense = { viewModel.deleteExpense(it) },
                                                topCategories = topExpenseCategories,
                                                defaultOdometer = activeCar?.initialOdometer,
                                                activeCar = activeCar
                                            )
                                            2 -> StatisticsScreen(
                                                activeCar = activeCar,
                                                allSessions = allSessions,
                                                carExpenses = activeCarExpenses
                                            )
                                            3 -> SettingsScreen(
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
                                                onUpdateNotifyAchievements = { viewModel.updateNotifyAchievements(it) },
                                                onSendTestNotification = { viewModel.sendTestNotification() },
                                                onTestDcNotification = { viewModel.testDcNotification() },
                                                onTestAchievementUnlocked = { viewModel.testAchievementUnlockedNotification() },
                                                onTestAchievementProgress = { viewModel.testAchievementProgressNotification() },
                                                onExportCsv = { viewModel.exportCsvData() },
                                                onExportJson = { viewModel.exportJsonBackup() },
                                                onImportJson = { viewModel.importBackupJson(it, replace = false) },
                                                onRefreshTariffs = { viewModel.refreshTariffsFromFile() },
                                                userProfile = userProfile,
                                                onOpenProfile = { showProfileAwardsDialog = true },
                                                onUpdateUserEmail = { viewModel.updateUserEmail(it) },
                                                onSendEmailBackup = { email ->
                                                    val backupJson = viewModel.exportJsonBackup()
                                                    com.example.util.EmailBackupHelper.sendBackupByEmail(
                                                        context = this@MainActivity,
                                                        email = email,
                                                        backupJson = backupJson
                                                    )
                                                },
                                                onSaveHomeChargingSettings = { std, night, nPrice, nStart, nEnd, three, pPrice, pStart, pEnd, sPrice, sStart, sEnd ->
                                                    viewModel.saveHomeChargingSettings(
                                                        standardPrice = std,
                                                        nightTariffEnabled = night,
                                                        nightPrice = nPrice,
                                                        nightStartHour = nStart,
                                                        nightEndHour = nEnd,
                                                        threeTariffEnabled = three,
                                                        peakPrice = pPrice,
                                                        peakStartHour = pStart,
                                                        peakEndHour = pEnd,
                                                        semiPeakPrice = sPrice,
                                                        semiPeakStartHour = sStart,
                                                        semiPeakEndHour = sEnd
                                                    )
                                                }
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
                                    onComplete = { endSoc, kwhDelivered, kwhReceived, penalty, fixed, endTime, comment, endOdo ->
                                        navController.popBackStack()
                                        sessionToFinish = null
                                        val energyCost = kwhDelivered * currentSession.pricePerKwh
                                        val totalCost = energyCost + penalty + fixed
                                        val finished = currentSession.copy(
                                            endSoc = endSoc,
                                            kwhDeliveredByStation = kwhDelivered,
                                            kwhReceivedByCar = kwhReceived,
                                            energyCost = energyCost,
                                            penaltyCost = penalty,
                                            fixedAmount = fixed,
                                            totalCost = totalCost,
                                            endTime = endTime,
                                            operatorComment = comment ?: currentSession.operatorComment,
                                            status = "completed"
                                        )
                                        sessionToSharePostcard = finished
                                        viewModel.finishCharging(
                                            session = currentSession,
                                            endSoc = endSoc,
                                            kwhDelivered = kwhDelivered,
                                            kwhReceived = kwhReceived,
                                            penaltyCost = penalty,
                                            fixedAmount = fixed,
                                            endTime = endTime,
                                            comment = comment,
                                            endOdometer = endOdo
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

                // Pilot Profile & Hall of Fame Awards Dialog
                if (showProfileAwardsDialog) {
                    ProfileAwardsDialog(
                        viewModel = viewModel,
                        onDismiss = { showProfileAwardsDialog = false }
                    )
                }

                // Social Media Postcard Share Dialog on Completion
                if (sessionToSharePostcard != null) {
                    ShareSessionCardDialog(
                        session = sessionToSharePostcard!!,
                        car = activeCar,
                        currency = settings.currency,
                        onDismiss = { sessionToSharePostcard = null }
                    )
                }
            }
        }
    }
}
