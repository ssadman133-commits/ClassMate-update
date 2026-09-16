package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    private val fullDateTimeFormat = SimpleDateFormat("MMMM d, yyyy\nh:mm a", Locale.getDefault())
    private val singleLineDateTimeFormat = SimpleDateFormat("MMMM d, yyyy • h:mm a", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun formatFullDateTime(timestamp: Long): String {
        return fullDateTimeFormat.format(Date(timestamp))
    }

    fun formatSingleLineDateTime(timestamp: Long): String {
        return singleLineDateTimeFormat.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }
}
