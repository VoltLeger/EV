package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Operator
import kotlinx.coroutines.flow.Flow

@Dao
interface OperatorDao {
    @Query("SELECT * FROM operators ORDER BY isBuiltin DESC, name ASC")
    fun getAllOperators(): Flow<List<Operator>>

    @Query("SELECT * FROM operators WHERE id = :id LIMIT 1")
    fun getOperatorById(id: Long): Flow<Operator?>

    @Query("SELECT * FROM operators WHERE name = :name LIMIT 1")
    suspend fun getOperatorByName(name: String): Operator?

    @Query("DELETE FROM operators WHERE id NOT IN (SELECT MIN(id) FROM operators GROUP BY name)")
    suspend fun deleteDuplicateOperators()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperator(operator: Operator): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operators: List<Operator>)

    @Update
    suspend fun updateOperator(operator: Operator)

    @Delete
    suspend fun deleteOperator(operator: Operator)

    @Query("SELECT COUNT(*) FROM operators")
    suspend fun countOperators(): Int
}
