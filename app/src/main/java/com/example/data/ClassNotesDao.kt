package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassNotesDao {

    // Courses
    @Query(
        """
        SELECT c.*,
               (SELECT COUNT(*) FROM topics WHERE topics.courseId = c.id) AS topicCount,
               (SELECT COUNT(*) FROM notes INNER JOIN topics ON notes.topicId = topics.id WHERE topics.courseId = c.id) AS noteCount
        FROM courses c
        ORDER BY c.createdAt DESC
        """
    )
    fun getAllCoursesWithCounts(): Flow<List<CourseWithCounts>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    fun getCourseById(courseId: Long): Flow<Course?>

    @Query("SELECT * FROM courses WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCourseByName(name: String): Course?

    @Query("SELECT * FROM topics WHERE courseId = :courseId AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTopicByCourseAndName(courseId: Long, name: String): Topic?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("SELECT n.* FROM notes n INNER JOIN topics t ON n.topicId = t.id WHERE t.courseId = :courseId")
    suspend fun getNotesForCourse(courseId: Long): List<Note>

    // Topics
    @Query(
        """
        SELECT t.*,
               (SELECT COUNT(*) FROM notes WHERE notes.topicId = t.id) AS noteCount
        FROM topics t
        WHERE t.courseId = :courseId
        ORDER BY t.createdAt DESC
        """
    )
    fun getTopicsWithCounts(courseId: Long): Flow<List<TopicWithCount>>

    @Query("SELECT * FROM topics WHERE id = :topicId LIMIT 1")
    fun getTopicById(topicId: Long): Flow<Topic?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: Topic): Long

    @Update
    suspend fun updateTopic(topic: Topic)

    @Delete
    suspend fun deleteTopic(topic: Topic)

    @Query("SELECT * FROM notes WHERE topicId = :topicId")
    suspend fun getNotesForTopic(topicId: Long): List<Note>

    // Notes
    @Query("SELECT * FROM notes WHERE topicId = :topicId ORDER BY createdAt DESC")
    fun getNotesForTopicFlow(topicId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :noteId LIMIT 1")
    fun getNoteById(noteId: Long): Flow<Note?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>)

    @Query("UPDATE notes SET importance = :importance WHERE id = :noteId")
    suspend fun updateNoteImportance(noteId: Long, importance: Int)

    @Query("UPDATE notes SET textNote = :textNote WHERE id = :noteId")
    suspend fun updateNoteCaption(noteId: Long, textNote: String?)

    @Query("UPDATE notes SET importance = :importance, textNote = :textNote WHERE id = :noteId")
    suspend fun updateNoteDetails(noteId: Long, importance: Int, textNote: String?)

    @Delete
    suspend fun deleteNote(note: Note)

    // Search Notes across course name, code, topic name, or text note
    @Query(
        """
        SELECT n.*, t.name AS topicName, c.id AS courseId, c.name AS courseName, c.courseCode AS courseCode
        FROM notes n
        INNER JOIN topics t ON n.topicId = t.id
        INNER JOIN courses c ON t.courseId = c.id
        WHERE c.name LIKE '%' || :query || '%'
           OR (c.courseCode IS NOT NULL AND c.courseCode LIKE '%' || :query || '%')
           OR t.name LIKE '%' || :query || '%'
           OR (n.textNote IS NOT NULL AND n.textNote LIKE '%' || :query || '%')
        ORDER BY n.createdAt DESC
        """
    )
    fun searchNotes(query: String): Flow<List<NoteSearchResult>>

    // Assignments
    @Query("SELECT * FROM assignments ORDER BY isCompleted ASC, dueDate ASC, dueTime ASC")
    fun getAllAssignments(): Flow<List<Assignment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)

    @Query("UPDATE assignments SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setAssignmentCompleted(id: Long, isCompleted: Boolean)

    // Exams
    @Query("SELECT * FROM exams ORDER BY examDate ASC, examTime ASC")
    fun getAllExams(): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Class Routine
    @Query("SELECT * FROM routine_items ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllRoutineItems(): Flow<List<RoutineItem>>

    @Query("SELECT * FROM routine_items WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getRoutineItemsForDay(dayOfWeek: Int): Flow<List<RoutineItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutineItem(item: RoutineItem): Long

    @Update
    suspend fun updateRoutineItem(item: RoutineItem)

    @Delete
    suspend fun deleteRoutineItem(item: RoutineItem)

    // Semester Records for CGPA Calculator
    @Query("SELECT * FROM semester_records ORDER BY createdAt ASC")
    fun getAllSemesterRecords(): Flow<List<SemesterRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemesterRecord(record: SemesterRecord): Long

    @Delete
    suspend fun deleteSemesterRecord(record: SemesterRecord)

    // Sponsor Cache
    @Query("SELECT * FROM cached_sponsors WHERE isActive = 1 AND :now BETWEEN startDate AND endDate ORDER BY cachedAt DESC")
    fun getActiveCachedSponsors(now: Long = System.currentTimeMillis()): Flow<List<CachedSponsor>>

    @Query("SELECT * FROM cached_sponsors WHERE isActive = 1 AND :now BETWEEN startDate AND endDate LIMIT 1")
    fun getActiveCachedSponsor(now: Long = System.currentTimeMillis()): Flow<CachedSponsor?>

    @Query("SELECT * FROM cached_sponsors")
    suspend fun getAllCachedSponsors(): List<CachedSponsor>

    @Query("SELECT * FROM cached_sponsors LIMIT 1")
    suspend fun getAnyCachedSponsor(): CachedSponsor?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSponsors(sponsors: List<CachedSponsor>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedSponsor(sponsor: CachedSponsor)

    @Query("DELETE FROM cached_sponsors")
    suspend fun clearCachedSponsors()

    // Study Plans
    @Query("SELECT * FROM study_plans ORDER BY createdAt DESC")
    fun getAllStudyPlans(): Flow<List<StudyPlan>>

    @Query("SELECT * FROM study_plans WHERE id = :id LIMIT 1")
    fun getStudyPlanById(id: Long): Flow<StudyPlan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlan(plan: StudyPlan): Long

    @Update
    suspend fun updateStudyPlan(plan: StudyPlan)

    @Delete
    suspend fun deleteStudyPlan(plan: StudyPlan)

    // Study Plan Tasks
    @Query("SELECT * FROM study_plan_tasks WHERE planId = :planId ORDER BY dayNumber ASC, id ASC")
    fun getTasksForPlan(planId: Long): Flow<List<StudyPlanTask>>

    @Query("SELECT * FROM study_plan_tasks ORDER BY dayNumber ASC, id ASC")
    fun getAllTasks(): Flow<List<StudyPlanTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlanTask(task: StudyPlanTask): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyPlanTasks(tasks: List<StudyPlanTask>)

    @Update
    suspend fun updateStudyPlanTask(task: StudyPlanTask)

    @Delete
    suspend fun deleteStudyPlanTask(task: StudyPlanTask)

    @Query("DELETE FROM study_plan_tasks WHERE planId = :planId")
    suspend fun deleteTasksByPlanId(planId: Long)
}
