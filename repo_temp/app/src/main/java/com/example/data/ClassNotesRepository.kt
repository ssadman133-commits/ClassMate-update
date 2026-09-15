package com.example.data

import com.example.util.ImageStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ClassNotesRepository(
    private val dao: ClassNotesDao
) {
    val allCoursesWithCounts: Flow<List<CourseWithCounts>> = dao.getAllCoursesWithCounts()

    fun getCourseById(courseId: Long): Flow<Course?> = dao.getCourseById(courseId)

    suspend fun getCourseByName(name: String): Course? = withContext(Dispatchers.IO) {
        dao.getCourseByName(name.trim())
    }

    suspend fun getTopicByCourseAndName(courseId: Long, name: String): Topic? = withContext(Dispatchers.IO) {
        dao.getTopicByCourseAndName(courseId, name.trim())
    }

    suspend fun insertCourse(name: String, courseCode: String?): Long = withContext(Dispatchers.IO) {
        val course = Course(
            name = name.trim(),
            courseCode = courseCode?.trim()?.ifBlank { null }
        )
        dao.insertCourse(course)
    }

    suspend fun updateCourse(course: Course) = withContext(Dispatchers.IO) {
        dao.updateCourse(course)
    }

    suspend fun deleteCourse(course: Course) = withContext(Dispatchers.IO) {
        // Collect and delete all associated note images from internal storage
        val notes = dao.getNotesForCourse(course.id)
        notes.forEach { note ->
            ImageStorageManager.deleteImageFile(note.imagePath)
        }
        dao.deleteCourse(course)
    }

    fun getTopicsWithCounts(courseId: Long): Flow<List<TopicWithCount>> =
        dao.getTopicsWithCounts(courseId)

    fun getTopicById(topicId: Long): Flow<Topic?> = dao.getTopicById(topicId)

    suspend fun insertTopic(courseId: Long, name: String): Long = withContext(Dispatchers.IO) {
        val topic = Topic(
            courseId = courseId,
            name = name.trim()
        )
        dao.insertTopic(topic)
    }

    suspend fun updateTopic(topic: Topic) = withContext(Dispatchers.IO) {
        dao.updateTopic(topic)
    }

    suspend fun deleteTopic(topic: Topic) = withContext(Dispatchers.IO) {
        // Collect and delete all associated note images from internal storage
        val notes = dao.getNotesForTopic(topic.id)
        notes.forEach { note ->
            ImageStorageManager.deleteImageFile(note.imagePath)
        }
        dao.deleteTopic(topic)
    }

    fun getNotesForTopic(topicId: Long): Flow<List<Note>> =
        dao.getNotesForTopicFlow(topicId)

    fun getNoteById(noteId: Long): Flow<Note?> = dao.getNoteById(noteId)

    suspend fun addNote(topicId: Long, imagePath: String, importance: Int): Long =
        withContext(Dispatchers.IO) {
            val note = Note(
                topicId = topicId,
                imagePath = imagePath,
                importance = importance.coerceIn(1, 5)
            )
            dao.insertNote(note)
        }

    suspend fun addNotes(
        topicId: Long,
        imagePaths: List<String>,
        importance: Int,
        textNote: String? = null
    ) = withContext(Dispatchers.IO) {
        val clampedImportance = importance.coerceIn(1, 5)
        val cleanCaption = textNote?.trim()?.ifBlank { null }
        val notes = imagePaths.map { path ->
            Note(
                topicId = topicId,
                imagePath = path,
                importance = clampedImportance,
                textNote = cleanCaption
            )
        }
        dao.insertNotes(notes)
    }

    suspend fun updateNoteImportance(noteId: Long, importance: Int) = withContext(Dispatchers.IO) {
        dao.updateNoteImportance(noteId, importance.coerceIn(1, 5))
    }

    suspend fun updateNoteDetails(noteId: Long, importance: Int, textNote: String?) = withContext(Dispatchers.IO) {
        val cleanCaption = textNote?.trim()?.ifBlank { null }
        dao.updateNoteDetails(noteId, importance.coerceIn(1, 5), cleanCaption)
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        ImageStorageManager.deleteImageFile(note.imagePath)
        dao.deleteNote(note)
    }

    // Search Notes
    fun searchNotes(query: String): Flow<List<NoteSearchResult>> = dao.searchNotes(query.trim())

    // Assignments
    val allAssignments: Flow<List<Assignment>> = dao.getAllAssignments()

    suspend fun insertAssignment(assignment: Assignment): Long = withContext(Dispatchers.IO) {
        dao.insertAssignment(assignment)
    }

    suspend fun updateAssignment(assignment: Assignment) = withContext(Dispatchers.IO) {
        dao.updateAssignment(assignment)
    }

    suspend fun deleteAssignment(assignment: Assignment) = withContext(Dispatchers.IO) {
        dao.deleteAssignment(assignment)
    }

    suspend fun setAssignmentCompleted(id: Long, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        dao.setAssignmentCompleted(id, isCompleted)
    }

    // Exams
    val allExams: Flow<List<Exam>> = dao.getAllExams()

    suspend fun insertExam(exam: Exam): Long = withContext(Dispatchers.IO) {
        dao.insertExam(exam)
    }

    suspend fun updateExam(exam: Exam) = withContext(Dispatchers.IO) {
        dao.updateExam(exam)
    }

    suspend fun deleteExam(exam: Exam) = withContext(Dispatchers.IO) {
        dao.deleteExam(exam)
    }

    // Class Routine
    val allRoutineItems: Flow<List<RoutineItem>> = dao.getAllRoutineItems()

    fun getRoutineItemsForDay(dayOfWeek: Int): Flow<List<RoutineItem>> = dao.getRoutineItemsForDay(dayOfWeek)

    suspend fun insertRoutineItem(item: RoutineItem): Long = withContext(Dispatchers.IO) {
        dao.insertRoutineItem(item)
    }

    suspend fun updateRoutineItem(item: RoutineItem) = withContext(Dispatchers.IO) {
        dao.updateRoutineItem(item)
    }

    suspend fun deleteRoutineItem(item: RoutineItem) = withContext(Dispatchers.IO) {
        dao.deleteRoutineItem(item)
    }

    // CGPA Semester Records
    val allSemesterRecords: Flow<List<SemesterRecord>> = dao.getAllSemesterRecords()

    suspend fun insertSemesterRecord(record: SemesterRecord): Long = withContext(Dispatchers.IO) {
        dao.insertSemesterRecord(record)
    }

    suspend fun deleteSemesterRecord(record: SemesterRecord) = withContext(Dispatchers.IO) {
        dao.deleteSemesterRecord(record)
    }

    // Sponsor Cache
    val activeSponsors: Flow<List<CachedSponsor>> get() = getActiveCachedSponsors()
    val activeSponsor: Flow<CachedSponsor?> get() = getActiveCachedSponsor()

    fun getActiveCachedSponsors(now: Long = System.currentTimeMillis()): Flow<List<CachedSponsor>> =
        dao.getActiveCachedSponsors(now)

    fun getActiveCachedSponsor(now: Long = System.currentTimeMillis()): Flow<CachedSponsor?> =
        dao.getActiveCachedSponsor(now)

    suspend fun getAnyCachedSponsor(): CachedSponsor? = withContext(Dispatchers.IO) {
        dao.getAnyCachedSponsor()
    }

    suspend fun getAllCachedSponsors(): List<CachedSponsor> = withContext(Dispatchers.IO) {
        dao.getAllCachedSponsors()
    }

    suspend fun saveCachedSponsors(sponsors: List<CachedSponsor>) = withContext(Dispatchers.IO) {
        dao.clearCachedSponsors()
        if (sponsors.isNotEmpty()) {
            dao.insertCachedSponsors(sponsors)
        }
    }

    suspend fun saveCachedSponsor(sponsor: CachedSponsor) = withContext(Dispatchers.IO) {
        dao.clearCachedSponsors()
        dao.insertCachedSponsor(sponsor)
    }

    suspend fun clearCachedSponsors() = withContext(Dispatchers.IO) {
        dao.clearCachedSponsors()
    }
}
