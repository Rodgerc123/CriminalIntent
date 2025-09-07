package com.bignerdranch.android.criminalintent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class MainActivity : AppCompatActivity(), CrimeListFragment.Callbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // The FragmentContainerView in activity_main.xml already hosts CrimeListFragment via android:name.
    }

    /** Called when a list row is tapped. Replace the list with the detail screen. */
    override fun onCrimeSelected(crimeId: UUID) {
        val fragment = CrimeDetailFragment.newInstance(crimeId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Back returns to list
            .commit()
    }
}