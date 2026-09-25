package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Car
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDao {
    @Query("SELECT * FROM cars ORDER BY createdAt ASC")
    fun getAllCars(): Flow<List<Car>>

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    fun getCarById(id: Long): Flow<Car?>

    @Query("SELECT * FROM cars WHERE id = :id LIMIT 1")
    suspend fun getCarByIdDirect(id: Long): Car?

    @Query("SELECT * FROM cars WHERE isActive = 1 LIMIT 1")
    fun getActiveCar(): Flow<Car?>

    @Query("SELECT * FROM cars ORDER BY createdAt ASC")
    suspend fun getAllCarsList(): List<Car>

    @Query("SELECT COUNT(*) FROM cars")
    suspend fun countCars(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCar(car: Car): Long

    @Update
    suspend fun updateCar(car: Car)

    @Delete
    suspend fun deleteCar(car: Car)

    @Query("UPDATE cars SET isActive = CASE WHEN id = :carId THEN 1 ELSE 0 END")
    suspend fun setActiveCar(carId: Long)

    @Query("DELETE FROM cars")
    suspend fun clearAll()
}
