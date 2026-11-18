package com.homecontrol.app.util

import java.util.Date
import java.util.Locale

object DateFormatter {
    /** Formats a timestamp using the Persian (Jalali) calendar. Falls back to Gregorian. */
    fun format(timestamp: Long): String = try {
        val fmt = android.icu.text.SimpleDateFormat(
            "yyyy/MM/dd - HH:mm",
            android.icu.util.ULocale("fa_IR@calendar=persian")
        )
        fmt.format(Date(timestamp))
    } catch (e: Exception) {
        java.text.SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(timestamp))
    }
}
