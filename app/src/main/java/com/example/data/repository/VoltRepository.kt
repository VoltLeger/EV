package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SettingsManager
import com.example.data.model.AppSettings
import com.example.data.model.Car
import com.example.data.model.CarExpense
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import kotlinx.coroutines.flow.Flow

class VoltRepository(
    private val db: AppDatabase,
    private val settingsManager: SettingsManager
) {
    val allCars: Flow<List<Car>> = db.carDao().getAllCars()
    val activeCar: Flow<Car?> = db.carDao().getActiveCar()
    val allOperators: Flow<List<Operator>> = db.operatorDao().getAllOperators()
    val allTags: Flow<List<Tag>> = db.tagDao().getAllTags()
    val appSettings: Flow<AppSettings> = settingsManager.settingsFlow
    val activeSession: Flow<ChargingSession?> = db.chargingSessionDao().getActiveSession()
    val userProfile: Flow<UserProfile?> = db.userProfileDao().getUserProfile()
    val allExpenses: Flow<List<CarExpense>> = db.carExpenseDao().getAllExpenses()

    fun getExpensesForCar(carId: Long): Flow<List<CarExpense>> =
        db.carExpenseDao().getExpensesForCar(carId)

    suspend fun insertExpense(expense: CarExpense): Long =
        db.carExpenseDao().insertExpense(expense)

    suspend fun updateExpense(expense: CarExpense) =
        db.carExpenseDao().updateExpense(expense)

    suspend fun deleteExpense(expense: CarExpense) =
        db.carExpenseDao().deleteExpense(expense)

    suspend fun getOrCreateUserProfile(): UserProfile {
        val existing = db.userProfileDao().getUserProfileDirect()
        if (existing != null) return existing
        val newProfile = UserProfile()
        db.userProfileDao().insertOrUpdate(newProfile)
        return newProfile
    }

    suspend fun updateUserProfile(profile: UserProfile) {
        db.userProfileDao().insertOrUpdate(profile)
    }

    fun getSessionsForCar(carId: Long): Flow<List<ChargingSession>> =
        db.chargingSessionDao().getSessionsForCar(carId)

    fun getAllSessions(): Flow<List<ChargingSession>> =
        db.chargingSessionDao().getAllSessions()

    fun getActiveSessionForCar(carId: Long): Flow<ChargingSession?> =
        db.chargingSessionDao().getActiveSessionForCar(carId)

    suspend fun getCarByIdDirect(id: Long): Car? = db.carDao().getCarByIdDirect(id)

    suspend fun insertCar(car: Car): Long = db.carDao().insertCar(car)

    suspend fun updateCar(car: Car) = db.carDao().updateCar(car)

    suspend fun deleteCar(car: Car) {
        db.chargingSessionDao().deleteSessionsForCar(car.id)
        db.carDao().deleteCar(car)
    }

    suspend fun setActiveCar(carId: Long) {
        db.carDao().setActiveCar(carId)
        settingsManager.updateSelectedCarId(carId)
    }

    suspend fun insertSession(session: ChargingSession): Long =
        db.chargingSessionDao().insertSession(session)

    suspend fun updateSession(session: ChargingSession) =
        db.chargingSessionDao().updateSession(session)

    suspend fun deleteSession(session: ChargingSession) =
        db.chargingSessionDao().deleteSession(session)

    suspend fun insertOperator(operator: Operator): Long =
        db.operatorDao().insertOperator(operator)

    suspend fun updateOperator(operator: Operator) =
        db.operatorDao().updateOperator(operator)

    suspend fun deleteOperator(operator: Operator) =
        db.operatorDao().deleteOperator(operator)

    suspend fun insertTag(tag: Tag): Long =
        db.tagDao().insertTag(tag)

    suspend fun updateTag(tag: Tag) =
        db.tagDao().updateTag(tag)

    suspend fun deleteTag(tag: Tag) =
        db.tagDao().deleteTag(tag)

    // Settings
    suspend fun updateLanguage(language: String) = settingsManager.updateLanguage(language)
    suspend fun updateTheme(theme: String) = settingsManager.updateTheme(theme)
    suspend fun updateCurrency(currency: String) = settingsManager.updateCurrency(currency)
    suspend fun updateAutoNightTariff(enabled: Boolean) = settingsManager.updateAutoNightTariff(enabled)
    suspend fun updateNotificationsEnabled(enabled: Boolean) = settingsManager.updateNotificationsEnabled(enabled)
    suspend fun updateNotifyUnfinished(enabled: Boolean) = settingsManager.updateNotifyUnfinished(enabled)
    suspend fun updateUnfinishedHours(hours: Int) = settingsManager.updateUnfinishedHours(hours)
    suspend fun updateNotifyWeekly(enabled: Boolean) = settingsManager.updateNotifyWeekly(enabled)
    suspend fun updateNotifyMonthly(enabled: Boolean) = settingsManager.updateNotifyMonthly(enabled)
    suspend fun updateNotifyAchievements(enabled: Boolean) = settingsManager.updateNotifyAchievements(enabled)
    suspend fun setOnboardingCompleted(completed: Boolean) = settingsManager.setOnboardingCompleted(completed)

    // Backup restore
    data class RestoreResult(
        val success: Boolean,
        val sessionsCount: Int,
        val carsCount: Int,
        val expensesCount: Int,
        val operatorsCount: Int,
        val message: String
    )

    suspend fun restoreSessions(sessions: List<ChargingSession>, replace: Boolean): RestoreResult {
        return restoreFullBackup(
            data = com.example.util.BackupData(sessions = sessions),
            replace = replace
        )
    }

    suspend fun restoreFullBackup(
        data: com.example.util.BackupData,
        replace: Boolean,
        targetCarId: Long? = null
    ): RestoreResult {
        try {
            if (replace) {
                db.chargingSessionDao().clearAll()
                db.carExpenseDao().clearAll()
                db.carDao().clearAll()
                db.operatorDao().clearAll()
                db.tagDao().clearAll()
            }

            if (data.profile != null) {
                db.userProfileDao().insertOrUpdate(data.profile)
            }

            val carIdMap = mutableMapOf<Long, Long>()

            // 1. Process Cars
            if (replace) {
                for (car in data.cars) {
                    val originalId = car.id
                    val insertedId = db.carDao().insertCar(car)
                    val effectiveId = if (insertedId > 0) insertedId else originalId
                    if (originalId > 0) {
                        carIdMap[originalId] = effectiveId
                    }
                }
            } else {
                val existingCars = db.carDao().getAllCarsList()
                for (car in data.cars) {
                    val originalId = car.id
                    val matchedCar = existingCars.find {
                        (it.vin != null && it.vin == car.vin) ||
                                (it.licensePlate != null && it.licensePlate == car.licensePlate) ||
                                it.name.equals(car.name, ignoreCase = true)
                    }
                    val effectiveId = if (matchedCar != null) {
                        matchedCar.id
                    } else {
                        db.carDao().insertCar(car.copy(id = 0L))
                    }
                    if (originalId > 0) {
                        carIdMap[originalId] = effectiveId
                    }
                }
            }

            // Ensure at least one car exists in DB
            val allCurrentCars = db.carDao().getAllCarsList()
            val fallbackCarId: Long = when {
                targetCarId != null && targetCarId > 0L -> targetCarId
                carIdMap.values.isNotEmpty() -> carIdMap.values.first()
                allCurrentCars.isNotEmpty() -> (allCurrentCars.find { it.isActive } ?: allCurrentCars.first()).id
                else -> {
                    db.carDao().insertCar(
                        Car(
                            name = "Мой Электромобиль",
                            declaredCapacityKwh = 60.0,
                            usableCapacityKwh = 57.0,
                            initialOdometer = 0.0,
                            currentSoc = 60.0,
                            passportConsumption = 16.0,
                            isActive = true
                        )
                    )
                }
            }

            // Update active car in settings if restored
            if (allCurrentCars.isNotEmpty() || carIdMap.isNotEmpty()) {
                val activeCarIdToSet = carIdMap.values.firstOrNull() ?: fallbackCarId
                db.carDao().setActiveCar(activeCarIdToSet)
                settingsManager.updateSelectedCarId(activeCarIdToSet)
            }

            // 2. Process Operators
            for (op in data.operators) {
                if (replace) {
                    db.operatorDao().insertOperator(op)
                } else {
                    val existing = db.operatorDao().getOperatorByName(op.name)
                    if (existing == null) {
                        db.operatorDao().insertOperator(op.copy(id = 0L))
                    }
                }
            }

            // 3. Process Tags
            for (tag in data.tags) {
                if (replace) {
                    db.tagDao().insertTag(tag)
                } else {
                    val existing = db.tagDao().getTagByName(tag.name)
                    if (existing == null) {
                        db.tagDao().insertTag(tag.copy(id = 0L))
                    }
                }
            }

            // 4. Process Sessions
            var sessionsInserted = 0
            for (session in data.sessions) {
                val resolvedCarId = carIdMap[session.carId]
                    ?: if (allCurrentCars.size <= 1) fallbackCarId
                    else if (session.carId in allCurrentCars.map { it.id }) session.carId
                    else fallbackCarId

                val sessionToInsert = session.copy(
                    id = if (replace) session.id else 0L,
                    carId = resolvedCarId
                )
                db.chargingSessionDao().insertSession(sessionToInsert)
                sessionsInserted++
            }

            // 5. Process Expenses
            var expensesInserted = 0
            for (expense in data.expenses) {
                val resolvedCarId = carIdMap[expense.carId]
                    ?: if (allCurrentCars.size <= 1) fallbackCarId
                    else if (expense.carId in allCurrentCars.map { it.id }) expense.carId
                    else fallbackCarId

                val expenseToInsert = expense.copy(
                    id = if (replace) expense.id else 0L,
                    carId = resolvedCarId
                )
                db.carExpenseDao().insertExpense(expenseToInsert)
                expensesInserted++
            }

            return RestoreResult(
                success = true,
                sessionsCount = sessionsInserted,
                carsCount = if (data.cars.isNotEmpty()) data.cars.size else (if (allCurrentCars.isNotEmpty()) allCurrentCars.size else 1),
                expensesCount = expensesInserted,
                operatorsCount = data.operators.size,
                message = "Успешно восстановлено: $sessionsInserted зарядок, ${data.cars.size} авто, $expensesInserted трат"
            )
        } catch (e: Exception) {
            return RestoreResult(
                success = false,
                sessionsCount = 0,
                carsCount = 0,
                expensesCount = 0,
                operatorsCount = 0,
                message = "Ошибка при восстановлении: ${e.message ?: "неизвестная ошибка"}"
            )
        }
    }

    suspend fun reassignAllSessionsToCar(targetCarId: Long): Int {
        db.chargingSessionDao().reassignAllSessionsToCar(targetCarId)
        db.carExpenseDao().reassignAllExpensesToCar(targetCarId)
        return db.chargingSessionDao().countAllSessions()
    }

    suspend fun fixOrphanSessions(): Int {
        val cars = db.carDao().getAllCarsList()
        if (cars.isEmpty()) return 0
        val validCarIds = cars.map { it.id }.toSet()
        val targetCarId = (cars.find { it.isActive } ?: cars.first()).id
        val allSessions = db.chargingSessionDao().getAllSessionsDirect()
        var fixed = 0
        for (s in allSessions) {
            if (s.carId !in validCarIds) {
                db.chargingSessionDao().reassignCarId(s.carId, targetCarId)
                fixed++
            }
        }
        val allExpenses = db.carExpenseDao().getAllExpensesDirect()
        for (e in allExpenses) {
            if (e.carId !in validCarIds) {
                db.carExpenseDao().reassignCarId(e.carId, targetCarId)
            }
        }
        return fixed
    }
}
