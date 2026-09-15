package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val courseCode: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
