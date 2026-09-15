package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_plans")
data class StudyPlan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subtitle: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long,
    val targetDays: Int = 10,
    val colorHex: String = "#8A2BE2",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "study_plan_tasks",
    foreignKeys = [
        ForeignKey(
            entity = StudyPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["planId"])]
)
data class StudyPlanTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val planId: Long,
    val title: String,
    val dayNumber: Int = 1,
    val isDone: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class StudyPlanWithTasks(
    val plan: StudyPlan,
    val tasks: List<StudyPlanTask>
) {
    val totalTasks: Int get() = tasks.size
    val completedTasks: Int get() = tasks.count { it.isDone }
    val progress: Float get() = if (totalTasks == 0) 0f else completedTasks.toFloat() / totalTasks.toFloat()
    val progressPercent: Int get() = (progress * 100).toInt()

    fun getDaysPassed(): Int {
        val now = System.currentTimeMillis()
        val diffMillis = now - plan.startDateMillis
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1
        return days.coerceIn(1, plan.targetDays)
    }

    fun getDaysLeft(): Int {
        val now = System.currentTimeMillis()
        val diffMillis = plan.endDateMillis - now
        val days = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return if (days < 0) 0 else days
    }
}
