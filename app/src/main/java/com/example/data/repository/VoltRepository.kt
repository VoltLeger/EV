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
    suspend fun updatePinSettings(enabled: Boolean, pin: String) = settingsManager.updatePinSettings(enabled, pin)
    suspend fun updateBiometricSettings(enabled: Boolean) = settingsManager.updateBiometricSettings(enabled)

    // Backup restore
    suspend fun restoreSessions(sessions: List<ChargingSession>, replace: Boolean) {
        if (replace) {
            db.chargingSessionDao().clearAll()
        }
        for (session in sessions) {
            db.chargingSessionDao().insertSession(session.copy(id = if (replace) session.id else 0L))
        }
    }
}
