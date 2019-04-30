package vova.com.primegenerator.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "numbers")
data class Number(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "primeNumber") var primeNumber: Int
)