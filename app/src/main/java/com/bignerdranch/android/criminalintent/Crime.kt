package com.bignerdranch.android.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

/**
 * Room entity stored in table "crimes".
 * - id is the primary key (UUID converted via TypeConverters)
 * - date is stored as epoch millis via TypeConverters
 */
@Entity(tableName = "crimes")
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val date: Date = Date(),
    val isSolved: Boolean = false,
    val requiresPolice: Boolean = false
)