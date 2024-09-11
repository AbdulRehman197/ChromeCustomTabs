package com.example.chromecustomtabs

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "fileshas")
data class FIleSHA(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val filename: String,
    val shavalue: String
    
)
