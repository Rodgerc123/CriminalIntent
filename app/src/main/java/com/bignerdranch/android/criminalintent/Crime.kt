package com.bignerdranch.android.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity(tableName = "crime") // <- must match DAO queries
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val date: Date = Date(),
    val isSolved: Boolean = false,
    val suspect: String? = null,
    val suspectPhone: String? = null,
    val requiresPolice: Boolean = false
)

