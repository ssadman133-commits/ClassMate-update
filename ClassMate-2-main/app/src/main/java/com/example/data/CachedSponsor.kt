package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_sponsors")
data class CachedSponsor(
    @PrimaryKey
    val id: String,
    val name: String,
    val imageUrl: String,
    val websiteUrl: String,
    val startDate: Long, // timestamp millis
    val endDate: Long,   // timestamp millis
    val isActive: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isValidCurrently(now: Long = System.currentTimeMillis()): Boolean {
        return isActive && now in startDate..endDate
    }
}
