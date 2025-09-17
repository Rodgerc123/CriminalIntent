package com.bignerdranch.android.criminalintent.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.bignerdranch.android.criminalintent.Crime
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * DAO = Data Access Object.
 * You only declare *what* queries exist. Room generates the implementation.
 *
 * Notes:
 * - Read queries return Flow so the UI can observe updates.
 * - Write queries (insert/update/delete) are suspend functions (call from coroutines).
 */
@Dao
interface CrimeDao {
    @Query("SELECT * FROM crime")
    fun getCrimes(): Flow<List<Crime>>

    @Query("SELECT * FROM crime WHERE id = :id LIMIT 1")
    fun getCrime(id: UUID): Flow<Crime?>

    @Query("DELETE FROM crime WHERE id = :id")
    suspend fun deleteById(id: UUID)

    // You can keep these if used elsewhere
    @Insert
    suspend fun insertCrime(crime: Crime)

    @Update
    suspend fun updateCrime(crime: Crime)

    // >>>New: true upsert (insert or update on conflict)
    @Upsert
    suspend fun upsert(crime: Crime)

    // (optional convenience)
    @Upsert
    suspend fun upsert(crimes: List<Crime>)

    @Delete
    suspend fun delete(crime: Crime)
}