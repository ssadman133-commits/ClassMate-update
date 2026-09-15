package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semester_records")
data class SemesterRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val semesterName: String, // e.g. "Semester 1", "Spring 2024"
    val gpa: Double,
    val totalCredits: Double,
    val createdAt: Long = System.currentTimeMillis()
)
