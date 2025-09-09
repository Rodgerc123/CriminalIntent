package com.bignerdranch.android.criminalintent.database

import androidx.room.TypeConverter
import java.util.Date
import java.util.UUID

/**
 * Room can only persist a limited set of simple types.
 * We store Date as a Long (epoch millis) and UUID as a String.
 * These @TypeConverter functions teach Room how to go back & forth.
 */
class CrimeTypeConverters {

    // ---- Date <-> Long ----

    /** Convert Date -> Long (millis since epoch) for storing in DB columns */
    @TypeConverter
    fun fromDate(date: Date?): Long? = date?.time

    /** Convert Long (millis) -> Date when reading from DB columns */
    @TypeConverter
    fun toDate(millis: Long?): Date? = millis?.let { Date(it) }

    // ---- UUID <-> String ----

    /** Convert UUID -> String for storing in DB columns */
    @TypeConverter
    fun fromUUID(id: UUID?): String? = id?.toString()

    /** Convert String -> UUID when reading from DB columns */
    @TypeConverter
    fun toUUID(id: String?): UUID? = id?.let(UUID::fromString)
}