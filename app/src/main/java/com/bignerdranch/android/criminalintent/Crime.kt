package com.bignerdranch.android.criminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class Crime(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val title: String = "",
    val date: Date = Date(),
    val isSolved: Boolean = false,
    val suspect: String = "",          // <-- default (book uses empty string)
    val suspectPhone: String? = null,  // <-- keep nullable default
    val requiresPolice: Boolean = false
)
