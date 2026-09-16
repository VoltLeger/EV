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
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.data.repository.VoltRepository
import com.example.util.AwardCalculator
import com.example.util.CsvJsonBackupHelper
import com.example.util.DefaultTariffsLoader
import com.example.util.EVCalculator
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

    // Current month consumption:
    val monthAvgConsumption: StateFlow<Double?> = combine(allSessions, activeCar) { sessions, car ->
        val carId = car?.id ?: return@combine null
        val carSessions = sessions.filter { it.carId == carId && it.status == "completed" }
        val now = Calendar.getInstance()
        EVCalculator.calculateMonthConsumption(carSessions, now.get(Calendar.YEAR), now.get(Calendar.MONTH))
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
        endTime: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val energyCost = kwhDelivered * session.pricePerKwh
            val totalCost = energyCost + penaltyCost + fixedAmount

            val updatedSession = session.copy(
                endSoc = endSoc,
                kwhDeliveredByStation = kwhDelivered,
                kwhReceivedByCar = kwhReceived,
                energyCost = energyCost,
                penaltyCost = penaltyCost,
                fixedAmount = fixedAmount,
                totalCost = totalCost,
                endTime = endTime,
                status = "completed"
            )
            repository.updateSession(updatedSession)

            // Update car's current SOC and odometer
            val car = activeCar.value ?: allCars.value.find { it.id == session.carId }
            if (car != null) {
                val newOdo = maxOf(car.initialOdometer, session.startOdometer)
                repository.updateCar(
                    car.copy(
                        currentSoc = endSoc,
                        initialOdometer = newOdo
                    )
                )
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

    fun updatePinSettings(enabled: Boolean, pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updatePinSettings(enabled, pin)
        }
    }

    fun startQuickHomeCharge(targetSoc: Double = 100.0, customPrice: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val car = activeCar.value ?: allCars.value.firstOrNull() ?: return@launch
            val homeOp = allOperators.value.find { it.name.contains("Дом", ignoreCase = true) || it.name.contains("Home", ignoreCase = true) }
            val now = Calendar.getInstance()
            val isNight = homeOp != null && EVCalculator.isNightTariffTime(homeOp.nightStartHour, homeOp.nightEndHour, now)
            val price = customPrice ?: (if (isNight && homeOp != null) (homeOp.nightPriceAc ?: homeOp.priceAc) else (homeOp?.priceAc ?: 0.25))

            val session = ChargingSession(
                carId = car.id,
                startOdometer = car.initialOdometer,
                startSoc = car.currentSoc,
                endSoc = targetSoc,
                pricePerKwh = price,
                currency = settings.value.currency,
                stationType = "AC",
                operatorId = homeOp?.id,
                operatorName = homeOp?.name ?: "Домашняя розетка (AC)",
                operatorComment = "Быстрый ввод в 1 тап",
                avgPowerKw = 3.7,
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
            operators = allOperators.value,
            tags = allTags.value
        )
    }

    fun importBackupJson(jsonString: String, replace: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val backup = CsvJsonBackupHelper.parseFullBackupJson(jsonString) ?: return@launch
            if (backup.sessions.isNotEmpty()) {
                repository.restoreSessions(backup.sessions, replace)
            }
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
