package com.bignerdranch.android.criminalintent

import android.app.Application

/**
 * App-level entry point. Runs before any Activity/Fragment.
 * We use it to initialize singletons (e.g., CrimeRepository).
 */
class CriminalIntentApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the repository singleton once for the whole process.
        CrimeRepository.initialize(this)
    }
}