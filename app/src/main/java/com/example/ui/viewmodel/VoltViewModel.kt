package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.local.seedDefaultData
import com.example.data.model.AppSettings
import com.example.data.model.Award
import com.example.data.model.Car
import com.example.data.model.CarExpense
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.data.repository.VoltRepository
import com.example.util.AwardCalculator
import com.example.util.CsvJsonBackupHelper
import com.example.util.DefaultTariffsLoader
import com.example.util.EVCalculator
import com.example.util.EmailBackupHelper
import com.example.util.NotificationHelper
import com.example.util.RangeForecastResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class VoltViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val settingsManager = SettingsManager(application)
    val repository = VoltRepository(db, settingsManager)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seedDefaultData(application, db)
            repository.getOrCreateUserProfile()
        }
    }

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfile()
        )

    val settings: StateFlow<AppSettings> = repository.appSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    val allCars: StateFlow<List<Car>> = repository.allCars.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val activeCar: StateFlow<Car?> = combine(allCars, settings) { cars, sett ->
        if (cars.isEmpty()) return@combine null
        cars.find { it.id == sett.selectedCarId } ?: cars.find { it.isActive } ?: cars.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val allSessions: StateFlow<List<ChargingSession>> = repository.getAllSessions().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val activeSession: StateFlow<ChargingSession?> = repository.activeSession.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val allOperators: StateFlow<List<Operator>> = repository.allOperators
        .map { list -> list.distinctBy { it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .map { list -> list.distinctBy { it.name } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val allExpenses: StateFlow<List<CarExpense>> = repository.allExpenses.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val carExpenses: StateFlow<List<CarExpense>> = combine(allExpenses, activeCar) { expenses, car ->
        if (car == null) expenses else expenses.filter { it.carId == car.id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val topExpenseCategories: StateFlow<List<String>> = carExpenses.map { list ->
        val counts = list.groupingBy { it.category }.eachCount()
        val sortedFromUser = counts.entries.sortedByDescending { it.value }.map { it.key }
        val defaults = listOf("Мойка", "ТО", "Страховка", "Шиномонтаж", "Парковка", "Омывайка и химия", "Ремонт", "Тюнинг", "Штрафы", "Другое")
        (sortedFromUser + defaults).distinct().take(5)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = listOf("Мойка", "ТО", "Страховка", "Шиномонтаж", "Парковка")
    )

    // Current month consumption:
    val monthAvgConsumption: StateFlow<Double?> = combine(allSessions, activeCar) { sessions, car ->
        val carId = car?.id ?: return@combine null
        val carSessions = sessions.filter { it.carId == carId && it.status == "completed" }
        val now = Calendar.getInstance()
        val capacity = car.usableCapacityKwh.takeIf { it > 0.0 } ?: 57.0
        EVCalculator.calculateMonthConsumption(carSessions, now.get(Calendar.YEAR), now.get(Calendar.MONTH), capacity)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    private val _forecastState = MutableStateFlow<RangeForecastResult?>(null)
    val forecastState: StateFlow<RangeForecastResult?> = _forecastState.asStateFlow()

    val awards: StateFlow<List<Award>> = combine(allSessions, allCars, activeCar) { sessions, cars, car ->
        AwardCalculator.calculateAwards(sessions, cars, car)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val totalXp: StateFlow<Long> = combine(allSessions, awards) { sessions, aw ->
        AwardCalculator.calculateTotalXp(sessions, aw)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0L
    )

    val rankTier: StateFlow<Pair<String, Int>> = totalXp.map { xp ->
        AwardCalculator.getRankTier(xp)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "Новичок" to 1
    )

    fun updateUserProfile(
        displayName: String? = null,
        callsign: String? = null,
        bio: String? = null,
        avatarEffect: String? = null,
        avatarIcon: String? = null,
        isLeaderboardOptIn: Boolean? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = repository.getOrCreateUserProfile()
            val xp = totalXp.value
            val tier = AwardCalculator.getRankTier(xp).first
            val updated = current.copy(
                displayName = displayName ?: current.displayName,
                callsign = callsign ?: current.callsign,
                bio = bio ?: current.bio,
                avatarEffect = avatarEffect ?: current.avatarEffect,
                avatarIcon = avatarIcon ?: current.avatarIcon,
                isLeaderboardOptIn = isLeaderboardOptIn ?: current.isLeaderboardOptIn,
                totalXp = xp,
                rankTier = tier,
                lastActiveAt = System.currentTimeMillis()
            )
            repository.updateUserProfile(updated)
        }
    }

    fun calculateForecast(remainingSoc: Double) {
        val car = activeCar.value ?: return
        val carSessions = allSessions.value.filter { it.carId == car.id && it.status == "completed" }
        val result = EVCalculator.calculateRangeForecast(
            completedSessions = carSessions,
            usableCapacityKwh = car.usableCapacityKwh,
            remainingSoc = remainingSoc
        )
        _forecastState.value = result
    }

    fun completeOnboarding(
        carName: String,
        declaredCapacityKwh: Double,
        odometer: Double,
        soc: Double,
        passportConsumption: Double = 16.0
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val usable = EVCalculator.calculateUsableCapacity(declaredCapacityKwh)
            val car = Car(
                name = carName.ifBlank { "My EV" },
                declaredCapacityKwh = declaredCapacityKwh.coerceIn(20.0, 240.0),
                usableCapacityKwh = usable,
                initialOdometer = odometer.coerceAtLeast(0.0),
                currentSoc = soc.coerceIn(0.0, 100.0),
                passportConsumption = passportConsumption.coerceIn(5.0, 50.0),
                isActive = true
            )
            val carId = repository.insertCar(car)
            repository.setActiveCar(carId)
            repository.setOnboardingCompleted(true)
        }
    }

    fun startCharging(
        odometer: Double,
        startSoc: Double,
        stationType: String,
        operator: Operator?,
        customOperatorName: String?,
        avgPowerKw: Double?,
        pricePerKwh: Double,
        startTime: Long,
        nightTariffApplied: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val car = activeCar.value ?: allCars.value.firstOrNull() ?: return@launch
            val isFree = operator?.isFree == true || pricePerKwh <= 0.0001
            val effectivePrice = if (isFree) 0.00001 else pricePerKwh

            val session = ChargingSession(
                carId = car.id,
                startOdometer = odometer,
                startSoc = startSoc,
                endSoc = startSoc,
                pricePerKwh = effectivePrice,
                currency = settings.value.currency,
                stationType = stationType,
                operatorId = operator?.id,
                operatorName = operator?.name ?: customOperatorName ?: "",
                operatorComment = operator?.comment,
                avgPowerKw = avgPowerKw,
                isFreeCharge = isFree,
                nightTariffApplied = nightTariffApplied,
                startTime = startTime,
                status = "active"
            )
            repository.insertSession(session)
        }
    }

    fun finishCharging(
        session: ChargingSession,
        endSoc: Double,
        kwhDelivered: Double,
        kwhReceived: Double?,
        penaltyCost: Double,
        fixedAmount: Double,
        endTime: Long,
        comment: String? = null,
        endOdometer: Double? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val energyCost = kwhDelivered * session.pricePerKwh
            val totalCost = energyCost + penaltyCost + fixedAmount

            val prevSessions = allSessions.value
            val currentCars = allCars.value
            val car = activeCar.value ?: currentCars.find { it.id == session.carId }
            val prevAwards = AwardCalculator.calculateAwards(prevSessions, currentCars, car)

            val updatedSession = session.copy(
                endSoc = endSoc,
                kwhDeliveredByStation = kwhDelivered,
                kwhReceivedByCar = kwhReceived,
                energyCost = energyCost,
                penaltyCost = penaltyCost,
                fixedAmount = fixedAmount,
                totalCost = totalCost,
                operatorComment = comment ?: session.operatorComment,
                endTime = endTime,
                status = "completed"
            )
            repository.updateSession(updatedSession)

            // Update car's current SOC and odometer
            if (car != null) {
                val candidateOdo = endOdometer ?: session.startOdometer
                val newOdo = maxOf(car.initialOdometer, candidateOdo)
                repository.updateCar(
                    car.copy(
                        currentSoc = endSoc,
                        initialOdometer = newOdo
                    )
                )
            }

            // Check achievement progress and unlocks
            val notifyOn = settings.value.notificationsEnabled && settings.value.notifyAchievements
            if (notifyOn) {
                val isEn = settings.value.language == "en"
                val nextSessions = prevSessions.map { if (it.id == updatedSession.id) updatedSession else it }
                val newAwards = AwardCalculator.calculateAwards(nextSessions, currentCars, car)

                val newlyUnlocked = newAwards.filter { newAw ->
                    val oldAw = prevAwards.find { it.id == newAw.id }
                    newAw.isUnlocked && (oldAw == null || !oldAw.isUnlocked)
                }

                if (newlyUnlocked.isNotEmpty()) {
                    for (aw in newlyUnlocked) {
                        NotificationHelper.showAchievementUnlockedNotification(
                            context = getApplication(),
                            title = aw.title,
                            description = aw.description,
                            xpReward = aw.xpReward,
                            isEn = isEn
                        )
                    }
                } else {
                    // Check if progress increased towards any achievement
                    val progressAw = newAwards.firstOrNull { newAw ->
                        val oldProgress = prevAwards.find { it.id == newAw.id }?.currentProgress ?: 0.0
                        !newAw.isUnlocked && newAw.currentProgress > oldProgress && newAw.currentProgress > 0
                    }
                    if (progressAw != null) {
                        NotificationHelper.showAchievementProgressNotification(
                            context = getApplication(),
                            title = progressAw.title,
                            current = progressAw.currentProgress.toInt(),
                            total = progressAw.maxProgress.toInt(),
                            unit = progressAw.unit,
                            isEn = isEn
                        )
                    }
                }
            }
        }
    }

    fun cancelActiveSession(session: ChargingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(session)
        }
    }

    fun addCar(name: String, declaredKwh: Double, odometer: Double, currentSoc: Double, passportConsumption: Double = 16.0) {
        viewModelScope.launch(Dispatchers.IO) {
            val usable = EVCalculator.calculateUsableCapacity(declaredKwh)
            val car = Car(
                name = name,
                declaredCapacityKwh = declaredKwh.coerceIn(20.0, 240.0),
                usableCapacityKwh = usable,
                initialOdometer = odometer.coerceAtLeast(0.0),
                currentSoc = currentSoc.coerceIn(0.0, 100.0),
                passportConsumption = passportConsumption.coerceIn(5.0, 50.0),
                isActive = false
            )
            repository.insertCar(car)
        }
    }

    fun updateCar(car: Car) {
        viewModelScope.launch(Dispatchers.IO) {
            val usable = EVCalculator.calculateUsableCapacity(car.declaredCapacityKwh)
            repository.updateCar(car.copy(usableCapacityKwh = usable))
        }
    }

    fun deleteCar(car: Car) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = allCars.value
            if (list.size > 1) {
                repository.deleteCar(car)
                val remaining = list.filter { it.id != car.id }
                if (remaining.isNotEmpty()) {
                    repository.setActiveCar(remaining.first().id)
                }
            }
        }
    }

    fun selectCar(carId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveCar(carId)
        }
    }

    fun addOperator(operator: Operator) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertOperator(operator)
        }
    }

    fun updateOperator(operator: Operator) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateOperator(operator)
        }
    }

    fun deleteOperator(operator: Operator) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteOperator(operator)
        }
    }

    fun refreshTariffsFromFile() {
        viewModelScope.launch(Dispatchers.IO) {
            DefaultTariffsLoader.syncTariffsAndCleanDuplicates(getApplication(), db)
        }
    }

    fun addTag(name: String, color: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertTag(Tag(name = name, color = color, isBuiltin = false))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTag(tag)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateLanguage(lang)
        }
    }

    fun updateTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateTheme(theme)
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateCurrency(currency)
        }
    }

    fun updateAutoNightTariff(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateAutoNightTariff(enabled)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateNotificationsEnabled(enabled)
        }
    }

    fun updateNotifyUnfinished(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateNotifyUnfinished(enabled)
        }
    }

    fun updateUnfinishedHours(hours: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateUnfinishedHours(hours)
        }
    }

    fun updateNotifyWeekly(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateNotifyWeekly(enabled)
        }
    }

    fun updateNotifyMonthly(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateNotifyMonthly(enabled)
        }
    }

    fun updateNotifyAchievements(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.updateNotifyAchievements(enabled)
        }
    }

    fun testAchievementUnlockedNotification() {
        val isEn = settings.value.language == "en"
        NotificationHelper.showAchievementUnlockedNotification(
            context = getApplication(),
            title = if (isEn) "DC Lightning" else "Молния DC",
            description = if (isEn) "Completed 3 DC fast charges" else "Провести 3 скоростные зарядки постоянным током",
            xpReward = 350,
            isEn = isEn
        )
    }

    fun testAchievementProgressNotification() {
        val isEn = settings.value.language == "en"
        NotificationHelper.showAchievementProgressNotification(
            context = getApplication(),
            title = if (isEn) "BatteryFly Master" else "Повелитель Бабочки",
            current = 1,
            total = 3,
            unit = if (isEn) "charges" else "зарядок на Бабочке",
            isEn = isEn
        )
    }

    fun sendTestNotification() {
        val curr = settings.value.currency
        val isEn = settings.value.language == "en"
        NotificationHelper.showWeeklyReportNotification(
            context = getApplication(),
            isEn = isEn,
            spentDiffPercent = -12,
            totalCost = 45.80,
            currency = curr
        )
    }

    fun testDcNotification() {
        val isEn = settings.value.language == "en"
        NotificationHelper.showDcSessionExceededNotification(
            context = getApplication(),
            isEn = isEn
        )
    }

    fun testUnfinishedNotification() {
        val isEn = settings.value.language == "en"
        NotificationHelper.showUnfinishedChargeNotification(
            context = getApplication(),
            isEn = isEn,
            hoursElapsed = settings.value.unfinishedHoursThreshold
        )
    }

    fun checkActiveSessionNotifications() {
        val session = activeSession.value ?: return
        val isEn = settings.value.language == "en"
        val durationMillis = System.currentTimeMillis() - session.startTime
        val durationHours = durationMillis / (1000.0 * 3600.0)

        if (session.stationType.equals("DC", ignoreCase = true) && durationHours >= 2.0) {
            NotificationHelper.showDcSessionExceededNotification(getApplication(), isEn)
        } else if (durationHours >= settings.value.unfinishedHoursThreshold) {
            NotificationHelper.showUnfinishedChargeNotification(getApplication(), isEn, durationHours.toInt())
        }
    }

    fun updateSession(session: ChargingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSession(session)
        }
    }

    fun deleteSession(session: ChargingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSession(session)
        }
    }

    fun addExpense(
        category: String,
        amount: Double,
        odometer: Double? = null,
        comment: String? = null,
        currency: String = settings.value.currency,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val car = activeCar.value ?: allCars.value.firstOrNull() ?: return@launch
            val exp = CarExpense(
                carId = car.id,
                category = category.trim(),
                amount = amount,
                currency = currency,
                timestamp = timestamp,
                odometer = odometer,
                comment = comment?.trim()?.ifBlank { null }
            )
            repository.insertExpense(exp)

            // Update car's odometer if expense specifies a new or higher odometer
            if (odometer != null && odometer > car.initialOdometer) {
                repository.updateCar(car.copy(initialOdometer = odometer))
            }
        }
    }

    fun updateExpense(expense: CarExpense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(expense)
            if (expense.odometer != null) {
                val car = activeCar.value ?: allCars.value.firstOrNull()
                if (car != null && expense.odometer > car.initialOdometer) {
                    repository.updateCar(car.copy(initialOdometer = expense.odometer))
                }
            }
        }
    }

    fun deleteExpense(expense: CarExpense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun saveHomeChargingSettings(
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
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.saveHomeChargingSettings(
                standardPrice = standardPrice,
                nightTariffEnabled = nightTariffEnabled,
                nightPrice = nightPrice,
                nightStartHour = nightStartHour,
                nightEndHour = nightEndHour,
                threeTariffEnabled = threeTariffEnabled,
                peakPrice = peakPrice,
                peakStartHour = peakStartHour,
                peakEndHour = peakEndHour,
                semiPeakPrice = semiPeakPrice,
                semiPeakStartHour = semiPeakStartHour,
                semiPeakEndHour = semiPeakEndHour
            )
        }
    }

    fun startQuickHomeCharge(
        currentSoc: Double,
        meterKwh: Double? = null,
        customPrice: Double? = null,
        tariffModeName: String? = null,
        isNightTariff: Boolean? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val car = activeCar.value ?: allCars.value.firstOrNull() ?: return@launch
            val appSet = settings.value
            val now = Calendar.getInstance()
            val tariffEstimate = EVCalculator.determineHomeTariff(appSet, now)
            val price = customPrice ?: tariffEstimate.pricePerKwh
            val tariffName = tariffModeName ?: tariffEstimate.tariffName
            val isNight = isNightTariff ?: tariffEstimate.isNight

            if (meterKwh != null && meterKwh > 0) {
                settingsManager.updateHomeLastMeterKwh(meterKwh)
            }

            val homeOp = allOperators.value.find { it.name.contains("Дом", ignoreCase = true) || it.name.contains("Home", ignoreCase = true) }

            val session = ChargingSession(
                carId = car.id,
                startOdometer = car.initialOdometer,
                startSoc = currentSoc.coerceIn(0.0, 100.0),
                endSoc = 100.0,
                pricePerKwh = price,
                currency = appSet.currency,
                stationType = "AC",
                operatorId = homeOp?.id,
                operatorName = "Домашняя розетка",
                operatorComment = buildString {
                    append(tariffName)
                    if (meterKwh != null && meterKwh > 0) {
                        append(" • Счётчик: $meterKwh кВт·ч")
                    }
                },
                avgPowerKw = 3.5,
                isFreeCharge = price <= 0.0001,
                nightTariffApplied = isNight,
                startTime = System.currentTimeMillis(),
                status = "active"
            )
            repository.insertSession(session)
        }
    }

    fun exportCsvData(): String {
        return CsvJsonBackupHelper.exportSessionsToCsv(allSessions.value)
    }

    fun exportJsonBackup(): String {
        return CsvJsonBackupHelper.exportFullBackupJson(
            cars = allCars.value,
            sessions = allSessions.value,
            expenses = allExpenses.value,
            operators = allOperators.value,
            tags = allTags.value,
            profile = userProfile.value
        )
    }

    fun importBackupJson(
        jsonString: String,
        replace: Boolean,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val backup = CsvJsonBackupHelper.parseFullBackupJson(jsonString)
            if (backup == null) {
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Неверный формат резервной копии")
                }
                return@launch
            }
            repository.restoreFullBackup(backup, replace)
            withContext(Dispatchers.Main) {
                onComplete?.invoke(true, "Данные успешно импортированы")
            }
        }
    }

    fun updateUserEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = userProfile.value.copy(email = email.trim().ifBlank { null })
            repository.updateUserProfile(prof)
        }
    }

    fun updateGithubToken(token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val prof = userProfile.value.copy(githubToken = token.trim().ifBlank { null })
            repository.updateUserProfile(prof)
        }
    }

    fun importCsvData(csvText: String, replace: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val carId = activeCar.value?.id ?: 1L
            val list = CsvJsonBackupHelper.parseSessionsFromCsv(csvText, carId)
            if (list.isNotEmpty()) {
                repository.restoreSessions(list, replace)
            }
        }
    }
}
