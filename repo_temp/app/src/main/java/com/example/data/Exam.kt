package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String, // e.g. "Midterm", "Final", "Quiz 1"
    val courseName: String,
    val examDate: Long, // timestamp for date
    val examTime: String, // e.g. "10:00 AM"
    val description: String? = null,
    val reminderDaysBefore: Int = 1, // 1, 3, or 7 days before, 0 = off
    val createdAt: Long = System.currentTimeMillis()
)
