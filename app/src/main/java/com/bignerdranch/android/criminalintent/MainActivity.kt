package com.bignerdranch.android.criminalintent

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * With Navigation Component, MainActivity just hosts the NavHostFragment
 * defined in res/layout/activity_main.xml. No fragment callbacks needed.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // contains FragmentContainerView(navHost)
    }
}