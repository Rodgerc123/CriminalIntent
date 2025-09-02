package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import java.util.Date
import java.util.UUID

class CrimeListViewModel : ViewModel() {
    // Temporary in-memory data for the chapter
    val crimes: List<Crime> = List(20) { index ->
        Crime(
            id = UUID.randomUUID(),
            title = "Crime #$index",
            date = Date(),
            isSolved = index % 2 == 0
        )
    }
}