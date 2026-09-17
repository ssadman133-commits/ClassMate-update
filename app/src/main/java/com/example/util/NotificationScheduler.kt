package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.Assignment
import com.example.data.Exam
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationScheduler {

    const val CHANNEL_ID = "student_reminders_channel"
    const val CHANNEL_NAME = "Student Reminders"

    const val EXTRA_TITLE = "extra_reminder_title"
    const val EXTRA_MESSAGE = "extra_reminder_message"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminders for upcoming assignments, exams, and classes"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 200, 400)
                    enableLights(true)
                    setShowBadge(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                    if (soundUri != null) {
                        setSound(soundUri, audioAttributes)
                    }
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (_: Throwable) {
            // Graceful fallback for environments with missing audio services
        }
    }

    /**
     * Combines date (millis) with time string (e.g. "11:59 PM", "14:30")
     * into exact Unix epoch milliseconds.
     */
    fun calculateExactTimestamp(dateMillis: Long, timeStr: String?): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
        }
        if (!timeStr.isNullOrBlank()) {
            val cleanTime = timeStr.trim()
            val format12 = SimpleDateFormat("hh:mm a", Locale.US)
            val format24 = SimpleDateFormat("HH:mm", Locale.US)
            val parsedDate = try {
                format12.parse(cleanTime)
            } catch (_: Exception) {
                try {
                    format24.parse(cleanTime)
                } catch (_: Exception) {
                    null
                }
            }

            if (parsedDate != null) {
                val timeCal = Calendar.getInstance().apply { time = parsedDate }
                cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
        }
        return cal.timeInMillis
    }

    fun scheduleAssignmentReminder(context: Context, assignment: Assignment) {
        if (assignment.isCompleted || assignment.reminderDaysBefore <= 0) {
            cancelAssignmentReminder(context, assignment.id)
            return
        }

        createNotificationChannel(context)

        val exactDueTime = calculateExactTimestamp(assignment.dueDate, assignment.dueTime)
        val reminderOffsetMillis = assignment.reminderDaysBefore * 86400000L
        var triggerTime = exactDueTime - reminderOffsetMillis
        val now = System.currentTimeMillis()

        // If trigger time already passed but due date is still in future (e.g. due today/tomorrow),
        // alert the user shortly (in 5 seconds) so they don't miss it!
        if (triggerTime <= now) {
            if (exactDueTime > now) {
                triggerTime = now + 5000L
            } else {
                // Completely passed
                return
            }
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

        createNotificationChannel(context)

        val exactExamTime = calculateExactTimestamp(exam.examDate, exam.examTime)
        val reminderOffsetMillis = exam.reminderDaysBefore * 86400000L
        var triggerTime = exactExamTime - reminderOffsetMillis
        val now = System.currentTimeMillis()

        // If trigger time already passed but exam is upcoming, notify shortly
        if (triggerTime <= now) {
            if (exactExamTime > now) {
                triggerTime = now + 5000L
            } else {
                return
            }
        }

        val daysText = if (exam.reminderDaysBefore == 1) "tomorrow" else "in ${exam.reminderDaysBefore} days"
        val message = "Exam scheduled $daysText at ${exam.examTime} (${exam.courseName})"

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

    /**
     * Schedules a test notification to verify that alerts pop up
     * even when the app is completely closed or in background.
     */
    fun scheduleTestNotification(context: Context, delaySeconds: Int = 5) {
        createNotificationChannel(context)
        val triggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)
        scheduleAlarm(
            context = context,
            requestCode = 99999,
            triggerAtMillis = triggerTime,
            title = "ClassMate Alert Test",
            message = "Notification system is working properly! You will receive reminders on time even when the app is closed."
        )
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } catch (_: Exception) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
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
