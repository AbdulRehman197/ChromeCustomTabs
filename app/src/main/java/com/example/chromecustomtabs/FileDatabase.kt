package com.example.chromecustomtabs

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [FIleSHA::class], version = 1)
abstract class FileDatabase: RoomDatabase() {
    abstract  fun fileshaDao(): FileDAO
}