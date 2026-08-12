package com.example.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class AppTools(private val context: Context) {

    fun openApp(packageNameOrQuery: String): ToolExecutionResult {
        val pm = context.packageManager

        // 1. Direct package match attempt
        val launchIntent = pm.getLaunchIntentForPackage(packageNameOrQuery)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return ToolExecutionResult(true, "Opened $packageNameOrQuery, sir.")
        }

        // 2. Search installed applications by label
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val query = packageNameOrQuery.lowercase()

        for (appInfo in installedApps) {
            val appLabel = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (appLabel.contains(query) || query.contains(appLabel)) {
                val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return ToolExecutionResult(true, "Opening ${pm.getApplicationLabel(appInfo)}, sir.")
                }
            }
        }

        // 3. Fallback: Search on Play Store / Google
        return searchWeb(packageNameOrQuery)
    }

    fun openUrl(url: String): ToolExecutionResult {
        var formattedUrl = url
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://$formattedUrl"
        }
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Opening URL, sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Failed to open URL: ${e.message}")
        }
    }

    fun searchWeb(query: String): ToolExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ToolExecutionResult(true, "Searching Google for '$query', sir.")
        } catch (e: Exception) {
            ToolExecutionResult(false, "Search failed: ${e.message}")
        }
    }
}
