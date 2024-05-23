package com.github.droidworksstudio.launcher.helper

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.UserHandle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.droidworksstudio.launcher.Constants
import java.io.File
import java.io.IOException

fun View.hideKeyboard() {
    this.clearFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showKeyboard(show: Boolean = true) {
    if (show.not()) return
    if (this.requestFocus())
        this.postDelayed({
            this.findViewById<EditText>(androidx.appcompat.R.id.search_src_text).apply {
                textSize = 28f
                isCursorVisible = false
            }
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
        }, 100)
}

fun Context.openSearch(query: String? = null) {
    val intent = Intent(Intent.ACTION_WEB_SEARCH)
    intent.putExtra(SearchManager.QUERY, query ?: "")
    startActivity(intent)
}

fun Context.openUrl(url: String) {
    if (url.isEmpty()) return
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    startActivity(intent)
}

fun Context.searchOnPlayStore(query: String? = null): Boolean {
    return try {
        val playStoreIntent = Intent(Intent.ACTION_VIEW)
        playStoreIntent.data = Uri.parse("${Constants.APP_GOOGLE_PLAY_STORE}=$query")

        // Check if the Play Store app is installed
        if (playStoreIntent.resolveActivity(packageManager) != null) {
            startActivity(playStoreIntent)
        } else {
            // If Play Store app is not installed, open Play Store website in browser
            playStoreIntent.data = Uri.parse("${Constants.URL_GOOGLE_PLAY_STORE}=$query")
            startActivity(playStoreIntent)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Context.searchCustomSearchEngine(searchQuery: String? = null): Boolean {
    val searchUrl = Constants.URL_GOOGLE_SEARCH
    val encodedQuery = Uri.encode(searchQuery)
    val fullUrl = "$searchUrl$encodedQuery"
    Log.d("fullUrl", fullUrl)
    openUrl(fullUrl)
    return true
}

fun Context.backupSharedPreferences(backupFileName: String) {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(Constants.PREFS_FILENAME, 0)
    val allPrefs = sharedPreferences.all
    val backupFile = File(filesDir, backupFileName)

    println("Backup SharedPreferences to: ${backupFile.absolutePath}")

    try {
        backupFile.bufferedWriter().use { writer ->
            for ((key, value) in allPrefs) {
                if (value != null) {
                    val line = when (value) {
                        is Boolean -> "$key=${value}\n"
                        is Int -> "$key=${value}\n"
                        is Float -> "$key=${value}\n"
                        is Long -> "$key=${value}\n"
                        is String -> "$key=${value}\n"
                        is Set<*> -> "$key=${value.joinToString(",")}\n"
                        else -> null
                    }
                    if (line != null) {
                        writer.write(line)
                        println("Writing: $line")
                    } else {
                        println("Skipping unsupported type for key: $key")
                    }
                } else {
                    println("Null value for key: $key")
                }
            }
        }
        println("Backup completed successfully.")
    } catch (e: IOException) {
        e.printStackTrace()
        println("Failed to backup SharedPreferences: ${e.message}")
    }
}

fun Context.restoreSharedPreferences(backupFileName: String) {
    val sharedPreferences: SharedPreferences = this.getSharedPreferences(Constants.PREFS_FILENAME, 0)
    val editor = sharedPreferences.edit()
    val backupFile = File(filesDir, backupFileName)

    println("Restoring SharedPreferences from: ${backupFile.absolutePath}")

    if (backupFile.exists()) {
        try {
            backupFile.forEachLine { line ->
                val (key, value) = line.split("=", limit = 2)
                when {
                    value.toBooleanStrictOrNull() != null -> editor.putBoolean(key, value.toBoolean())
                    value.toIntOrNull() != null -> editor.putInt(key, value.toInt())
                    value.toFloatOrNull() != null -> editor.putFloat(key, value.toFloat())
                    value.toLongOrNull() != null -> editor.putLong(key, value.toLong())
                    value.contains(",") -> editor.putStringSet(key, value.split(",").toSet())
                    else -> editor.putString(key, value)
                }
                println("Restoring: $key=$value")
            }
            editor.apply()
            println("Restore completed successfully.")
        } catch (e: IOException) {
            e.printStackTrace()
            println("Failed to restore SharedPreferences: ${e.message}")
        }
    } else {
        println("Backup file does not exist.")
    }
}

fun Fragment.restartApp() {
    val packageManager = requireContext().packageManager
    val intent = packageManager.getLaunchIntentForPackage(requireContext().packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    if (intent != null) {
        startActivity(intent)
    }
    requireActivity().finish()
}

fun Context.isPackageInstalled(packageName: String, userHandle: UserHandle = android.os.Process.myUserHandle()): Boolean {
    val launcher = getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val activityInfo = launcher.getActivityList(packageName, userHandle)
    return activityInfo.size > 0
}