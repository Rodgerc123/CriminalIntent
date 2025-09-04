package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import java.util.Date
import java.util.UUID

class CrimeListViewModel : ViewModel() {
    // Temporary in-memory data for the chapter
    val crimes: List<Crime> = List(20) { i ->
        Crime(
            title = "Crime #$i",
            isSolved = i % 2 == 0,
            requiresPolice = i % 5 == 0   // every 5th one is “serious”, for example
        )
    }
}