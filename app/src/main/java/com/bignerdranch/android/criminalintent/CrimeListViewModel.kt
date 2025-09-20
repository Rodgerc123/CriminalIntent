package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel exposes crimes as a StateFlow<List<Crime>> sourced from Room via the repository.
 * Also provides add/delete operations using coroutines.
 */
class CrimeListViewModel : ViewModel() {

    private val repo = CrimeRepository.get()

    // Emits the latest list of crimes and updates automatically when DB changes.
    // initialValue = emptyList() so UI can render immediately.
    val crimes: StateFlow<List<Crime>> =
        repo.getCrimes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    suspend fun addCrime(): java.util.UUID {
        val crime = Crime(
            id = java.util.UUID.randomUUID(),
            title = "",
            date = java.util.Date(),
            isSolved = false,
            requiresPolice = false
        )
        CrimeRepository.get().upsert(crime)
        return crime.id
    }

    fun seedIfEmpty() {
        viewModelScope.launch {
            val repo = CrimeRepository.get()
            val current = repo.getCrimes().firstOrNull().orEmpty()
            if (current.isEmpty()) {
                repeat(20) { i ->
                    repo.upsert(
                        Crime(
                            title = "Crime #$i",
                            isSolved = i % 2 == 0,
                            requiresPolice = i % 5 == 0
                        )
                    )
                }
            }
        }
    }

    /** Insert/update a crime (Room upsert). */
    fun addCrime(title: String, requiresPolice: Boolean = false) {
        viewModelScope.launch {
            repo.upsert(Crime(title = title, requiresPolice = requiresPolice))
        }
    }

    /** Delete a crime. */
    fun deleteCrime(crime: Crime) {
        viewModelScope.launch {
            repo.delete(crime)
        }
    }



}