package com.example.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class GradeScale(
    val aPlus: Double = 4.00,
    val a: Double = 3.75,
    val aMinus: Double = 3.50,
    val bPlus: Double = 3.25,
    val b: Double = 3.00,
    val bMinus: Double = 2.75,
    val cPlus: Double = 2.50,
    val c: Double = 2.25,
    val d: Double = 2.00,
    val f: Double = 0.00
) {
    fun getPointForGrade(grade: String): Double {
        return when (grade.trim().uppercase()) {
            "A+" -> aPlus
            "A" -> a
            "A-" -> aMinus
            "B+" -> bPlus
            "B" -> b
            "B-" -> bMinus
            "C+" -> cPlus
            "C" -> c
            "D" -> d
            "F" -> f
            else -> 0.0
        }
    }
}

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("student_productivity_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _gradeScale = MutableStateFlow(loadGradeScale())
    val gradeScale: StateFlow<GradeScale> = _gradeScale.asStateFlow()

    private val _assignmentRemindersEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_ASSIGNMENT_REMINDERS, true)
    )
    val assignmentRemindersEnabled: StateFlow<Boolean> = _assignmentRemindersEnabled.asStateFlow()

    private val _examRemindersEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_EXAM_REMINDERS, true)
    )
    val examRemindersEnabled: StateFlow<Boolean> = _examRemindersEnabled.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun saveGradeScale(scale: GradeScale) {
        prefs.edit()
            .putFloat(KEY_GRADE_A_PLUS, scale.aPlus.toFloat())
            .putFloat(KEY_GRADE_A, scale.a.toFloat())
            .putFloat(KEY_GRADE_A_MINUS, scale.aMinus.toFloat())
            .putFloat(KEY_GRADE_B_PLUS, scale.bPlus.toFloat())
            .putFloat(KEY_GRADE_B, scale.b.toFloat())
            .putFloat(KEY_GRADE_B_MINUS, scale.bMinus.toFloat())
            .putFloat(KEY_GRADE_C_PLUS, scale.cPlus.toFloat())
            .putFloat(KEY_GRADE_C, scale.c.toFloat())
            .putFloat(KEY_GRADE_D, scale.d.toFloat())
            .putFloat(KEY_GRADE_F, scale.f.toFloat())
            .apply()
        _gradeScale.value = scale
    }

    private fun loadGradeScale(): GradeScale {
        return GradeScale(
            aPlus = prefs.getFloat(KEY_GRADE_A_PLUS, 4.00f).toDouble(),
            a = prefs.getFloat(KEY_GRADE_A, 3.75f).toDouble(),
            aMinus = prefs.getFloat(KEY_GRADE_A_MINUS, 3.50f).toDouble(),
            bPlus = prefs.getFloat(KEY_GRADE_B_PLUS, 3.25f).toDouble(),
            b = prefs.getFloat(KEY_GRADE_B, 3.00f).toDouble(),
            bMinus = prefs.getFloat(KEY_GRADE_B_MINUS, 2.75f).toDouble(),
            cPlus = prefs.getFloat(KEY_GRADE_C_PLUS, 2.50f).toDouble(),
            c = prefs.getFloat(KEY_GRADE_C, 2.25f).toDouble(),
            d = prefs.getFloat(KEY_GRADE_D, 2.00f).toDouble(),
            f = prefs.getFloat(KEY_GRADE_F, 0.00f).toDouble()
        )
    }

    fun setAssignmentRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASSIGNMENT_REMINDERS, enabled).apply()
        _assignmentRemindersEnabled.value = enabled
    }

    fun setExamRemindersEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EXAM_REMINDERS, enabled).apply()
        _examRemindersEnabled.value = enabled
    }

    companion object {
        private const val KEY_THEME_MODE = "app_theme_mode"
        private const val KEY_ASSIGNMENT_REMINDERS = "assignment_reminders_enabled"
        private const val KEY_EXAM_REMINDERS = "exam_reminders_enabled"

        private const val KEY_GRADE_A_PLUS = "grade_a_plus"
        private const val KEY_GRADE_A = "grade_a"
        private const val KEY_GRADE_A_MINUS = "grade_a_minus"
        private const val KEY_GRADE_B_PLUS = "grade_b_plus"
        private const val KEY_GRADE_B = "grade_b"
        private const val KEY_GRADE_B_MINUS = "grade_b_minus"
        private const val KEY_GRADE_C_PLUS = "grade_c_plus"
        private const val KEY_GRADE_C = "grade_c"
        private const val KEY_GRADE_D = "grade_d"
        private const val KEY_GRADE_F = "grade_f"
    }
}
