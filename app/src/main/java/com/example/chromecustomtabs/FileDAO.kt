package com.example.chromecustomtabs


import androidx.room.*

@Dao
interface FileDAO {
    @Insert
    suspend fun insertSha(filesha: FIleSHA)
    @Update
    suspend fun updateSha(filesha: FIleSHA)
    @Delete
    suspend fun deleteSha(filesha: FIleSHA)
    @Query("SELECT * FROM fileshas WHERE shavalue = :sha256")
    suspend fun getFileSha(sha256: String) : FIleSHA
    @Query("DELETE FROM fileshas")
    suspend fun deleteAll()
}