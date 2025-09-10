package com.bignerdranch.android.criminalintent.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /** Stream all crimes, newest first. */
    @Query("SELECT * FROM crimes ORDER BY date ASC")
    fun getCrimes(): Flow<List<Crime>>

    /** Stream one crime by id (null if it doesn’t exist). */
    @Query("SELECT * FROM crimes WHERE id = :id LIMIT 1")
    fun getCrime(id: UUID): Flow<Crime?>

    /** Insert or update a crime (primary key is id). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(crime: Crime)

    /** Delete a crime row. */
    @Delete
    suspend fun delete(crime: Crime)
}