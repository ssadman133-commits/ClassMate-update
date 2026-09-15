package com.example.data

import androidx.room.Embedded

data class CourseWithCounts(
    @Embedded val course: Course,
    val topicCount: Int,
    val noteCount: Int
)

data class TopicWithCount(
    @Embedded val topic: Topic,
    val noteCount: Int
)

data class NoteSearchResult(
    @Embedded val note: Note,
    val topicName: String,
    val courseId: Long,
    val courseName: String,
    val courseCode: String?
)

