package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CrimeDetailViewModel : ViewModel() {
    private val repo = CrimeRepository.get()

    // Expose the current crime for the UI to render
    private val _crime = MutableStateFlow<Crime?>(null)
    val crime: StateFlow<Crime?> = _crime

    /** Load a crime by id (or create a placeholder if not found yet). */
    fun load(crimeId: UUID) {
        viewModelScope.launch {
            val current = repo.getCrime(crimeId).first()
            _crime.value = current ?: Crime(id = crimeId)
        }
    }

    /** Update suspect and persist. */
    fun updateSuspect(name: String?, phone: String?) {
        val current = _crime.value ?: return
        val updated = current.copy(suspect = name, suspectPhone = phone)
        _crime.value = updated
        viewModelScope.launch { repo.upsert(updated) }
    }

    /** Optional helpers if you want title/solved to persist immediately too. */
    fun updateTitle(title: String) {
        val current = _crime.value ?: return
        val updated = current.copy(title = title)
        _crime.value = updated
        viewModelScope.launch { repo.upsert(updated) }
    }

    fun updateSolved(isSolved: Boolean) {
        val current = _crime.value ?: return
        val updated = current.copy(isSolved = isSolved)
        _crime.value = updated
        viewModelScope.launch { repo.upsert(updated) }
    }
}