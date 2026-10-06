package com.deproof.presentation.navigation

import android.net.Uri
import androidx.navigation.NavController
import timber.log.Timber

object DeepLinkNavigator {
    const val DEEP_LINK_SCHEME = "deproof"
    const val DEEP_LINK_HOST = "app"

    const val ROUTE_NOW = "now"
    const val ROUTE_TASKS = "tasks"
    const val ROUTE_NODES = "nodes"
    const val ROUTE_REVIEW = "review"
    const val ROUTE_RECEIPTS = "receipts"
    const val ROUTE_RECEIPT_DETAILS = "receipt_details"

    fun buildDeepLink(route: String, vararg params: Pair<String, String>): String {
        val uri = StringBuilder("$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$route")
        if (params.isNotEmpty()) {
            uri.append("?")
            uri.append(params.joinToString("&") { (key, value) ->
                "$key=${Uri.encode(value)}"
            })
        }
        return uri.toString()
    }

    fun handleDeepLink(uri: Uri?, navController: NavController): Boolean {
        if (uri == null) return false

        Timber.d("Handling deep link: ${uri.toString()}")

        if (uri.scheme != DEEP_LINK_SCHEME || uri.host != DEEP_LINK_HOST) {
            Timber.w("Invalid deep link scheme or host: ${uri.scheme}://${uri.host}")
            return false
        }

        val path = uri.pathSegments.firstOrNull() ?: return false
        val queryParams = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }

        return when (path) {
            ROUTE_NOW -> {
                navController.navigate(ROUTE_NOW) {
                    popUpTo(ROUTE_NOW) { inclusive = true }
                }
                true
            }
            ROUTE_TASKS -> {
                navController.navigate(ROUTE_TASKS) {
                    popUpTo(ROUTE_TASKS) { inclusive = true }
                }
                true
            }
            ROUTE_NODES -> {
                navController.navigate(ROUTE_NODES) {
                    popUpTo(ROUTE_NODES) { inclusive = true }
                }
                true
            }
            ROUTE_REVIEW -> {
                navController.navigate(ROUTE_REVIEW) {
                    popUpTo(ROUTE_REVIEW) { inclusive = true }
                }
                true
            }
            ROUTE_RECEIPTS -> {
                navController.navigate(ROUTE_RECEIPTS) {
                    popUpTo(ROUTE_RECEIPTS) { inclusive = true }
                }
                true
            }
            ROUTE_RECEIPT_DETAILS -> {
                val receiptId = queryParams["id"]
                if (!receiptId.isNullOrEmpty()) {
                    navController.navigate("receipt_details/$receiptId")
                    true
                } else {
                    Timber.w("Receipt details deep link missing 'id' parameter")
                    false
                }
            }
            else -> {
                Timber.w("Unknown deep link route: $path")
                false
            }
        }
    }
}
