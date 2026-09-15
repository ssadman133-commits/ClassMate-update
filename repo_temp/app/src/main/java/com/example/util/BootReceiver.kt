package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val dao = db.classNotesDao()

                    // Reschedule active assignments
                    val assignments = dao.getAllAssignments().firstOrNull() ?: emptyList()
                    for (assignment in assignments) {
                        if (!assignment.isCompleted) {
                            NotificationScheduler.scheduleAssignmentReminder(context, assignment)
                        }
                    }

                    // Reschedule active exams
                    val exams = dao.getAllExams().firstOrNull() ?: emptyList()
                    for (exam in exams) {
                        NotificationScheduler.scheduleExamReminder(context, exam)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
