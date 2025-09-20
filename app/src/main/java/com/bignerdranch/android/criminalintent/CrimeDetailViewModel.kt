package com.bignerdranch.android.criminalintent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * Detail VM exposes a single Crime as StateFlow and provides update helpers
 * that (1) update UI state immediately and (2) persist to Room.
 */
class CrimeDetailViewModel : ViewModel() {

    private val repo get() = CrimeRepository.get()

    // Backing state for the currently loaded Crime
    private val _crime = MutableStateFlow<Crime?>(null)
    val crime: StateFlow<Crime?> = _crime

    /**
     * Begin observing this crime by id. Keeps _crime in sync with DB.
     */
    fun load(id: UUID) {
        viewModelScope.launch {
            repo.getCrime(id)               // <-- NOTE: getCrime, not getCrimeFlow
                .collect { fromDb ->
                    _crime.value = fromDb
                }
        }
    }

    // ---- Update helpers (optimistic UI, then persist) ----

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

    fun updateDate(date: Date) {
        val current = _crime.value ?: return
        val updated = current.copy(date = date)
        _crime.value = updated
        viewModelScope.launch { repo.upsert(updated) }
    }

    fun updateSuspect(name: String?, phone: String?) {
        val current = crime.value ?: return
        val updated = current.copy(
            suspect = name.orEmpty(),   // <-- coalesce nullable to non-null
            suspectPhone = phone        // still nullable in your schema
        )

        _crime.value = updated
        viewModelScope.launch { repo.upsert(updated) }
    }
    fun deleteCurrent() {
        val current = crime.value ?: return
        viewModelScope.launch {
            CrimeRepository.get().delete(current)
        }
    }
}