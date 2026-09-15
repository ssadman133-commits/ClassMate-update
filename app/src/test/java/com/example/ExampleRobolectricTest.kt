package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.Course
import com.example.data.Note
import com.example.data.Topic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClassMate", appName)
    }

    @Test
    fun `course topic and note lifecycle test`() = runBlocking {
        val dao = db.classNotesDao()

        // 1. Create Course
        val courseId = dao.insertCourse(
            Course(name = "Data Structures", courseCode = "CSE 221")
        )
        assertTrue(courseId > 0)

        // 2. Create Topic
        val topicId = dao.insertTopic(
            Topic(courseId = courseId, name = "Binary Search Tree")
        )
        assertTrue(topicId > 0)

        // 3. Add Note with importance 4
        val noteId = dao.insertNote(
            Note(topicId = topicId, imagePath = "/dummy/path.jpg", importance = 4)
        )
        assertTrue(noteId > 0)

        // 4. Verify Counts
        val coursesWithCounts = dao.getAllCoursesWithCounts().first()
        assertEquals(1, coursesWithCounts.size)
        assertEquals("Data Structures", coursesWithCounts[0].course.name)
        assertEquals(1, coursesWithCounts[0].topicCount)
        assertEquals(1, coursesWithCounts[0].noteCount)

        // 5. Update Note Importance
        dao.updateNoteImportance(noteId, 5)
        val updatedNote = dao.getNoteById(noteId).first()
        assertNotNull(updatedNote)
        assertEquals(5, updatedNote?.importance)

        // 6. Rename Course
        dao.updateCourse(coursesWithCounts[0].course.copy(name = "Advanced Data Structures"))
        val renamedCourse = dao.getCourseById(courseId).first()
        assertEquals("Advanced Data Structures", renamedCourse?.name)

        // 7. Delete Topic (cascades to Note)
        val topic = dao.getTopicById(topicId).first()
        assertNotNull(topic)
        dao.deleteTopic(topic!!)
        val remainingNotes = dao.getNotesForTopic(topicId)
        assertTrue(remainingNotes.isEmpty())
    }
}
