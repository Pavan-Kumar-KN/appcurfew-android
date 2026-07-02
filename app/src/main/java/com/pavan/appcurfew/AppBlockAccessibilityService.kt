package com.pavan.appcurfew

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class AppBlockAccessibilityService : AccessibilityService() {

    private val prefs by lazy { BedtimePrefs(this) }
    private var lastBlockedPackage: String? = null
    private var lastBlockedAtMillis: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString().orEmpty()
        if (packageName.isBlank()) {
            return
        }

        if (shouldBlock(packageName)) {
            val now = System.currentTimeMillis()
            if (packageName == lastBlockedPackage && now - lastBlockedAtMillis < 750) {
                return
            }

            lastBlockedPackage = packageName
            lastBlockedAtMillis = now
            
            // Increment attempt count
            prefs.incrementAttemptCount(packageName)
            
            // Launch Warning Activity
            val intent = Intent(this, BlockedWarningActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(BlockedWarningActivity.EXTRA_PACKAGE_NAME, packageName)
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() = Unit

    private fun shouldBlock(packageName: String): Boolean {
        // Enforce re-enabling if override expired
        val isEnabled = prefs.isBlockingEnabled()
        if (!isEnabled) {
            return false
        }

        if (!prefs.isWithinActiveWindow()) {
            return false
        }

        // Whitelist critical apps
        if (packageName in ALLOWED_PACKAGES) {
            return false
        }

        // Hard Restrictions: Block Settings and Play Store during bedtime
        if (packageName == "com.android.settings" || packageName == "com.android.vending") {
            return true
        }

        return packageName in prefs.getBlockedPackages()
    }

    companion object {
        private val ALLOWED_PACKAGES = setOf(
            "com.android.dialer",
            "com.android.contacts",
            "com.google.android.dialer",
            "com.android.phone",
            "com.android.systemui",
            "com.pavan.appcurfew"
        )
    }
}