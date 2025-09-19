package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.room.Room
import com.bignerdranch.android.criminalintent.database.CrimeDao
import com.bignerdranch.android.criminalintent.database.CrimeDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CrimeRepository private constructor(context: Context) {

    // Build the Room database once
    private val database: CrimeDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            CrimeDatabase::class.java,
            "crime-database"
        )
            // dev convenience; remove when you add real migrations
            .fallbackToDestructiveMigration()
            .build()

    // <<< THIS was missing: get the DAO from the DB and keep it as a field
    private val crimeDao: CrimeDao = database.crimeDao()

    // --- Read APIs ---
    fun getCrimes(): Flow<List<Crime>> = crimeDao.getCrimes()
    fun getCrime(id: UUID): kotlinx.coroutines.flow.Flow<Crime?> {
        return crimeDao.getCrime(id)
    }
    // --- Write APIs ---
    suspend fun upsert(crime: Crime) = crimeDao.upsert(crime)
    suspend fun upsert(crimes: List<Crime>) = crimeDao.upsert(crimes)
    suspend fun delete(crime: Crime) = crimeDao.delete(crime)

    companion object {
        @Volatile private var INSTANCE: CrimeRepository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }

        fun get(): CrimeRepository =
            INSTANCE ?: throw IllegalStateException("CrimeRepository must be initialized")
    }
}