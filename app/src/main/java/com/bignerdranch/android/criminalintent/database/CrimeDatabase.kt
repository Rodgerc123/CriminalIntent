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
    version = 2,                 // bumped from 1 → 2
    exportSchema = true
)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase : RoomDatabase() {
    abstract fun crimeDao(): CrimeDao
}