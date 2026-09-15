package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assignments")
data class Assignment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val courseName: String,
    val dueDate: Long, // timestamp for date
    val dueTime: String, // e.g. "11:59 PM"
    val description: String? = null,
    val reminderDaysBefore: Int = 1, // 1, 3, or 7 days before, 0 = off
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
