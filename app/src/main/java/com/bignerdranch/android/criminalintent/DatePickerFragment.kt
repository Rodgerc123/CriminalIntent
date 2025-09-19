package com.bignerdranch.android.criminalintent

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar
import java.util.Date

class DatePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Default to "now" if no date was passed
        val millis = requireArguments().getLong(ARG_DATE_MILLIS, System.currentTimeMillis())
        val cal = Calendar.getInstance().apply { time = Date(millis) }

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)            // 0-based!
        val day = cal.get(Calendar.DAY_OF_MONTH)

        // Build DatePickerDialog initialized to current (or passed) date.
        return DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                // Return result via Fragment Result API
                val pickedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, y)
                    set(Calendar.MONTH, m)
                    set(Calendar.DAY_OF_MONTH, d)
                    // Keep hour/min/sec as-is (optional). Zeroing is also fine:
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(BUNDLE_KEY_DATE_MILLIS to pickedCal.timeInMillis)
                )
            },
            year, month, day
        )
    }

    companion object {
        const val REQUEST_KEY = "DATE_PICKER_REQUEST"
        const val BUNDLE_KEY_DATE_MILLIS = "DATE_PICKER_RESULT_MILLIS"
        private const val ARG_DATE_MILLIS = "ARG_DATE_MILLIS"

        fun newInstance(initialDate: Date): DatePickerFragment =
            DatePickerFragment().apply {
                arguments = bundleOf(ARG_DATE_MILLIS to initialDate.time)
            }
    }
}