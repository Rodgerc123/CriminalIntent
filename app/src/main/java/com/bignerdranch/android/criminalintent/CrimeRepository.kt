package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bignerdranch.android.criminalintent.database.CrimeDao
import com.bignerdranch.android.criminalintent.database.CrimeDatabase
import com.bignerdranch.android.criminalintent.database.MIGRATION_1_2
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class CrimeRepository private constructor(context: Context) {

    // --- Migration(s) ---
    // v2 -> v3: make suspect NON-NULL with default '', keep types aligned with entity
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create a new table with the desired NOT NULL / defaults
            db.execSQL("""
            CREATE TABLE IF NOT EXISTS crime_new (
                id TEXT NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                date INTEGER NOT NULL,
                isSolved INTEGER NOT NULL,
                suspect TEXT NOT NULL DEFAULT '',
                suspectPhone TEXT,
                requiresPolice INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

            // Copy data over, coalescing suspect from NULL -> ''
            db.execSQL("""
            INSERT INTO crime_new (id, title, date, isSolved, suspect, suspectPhone, requiresPolice)
            SELECT id, title, date, isSolved, COALESCE(suspect, ''), suspectPhone, requiresPolice
            FROM crime
        """.trimIndent())

            // Swap tables
            db.execSQL("DROP TABLE crime")
            db.execSQL("ALTER TABLE crime_new RENAME TO crime")
        }
    }

    // --- Build the Room database once for the app ---
    private val database: CrimeDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            CrimeDatabase::class.java,
            "crime-database"
        )
            // Attach real migrations here (remove destructive fallback)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            // .fallbackToDestructiveMigration() // keep commented out once confident
            .build()

    // Keep a DAO reference
    private val crimeDao: CrimeDao = database.crimeDao()

    // --- Read APIs ---
    fun getCrimes(): Flow<List<Crime>> = crimeDao.getCrimes()
    fun getCrime(id: UUID): kotlinx.coroutines.flow.Flow<Crime?> = crimeDao.getCrime(id)

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