package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChargingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface ChargingSessionDao {
    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions WHERE carId = :carId ORDER BY startTime DESC")
    fun getSessionsForCar(carId: Long): Flow<List<ChargingSession>>

    @Query("SELECT * FROM charging_sessions WHERE status = 'active' LIMIT 1")
    fun getActiveSession(): Flow<ChargingSession?>

    @Query("SELECT * FROM charging_sessions WHERE carId = :carId AND status = 'active' LIMIT 1")
    fun getActiveSessionForCar(carId: Long): Flow<ChargingSession?>

    @Query("SELECT * FROM charging_sessions WHERE id = :id LIMIT 1")
    fun getSessionById(id: Long): Flow<ChargingSession?>

    @Query("SELECT * FROM charging_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionByIdDirect(id: Long): ChargingSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChargingSession): Long

    @Update
    suspend fun updateSession(session: ChargingSession)

    @Delete
    suspend fun deleteSession(session: ChargingSession)

    @Query("DELETE FROM charging_sessions WHERE carId = :carId")
    suspend fun deleteSessionsForCar(carId: Long)

    @Query("SELECT COUNT(*) FROM charging_sessions")
    suspend fun countAllSessions(): Int

    @Query("SELECT * FROM charging_sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsDirect(): List<ChargingSession>

    @Query("UPDATE charging_sessions SET carId = :newCarId WHERE carId = :oldCarId")
    suspend fun reassignCarId(oldCarId: Long, newCarId: Long)

    @Query("UPDATE charging_sessions SET carId = :targetCarId")
    suspend fun reassignAllSessionsToCar(targetCarId: Long)

    @Query("DELETE FROM charging_sessions")
    suspend fun clearAll()
}
