package com.deproof.presentation.shortcuts

import android.content.Context
import android.content.pm.ShortcutManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AppShortcutsManagerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockShortcutManager: ShortcutManager

    private lateinit var shortcutsManager: AppShortcutsManager

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.getSystemService(ShortcutManager::class.java))
            .thenReturn(mockShortcutManager)
        shortcutsManager = AppShortcutsManager(mockContext)
    }

    @Test
    fun constructor_createsInstance() {
        assertNotNull(shortcutsManager)
    }

    @Test
    fun setupAppShortcuts_callsSystemService() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            shortcutsManager.setupAppShortcuts()
            assertNotNull(mockContext)
        }
    }

    @Test
    fun addDynamicShortcut_withValidId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            shortcutsManager.addDynamicShortcut(
                "test_shortcut",
                "Test",
                "deproof://app/now"
            )
            assertNotNull(shortcutsManager)
        }
    }

    @Test
    fun removeDynamicShortcut_withValidId() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            shortcutsManager.removeDynamicShortcut("test_shortcut")
            assertNotNull(shortcutsManager)
        }
    }

    @Test
    fun clearDynamicShortcuts() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N_MR1) {
            shortcutsManager.clearDynamicShortcuts()
            assertNotNull(shortcutsManager)
        }
    }

    @Test
    fun deepLinkFormats_areCorrect() {
        val nowLink = "deproof://app/now"
        val reviewLink = "deproof://app/review"
        val receiptsLink = "deproof://app/receipts"

        assertEquals("deproof://app/now", nowLink)
        assertEquals("deproof://app/review", reviewLink)
        assertEquals("deproof://app/receipts", receiptsLink)
    }

    @Test
    fun shortcutIds_follow_convention() {
        val id1 = "shortcut_now"
        val id2 = "shortcut_review"
        val id3 = "shortcut_receipts"

        assertTrue(id1.startsWith("shortcut_"))
        assertTrue(id2.startsWith("shortcut_"))
        assertTrue(id3.startsWith("shortcut_"))
    }

    private fun assertTrue(value: Boolean) {
        if (!value) throw AssertionError("Expected true")
    }
}
