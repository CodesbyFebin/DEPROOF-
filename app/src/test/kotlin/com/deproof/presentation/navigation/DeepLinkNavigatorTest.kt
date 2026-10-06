package com.deproof.presentation.navigation

import android.net.Uri
import androidx.navigation.NavController
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeepLinkNavigatorTest {
    private val mockNavController = MockNavController()

    @Before
    fun setup() {
        mockNavController.reset()
    }

    @Test
    fun buildDeepLink_withoutParams() {
        val link = DeepLinkNavigator.buildDeepLink("now")
        assertEquals("deproof://app/now", link)
    }

    @Test
    fun buildDeepLink_withSingleParam() {
        val link = DeepLinkNavigator.buildDeepLink("receipt_details", "id" to "123")
        assertEquals("deproof://app/receipt_details?id=123", link)
    }

    @Test
    fun buildDeepLink_withMultipleParams() {
        val link = DeepLinkNavigator.buildDeepLink(
            "search",
            "query" to "solana",
            "filter" to "verified"
        )
        assertTrue(link.contains("query=solana"))
        assertTrue(link.contains("filter=verified"))
        assertEquals("deproof://app/search", link.substringBefore("?"))
    }

    @Test
    fun buildDeepLink_encodesSpecialCharacters() {
        val link = DeepLinkNavigator.buildDeepLink("search", "query" to "hello world")
        assertTrue(link.contains("query=hello%20world"))
    }

    @Test
    fun handleDeepLink_nullUri_returnsFalse() {
        val result = DeepLinkNavigator.handleDeepLink(null, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_invalidScheme_returnsFalse() {
        val uri = Uri.parse("https://example.com/now")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_invalidHost_returnsFalse() {
        val uri = Uri.parse("deproof://invalid.host/now")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_nowRoute_navigatesToNow() {
        val uri = Uri.parse("deproof://app/now")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("now", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_tasksRoute_navigatesToTasks() {
        val uri = Uri.parse("deproof://app/tasks")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("tasks", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_nodesRoute_navigatesToNodes() {
        val uri = Uri.parse("deproof://app/nodes")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("nodes", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_reviewRoute_navigatesToReview() {
        val uri = Uri.parse("deproof://app/review")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("review", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_receiptsRoute_navigatesToReceipts() {
        val uri = Uri.parse("deproof://app/receipts")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("receipts", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_receiptDetailsRoute_withValidId() {
        val uri = Uri.parse("deproof://app/receipt_details?id=abc123")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("receipt_details/abc123", mockNavController.lastNavigatedRoute)
    }

    @Test
    fun handleDeepLink_receiptDetailsRoute_withoutId_returnsFalse() {
        val uri = Uri.parse("deproof://app/receipt_details")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_unknownRoute_returnsFalse() {
        val uri = Uri.parse("deproof://app/unknown")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_emptyPath_returnsFalse() {
        val uri = Uri.parse("deproof://app/")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertFalse(result)
    }

    @Test
    fun handleDeepLink_multipleQueryParams() {
        val uri = Uri.parse("deproof://app/receipt_details?id=xyz&view=full&sort=date")
        val result = DeepLinkNavigator.handleDeepLink(uri, mockNavController)
        assertTrue(result)
        assertEquals("receipt_details/xyz", mockNavController.lastNavigatedRoute)
    }

    private class MockNavController : NavController(null) {
        var lastNavigatedRoute: String? = null
        private val navigateHistory = mutableListOf<String>()

        override fun navigate(route: String, builder: androidx.navigation.NavOptions.Builder.() -> Unit) {
            lastNavigatedRoute = route
            navigateHistory.add(route)
        }

        override fun navigate(
            route: String,
            args: android.os.Bundle?,
            navOptions: androidx.navigation.NavOptions?,
            navigatorExtras: androidx.navigation.Navigator.Extras?
        ) {
            lastNavigatedRoute = route
            navigateHistory.add(route)
        }

        fun reset() {
            lastNavigatedRoute = null
            navigateHistory.clear()
        }
    }
}
