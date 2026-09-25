package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CarExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface CarExpenseDao {
    @Query("SELECT * FROM car_expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<CarExpense>>

    @Query("SELECT * FROM car_expenses WHERE carId = :carId ORDER BY timestamp DESC")
    fun getExpensesForCar(carId: Long): Flow<List<CarExpense>>

    @Query("SELECT * FROM car_expenses WHERE id = :id LIMIT 1")
    suspend fun getExpenseById(id: Long): CarExpense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: CarExpense): Long

    @Update
    suspend fun updateExpense(expense: CarExpense)

    @Delete
    suspend fun deleteExpense(expense: CarExpense)

    @Query("DELETE FROM car_expenses WHERE carId = :carId")
    suspend fun deleteExpensesForCar(carId: Long)

    @Query("SELECT * FROM car_expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesDirect(): List<CarExpense>

    @Query("UPDATE car_expenses SET carId = :newCarId WHERE carId = :oldCarId")
    suspend fun reassignCarId(oldCarId: Long, newCarId: Long)

    @Query("UPDATE car_expenses SET carId = :targetCarId")
    suspend fun reassignAllExpensesToCar(targetCarId: Long)

    @Query("DELETE FROM car_expenses")
    suspend fun clearAll()
}
