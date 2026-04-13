package com.pavan.appcurfew

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: BedtimePrefs
    private lateinit var enableSwitch: MaterialSwitch
    private lateinit var startTimeButton: Button
    private lateinit var endTimeButton: Button
    private lateinit var selectAppsButton: Button
    private lateinit var accessibilityButton: Button
    private lateinit var whitelistSummary: TextView
    private lateinit var blockedAppsGroup: ChipGroup
    private lateinit var blockedAppsEmpty: TextView
    private var accessibilityPromptShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        prefs = BedtimePrefs(this)
        enableSwitch = findViewById(R.id.switchEnableBlocking)
        startTimeButton = findViewById(R.id.buttonStartTime)
        endTimeButton = findViewById(R.id.buttonEndTime)
        selectAppsButton = findViewById(R.id.buttonSelectApps)
        accessibilityButton = findViewById(R.id.buttonAccessibilitySettings)
        whitelistSummary = findViewById(R.id.textWhitelistSummary)
        blockedAppsGroup = findViewById(R.id.blockedAppsGroup)
        blockedAppsEmpty = findViewById(R.id.blockedAppsEmpty)

        enableSwitch.isChecked = prefs.isBlockingEnabled()
        startTimeButton.text = formatMinutes(prefs.getStartMinutes())
        endTimeButton.text = formatMinutes(prefs.getEndMinutes())
        updateSummary()
        renderBlockedApps()

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setBlockingEnabled(isChecked)
            updateSummary()
        }

        startTimeButton.setOnClickListener {
            pickTime(prefs.getStartMinutes()) {
                prefs.setStartMinutes(it)
                startTimeButton.text = formatMinutes(it)
                updateSummary()
                renderBlockedApps()
            }
        }

        endTimeButton.setOnClickListener {
            pickTime(prefs.getEndMinutes()) {
                prefs.setEndMinutes(it)
                endTimeButton.text = formatMinutes(it)
                updateSummary()
                renderBlockedApps()
            }
        }

        selectAppsButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, R.string.accessibility_help, Toast.LENGTH_LONG).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        enableSwitch.isChecked = prefs.isBlockingEnabled()
        startTimeButton.text = formatMinutes(prefs.getStartMinutes())
        endTimeButton.text = formatMinutes(prefs.getEndMinutes())
        updateSummary()
        renderBlockedApps()
        showAccessibilityPromptIfNeeded()
    }

    private fun updateSummary() {
        val blockedCount = prefs.getBlockedPackages().size
        whitelistSummary.text = getString(
            R.string.whitelist_summary,
            blockedCount,
            if (prefs.isBlockingEnabled()) getString(R.string.enabled) else getString(R.string.disabled)
        )
    }

    private fun pickTime(initialMinutes: Int, onSelected: (Int) -> Unit) {
        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute -> onSelected(selectedHour * 60 + selectedMinute) },
            initialMinutes / 60,
            initialMinutes % 60,
            false
        ).show()
    }

    private fun formatMinutes(minutesOfDay: Int): String {
        val hour24 = minutesOfDay / 60
        val minute = minutesOfDay % 60
        val suffix = if (hour24 >= 12) getString(R.string.pm) else getString(R.string.am)
        val normalizedHour = hour24 % 12
        val hour12 = if (normalizedHour == 0) 12 else normalizedHour
        return String.format("%d:%02d %s", hour12, minute, suffix)
    }

    private fun renderBlockedApps() {
        blockedAppsGroup.removeAllViews()
        val blockedPackages = prefs.getBlockedPackages()

        blockedAppsEmpty.visibility = if (blockedPackages.isEmpty()) View.VISIBLE else View.GONE
        blockedAppsGroup.visibility = if (blockedPackages.isEmpty()) View.GONE else View.VISIBLE

        blockedPackages
            .mapNotNull { packageName -> resolveAppLabel(packageName) }
            .sortedBy { it.label.lowercase() }
            .forEach { item ->
                val chip = Chip(this).apply {
                    text = item.label
                    isCheckable = false
                    isClickable = false
                    setTextColor(resources.getColor(R.color.app_on_background, theme))
                    chipBackgroundColor = resources.getColorStateList(R.color.app_primary_container, theme)
                    chipStrokeColor = resources.getColorStateList(R.color.app_primary, theme)
                    chipStrokeWidth = resources.displayMetrics.density
                    chipIcon = item.icon
                    isChipIconVisible = item.icon != null
                    chipIconTint = null
                    contentDescription = "${item.label} ${item.packageName}"
                }
                blockedAppsGroup.addView(chip)
            }
    }

    private fun resolveAppLabel(packageName: String): BlockedAppUi? {
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val label = packageManager.getApplicationLabel(applicationInfo).toString()
            val icon = packageManager.getApplicationIcon(applicationInfo)
            BlockedAppUi(label = label, packageName = packageName, icon = icon)
        } catch (_: NameNotFoundException) {
            null
        }
    }

    private data class BlockedAppUi(
        val label: String,
        val packageName: String,
        val icon: Drawable?
    )

    private fun showAccessibilityPromptIfNeeded() {
        if (accessibilityPromptShown || isAccessibilityServiceEnabled()) {
            return
        }

        accessibilityPromptShown = true
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.enable_accessibility_title)
            .setMessage(R.string.enable_accessibility_message)
            .setCancelable(false)
            .setPositiveButton(R.string.open_accessibility_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .setNegativeButton(R.string.not_now) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AppBlockAccessibilityService::class.java)
            .flattenToString()
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()

        if (enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }) {
            return true
        }

        val enabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        return enabled == 1 && enabledServices.contains(expected)
    }
}