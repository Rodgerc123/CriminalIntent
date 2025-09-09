package com.bignerdranch.android.criminalintent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bignerdranch.android.criminalintent.Crime

/**
 * Main Room database for the app.
 * - Lists the entities to persist (Crime)
 * - Registers our TypeConverters so Room knows how to store Date/UUID
 */
@Database(
    entities = [Crime::class],
    version = 1,
    exportSchema = true // good practice for migrations later
)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase : RoomDatabase() {
    // Room generates the implementation and returns it here
    abstract fun crimeDao(): CrimeDao
}