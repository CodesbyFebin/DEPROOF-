package com.deproof.presentation.shortcuts

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import com.deproof.presentation.MainActivity
import com.deproof.presentation.navigation.DeepLinkNavigator
import timber.log.Timber

class AppShortcutsManager(private val context: Context) {

    fun setupAppShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            createStaticShortcuts()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createStaticShortcuts() {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
            ?: return

        try {
            val shortcuts = mutableListOf<ShortcutInfo>()

            // Now screen shortcut
            shortcuts.add(
                createShortcut(
                    id = "shortcut_now",
                    label = "Now",
                    icon = android.R.drawable.ic_menu_view,
                    action = DeepLinkNavigator.buildDeepLink(DeepLinkNavigator.ROUTE_NOW)
                )
            )

            // Review screen shortcut
            shortcuts.add(
                createShortcut(
                    id = "shortcut_review",
                    label = "Review",
                    icon = android.R.drawable.ic_menu_zoom,
                    action = DeepLinkNavigator.buildDeepLink(DeepLinkNavigator.ROUTE_REVIEW)
                )
            )

            // Receipts screen shortcut
            shortcuts.add(
                createShortcut(
                    id = "shortcut_receipts",
                    label = "Receipts",
                    icon = android.R.drawable.ic_menu_save,
                    action = DeepLinkNavigator.buildDeepLink(DeepLinkNavigator.ROUTE_RECEIPTS)
                )
            )

            shortcutManager.dynamicShortcuts = shortcuts
            Timber.d("App shortcuts created: ${shortcuts.size}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create app shortcuts")
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun createShortcut(
        id: String,
        label: String,
        icon: Int,
        action: String
    ): ShortcutInfo {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(action)
            setClass(context, MainActivity::class.java)
        }

        return ShortcutInfo.Builder(context, id)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(Icon.createWithResource(context, icon))
            .setIntent(intent)
            .build()
    }

    fun addDynamicShortcut(id: String, label: String, deepLink: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                ?: return

            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(deepLink)
                    setClass(context, MainActivity::class.java)
                }

                val shortcut = ShortcutInfo.Builder(context, id)
                    .setShortLabel(label)
                    .setLongLabel(label)
                    .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_view))
                    .setIntent(intent)
                    .build()

                shortcutManager.addDynamicShortcuts(listOf(shortcut))
                Timber.d("Dynamic shortcut added: $id")
            } catch (e: Exception) {
                Timber.e(e, "Failed to add dynamic shortcut: $id")
            }
        }
    }

    fun removeDynamicShortcut(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                ?: return

            try {
                shortcutManager.removeDynamicShortcuts(listOf(id))
                Timber.d("Dynamic shortcut removed: $id")
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove dynamic shortcut: $id")
            }
        }
    }

    fun clearDynamicShortcuts() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)
                ?: return

            try {
                shortcutManager.removeAllDynamicShortcuts()
                Timber.d("All dynamic shortcuts cleared")
            } catch (e: Exception) {
                Timber.e(e, "Failed to clear dynamic shortcuts")
            }
        }
    }
}
