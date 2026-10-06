package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// FN051 createTask and FN052 attachEvidence persistence, against Room on the JVM
// (Robolectric, in-memory database). No device is used. Device checks stay NOT_RUN.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomStoreTest {
    private lateinit var db: DeproofDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DeproofDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun close() { db.close() }

    private fun attachment(id: String, taskId: String?, digest: String = "a".repeat(64)) =
        Attachment(id, taskId, digest, "application/pdf", "1234", "imported", "2026-10-06T00:00:00Z")

    @Test fun createTaskRefusesBlankTitleAndTooManyRequirements() = runBlocking {
        val dao = db.records()
        try { dao.createTask("   ", emptyList()); fail("blank title accepted") } catch (e: com.example.domain.Failure) { assertEquals("BAD_TASK", e.code) }
        try { dao.createTask("x".repeat(201), emptyList()); fail("long title accepted") } catch (e: com.example.domain.Failure) { assertEquals("BAD_TASK", e.code) }
        try { dao.createTask("ok", List(101) { "r$it" }); fail("too many requirements accepted") } catch (e: com.example.domain.Failure) { assertEquals("BAD_TASK", e.code) }
        assertTrue(dao.tasksSnapshot().isEmpty())
    }

    @Test fun createdTaskIsPersistedWithItsRequirements() = runBlocking {
        val dao = db.records()
        val t = dao.createTask("Inspect roof", listOf("photo", "measure"))
        val stored = dao.tasksSnapshot().single()
        assertEquals(t.id, stored.id)
        assertEquals("Inspect roof", stored.title)
        assertTrue(stored.checklist.contains("\"text\":\"photo\"") && stored.checklist.contains("\"text\":\"measure\""))
    }

    @Test fun attachmentKeepsDigestAndIsAssociatedWithItsTask() = runBlocking {
        val dao = db.records()
        val t = dao.createTask("Task", emptyList())
        dao.attach(attachment("att-1", t.id))
        val got = dao.attachments(t.id).single()
        assertEquals("a".repeat(64), got.digest)
        assertEquals(t.id, got.taskId)
    }

    @Test fun attachmentToMissingTaskIsRejectedByForeignKey() = runBlocking {
        val dao = db.records()
        try {
            dao.attach(attachment("att-orphan", "no-such-task"))
            fail("orphan attachment accepted")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            assertNotNull(e.message)
        }
        assertNull(dao.attachment("att-orphan"))
    }
}
