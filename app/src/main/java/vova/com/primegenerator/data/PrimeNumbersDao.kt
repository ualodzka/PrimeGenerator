package vova.com.primegenerator.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface PrimeNumbersDao {

    @Query("SELECT * from numbers")
    fun getAll(): List<Number>

    @Insert(onConflict = REPLACE)
    fun insert(numbers: List<Number>)


    @Query("DELETE from numbers")
    fun deleteAll()
}