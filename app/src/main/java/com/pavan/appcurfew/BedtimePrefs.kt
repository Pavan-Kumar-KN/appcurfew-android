package com.pavan.appcurfew

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar

class BedtimePrefs(context: Context) {

    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns whether blocking is logically enabled.
     * This considers the master switch and any active overrides during bedtime.
     */
    fun isBlockingEnabled(): Boolean {
        val masterEnabled = preferences.getBoolean(KEY_ENABLED, false)
        if (!masterEnabled) return false
        
        if (!isWithinActiveWindow()) return true
        
        // If within active window, check if override is active
        if (isOverrideActive()) {
            val endTime = getOverrideEndTime()
            if (System.currentTimeMillis() > endTime) {
                // Override expired, re-enable protection (clear override)
                setOverrideActive(false)
                return true
            }
            // Override still active, logically disabled
            return false
        }
        
        return true
    }

    /**
     * Sets the master switch state.
     * If enabling, we clear any active overrides to ensure protection starts immediately.
     */
    fun setBlockingEnabled(enabled: Boolean) {
        if (enabled) {
            // If the user manually turns it ON, clear any existing overrides
            setOverrideActive(false)
        }
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

    fun getPinCode(): String? = preferences.getString(KEY_PIN_CODE, null)

    fun setPinCode(pin: String) {
        preferences.edit().putString(KEY_PIN_CODE, pin).apply()
    }

    fun incrementAttemptCount(packageName: String) {
        checkAndResetDailyCounts()
        val counts = getAttemptCountsMap().toMutableMap()
        counts[packageName] = (counts[packageName] ?: 0) + 1
        saveAttemptCountsMap(counts)
    }

    fun getAttemptCount(packageName: String): Int {
        checkAndResetDailyCounts()
        return getAttemptCountsMap()[packageName] ?: 0
    }

    fun getTotalAttemptCount(): Int {
        checkAndResetDailyCounts()
        return getAttemptCountsMap().values.sum()
    }

    fun getOverrideAttemptCount(): Int {
        checkAndResetDailyCounts()
        return preferences.getInt(KEY_OVERRIDE_ATTEMPT_COUNT, 0)
    }

    fun incrementOverrideAttemptCount() {
        checkAndResetDailyCounts()
        val current = getOverrideAttemptCount()
        preferences.edit().putInt(KEY_OVERRIDE_ATTEMPT_COUNT, current + 1).apply()
    }

    fun resetOverrideAttemptCount() {
        preferences.edit().putInt(KEY_OVERRIDE_ATTEMPT_COUNT, 0).apply()
    }

    fun getRemainingMinutesUntilUnlock(): Int {
        if (!isWithinActiveWindow()) return 0
        
        val now = currentMinutesOfDay()
        val end = getEndMinutes()
        
        return if (getStartMinutes() > end) {
            // Overlapping midnight
            if (now >= getStartMinutes()) {
                (1440 - now) + end
            } else {
                end - now
            }
        } else {
            end - now
        }
    }

    private fun getAttemptCountsMap(): Map<String, Int> {
        val serialized = preferences.getString(KEY_ATTEMPT_COUNTS, "") ?: ""
        if (serialized.isEmpty()) return emptyMap()
        
        return serialized.split(",").associate {
            val parts = it.split(":")
            if (parts.size == 2) {
                parts[0] to (parts[1].toIntOrNull() ?: 0)
            } else {
                "" to 0
            }
        }.filter { it.key.isNotEmpty() }
    }

    private fun saveAttemptCountsMap(counts: Map<String, Int>) {
        val serialized = counts.entries.joinToString(",") { "${it.key}:${it.value}" }
        preferences.edit().putString(KEY_ATTEMPT_COUNTS, serialized).apply()
    }

    private fun checkAndResetDailyCounts() {
        val lastResetDay = preferences.getInt(KEY_LAST_RESET_DAY, -1)
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        
        if (lastResetDay != currentDay) {
            preferences.edit()
                .putString(KEY_ATTEMPT_COUNTS, "")
                .putInt(KEY_OVERRIDE_ATTEMPT_COUNT, 0)
                .putInt(KEY_LAST_RESET_DAY, currentDay)
                .apply()
        }
    }

    fun isOverrideActive(): Boolean = preferences.getBoolean(KEY_OVERRIDE_ACTIVE, false)

    fun setOverrideActive(active: Boolean) {
        preferences.edit().putBoolean(KEY_OVERRIDE_ACTIVE, active).apply()
        if (!active) {
            preferences.edit().putLong(KEY_OVERRIDE_END_TIME, 0L).apply()
        }
    }

    fun getOverrideEndTime(): Long = preferences.getLong(KEY_OVERRIDE_END_TIME, 0L)

    fun setOverrideEndTime(timestamp: Long) {
        preferences.edit().putLong(KEY_OVERRIDE_END_TIME, timestamp).apply()
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
        private const val KEY_PIN_CODE = "pin_code"
        private const val KEY_OVERRIDE_ACTIVE = "override_active"
        private const val KEY_OVERRIDE_END_TIME = "override_end_time"
        private const val KEY_ATTEMPT_COUNTS = "attempt_counts"
        private const val KEY_LAST_RESET_DAY = "last_reset_day"
        private const val KEY_OVERRIDE_ATTEMPT_COUNT = "override_attempt_count"
    }
}

fun currentMinutesOfDay(): Int {
    val calendar = Calendar.getInstance()
    return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
}