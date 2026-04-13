package com.pavan.appcurfew

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class BedtimePrefs(context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isBlockingEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun setBlockingEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getStartMinutes(): Int = preferences.getInt(KEY_START_MINUTES, 22 * 60)

    fun setStartMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_START_MINUTES, minutes.coerceIn(0, 23 * 60 + 59)).apply()
    }

    fun getEndMinutes(): Int = preferences.getInt(KEY_END_MINUTES, 6 * 60)

    fun setEndMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_END_MINUTES, minutes.coerceIn(0, 23 * 60 + 59)).apply()
    }

    fun getBlockedPackages(): Set<String> = preferences.getStringSet(KEY_BLOCKED_PACKAGES, emptySet()).orEmpty()

    fun setBlockedPackages(packages: Set<String>) {
        preferences.edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun isWithinActiveWindow(currentMinutes: Int = currentMinutesOfDay()): Boolean {
        val start = getStartMinutes()
        val end = getEndMinutes()
        return if (start > end) {
            currentMinutes >= start || currentMinutes < end
        } else {
            currentMinutes >= start && currentMinutes < end
        }
    }

    companion object {
        private const val PREFS_NAME = "bedtime_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_START_MINUTES = "start_minutes"
        private const val KEY_END_MINUTES = "end_minutes"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    }
}

fun currentMinutesOfDay(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}