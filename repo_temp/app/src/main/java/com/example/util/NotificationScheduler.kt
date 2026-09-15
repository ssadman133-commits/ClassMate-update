package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.Assignment
import com.example.data.Exam

object NotificationScheduler {

    const val CHANNEL_ID = "student_reminders_channel"
    const val CHANNEL_NAME = "Student Reminders"

    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_MESSAGE = "extra_reminder_message"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming assignments and exams"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleAssignmentReminder(context: Context, assignment: Assignment) {
        if (assignment.isCompleted || assignment.reminderDaysBefore <= 0) {
            cancelAssignmentReminder(context, assignment.id)
            return
        }

        val triggerTime = assignment.dueDate - (assignment.reminderDaysBefore * 86400000L)
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val daysText = if (assignment.reminderDaysBefore == 1) "tomorrow" else "in ${assignment.reminderDaysBefore} days"
        val message = "Assignment due $daysText at ${assignment.dueTime} (${assignment.courseName})"

        scheduleAlarm(
            context = context,
            requestCode = (assignment.id * 10 + 1).toInt(),
            triggerAtMillis = triggerTime,
            title = "Assignment: ${assignment.title}",
            message = message
        )
    }

    fun cancelAssignmentReminder(context: Context, assignmentId: Long) {
        cancelAlarm(context, (assignmentId * 10 + 1).toInt())
    }

    fun scheduleExamReminder(context: Context, exam: Exam) {
        if (exam.reminderDaysBefore <= 0) {
            cancelExamReminder(context, exam.id)
            return
        }

        val triggerTime = exam.examDate - (exam.reminderDaysBefore * 86400000L)
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }

        val daysText = if (exam.reminderDaysBefore == 1) "tomorrow" else "in ${exam.reminderDaysBefore} days"
        val message = "Exam coming up $daysText at ${exam.examTime} (${exam.courseName})"

        scheduleAlarm(
            context = context,
            requestCode = (exam.id * 10 + 2).toInt(),
            triggerAtMillis = triggerTime,
            title = "Upcoming Exam: ${exam.title}",
            message = message
        )
    }

    fun cancelExamReminder(context: Context, examId: Long) {
        cancelAlarm(context, (examId * 10 + 2).toInt())
    }

    private fun scheduleAlarm(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        message: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_NOTIFICATION_ID, requestCode)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
