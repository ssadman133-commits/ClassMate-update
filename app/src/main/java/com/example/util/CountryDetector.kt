package com.example.util

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale
import java.util.TimeZone

object CountryDetector {

    /**
     * Determines whether the user is located in Bangladesh.
     * Checks multiple device-level signals:
     * 1. Active Mobile Network Country ISO ("bd")
     * 2. SIM Card Country ISO ("bd")
     * 3. System Locale Country code ("BD")
     * 4. System Timezone ("Asia/Dhaka")
     */
    fun isBangladeshUser(context: Context): Boolean {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkCountry = telephonyManager?.networkCountryIso?.lowercase()?.trim()
            if (networkCountry == "bd") return true

            val simCountry = telephonyManager?.simCountryIso?.lowercase()?.trim()
            if (simCountry == "bd") return true

            val localeCountry = Locale.getDefault().country.lowercase().trim()
            if (localeCountry == "bd") return true

            val timeZoneId = TimeZone.getDefault().id
            if (timeZoneId.contains("Dhaka", ignoreCase = true) ||
                timeZoneId.contains("Bangladesh", ignoreCase = true)) {
                return true
            }
        } catch (_: Exception) {
            // Graceful fallback
        }
        return false
    }
}
