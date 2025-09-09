package com.bignerdranch.android.criminalintent.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Extension function on java.util.Date that returns an AU-formatted string.
 *
 * withWeekday = false → "11 May 2022"
 * withWeekday = true  → "Wednesday, 11 May 2022"
 *
 * We use Android's DateFormat.getBestDateTimePattern with an AU locale so
 * ordering is day → month → year.
 */
fun Date.toAuDisplayString(context: Context, withWeekday: Boolean = false): String {
    // Use a fixed AU locale to always get Australian ordering.
    // If you prefer device locale, swap to: val locale = Locale.getDefault()
    val locale = Locale("en", "AU")

    // Skeletons describe the *components*; Android picks the right local pattern.
    // dMMMMy      -> e.g., 11 May 2022
    // EEEEdMMMMy  -> e.g., Wednesday, 11 May 2022
    val skeleton = if (withWeekday) "EEEEdMMMMy" else "dMMMMy"

    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(this)
}
