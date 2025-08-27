package com.bignerdranch.android.criminalintent

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment

class CrimeDetailFragment : Fragment(R.layout.fragment_crime_detail) {
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox

    private var crime: Crime = Crime()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        solvedCheckBox = view.findViewById(R.id.crime_solved)

        // Initial binding
        dateButton.text = crime.date.toString()
        dateButton.isEnabled = false  // we'll enable with DatePicker later

        titleField.doAfterTextChanged { crime.title = it?.toString().orEmpty() }
        solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
        }
    }
}