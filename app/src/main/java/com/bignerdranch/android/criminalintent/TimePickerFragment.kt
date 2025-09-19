package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import java.util.Calendar

class TimePickerFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val cal = Calendar.getInstance().apply {
            timeInMillis = requireArguments().getLong(ARG_DATE_MILLIS, System.currentTimeMillis())
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        return TimePickerDialog(
            requireContext(),
            { _, h, m ->
                // return the picked hour/min via Fragment Result
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(KEY_HOUR to h, KEY_MINUTE to m)
                )
            },
            hour,
            minute,
            android.text.format.DateFormat.is24HourFormat(requireContext())
        )
    }

    companion object {
        const val REQUEST_KEY = "TIME_PICKER_REQUEST"
        const val KEY_HOUR = "TIME_PICKER_HOUR"
        const val KEY_MINUTE = "TIME_PICKER_MINUTE"
        private const val ARG_DATE_MILLIS = "ARG_TIME_INIT_MILLIS"

        fun newInstance(initMillis: Long): TimePickerFragment =
            TimePickerFragment().apply {
                arguments = bundleOf(ARG_DATE_MILLIS to initMillis)
            }
    }
}