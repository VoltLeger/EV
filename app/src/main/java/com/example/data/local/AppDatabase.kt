package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Car
import com.example.data.model.ChargingSession
import com.example.data.model.Operator
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.util.DefaultTariffsLoader

@Database(
    entities = [Car::class, ChargingSession::class, Operator::class, Tag::class, UserProfile::class],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun carDao(): CarDao
    abstract fun chargingSessionDao(): ChargingSessionDao
    abstract fun operatorDao(): OperatorDao
    abstract fun tagDao(): TagDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "voltledger_database"
                )
                    .fallbackToDestructiveMigration()
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

suspend fun seedDefaultData(context: Context, db: AppDatabase) {
    DefaultTariffsLoader.syncTariffsAndCleanDuplicates(context, db)
    if (db.carDao().countCars() == 0) {
        db.carDao().insertCar(
            Car(
                name = "Мой Электромобиль",
                declaredCapacityKwh = 70.0,
                usableCapacityKwh = 66.5,
                initialOdometer = 15000.0,
                currentSoc = 80.0,
                passportConsumption = 16.0,
                isActive = true
            )
        )
    }
}
