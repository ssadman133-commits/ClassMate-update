package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_items")
data class RoutineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dayOfWeek: Int, // 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri, 6 = Sat, 7 = Sun
    val courseName: String,
    val startTime: String, // e.g. "09:00 AM"
    val endTime: String, // e.g. "10:30 AM"
    val roomNumber: String? = null
)
