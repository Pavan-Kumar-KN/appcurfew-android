package com.pavan.appcurfew

import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

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
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            updateUIState()
            handler.postDelayed(this, 5000)
        }
    }

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

        setupListeners()
        updateUIState()

        val basePadding = (24 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left + basePadding,
                systemBars.top + basePadding,
                systemBars.right + basePadding,
                systemBars.bottom + basePadding
            )
            insets
        }
    }

    private fun setupListeners() {
        // Use setOnClickListener for the master toggle to intercept clicks before they change the check state
        enableSwitch.setOnClickListener {
            val isCurrentlyChecked = enableSwitch.isChecked
            val wasEnabled = prefs.isBlockingEnabled()
            val isBedtime = prefs.isWithinActiveWindow()

            if (wasEnabled && isBedtime && !isCurrentlyChecked && !prefs.isOverrideActive()) {
                // Intercept manual disable during bedtime: Force it back to ON and show protection
                enableSwitch.isChecked = true
                showDisableProtectionDialog()
            } else {
                // Normal toggle behavior
                if (isCurrentlyChecked && prefs.getPinCode() == null) {
                    // First time enable: Setup PIN
                    enableSwitch.isChecked = false // Reset until PIN is set
                    showSetupPinDialog {
                        prefs.setBlockingEnabled(true)
                        updateUIState()
                    }
                } else {
                    prefs.setBlockingEnabled(isCurrentlyChecked)
                    updateUIState()
                }
            }
        }

        startTimeButton.setOnClickListener {
            pickTime(prefs.getStartMinutes()) {
                prefs.setStartMinutes(it)
                updateUIState()
            }
        }

        endTimeButton.setOnClickListener {
            pickTime(prefs.getEndMinutes()) {
                prefs.setEndMinutes(it)
                updateUIState()
            }
        }

        selectAppsButton.setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, R.string.accessibility_help, Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        showAccessibilityPromptIfNeeded()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun updateUIState() {
        val isEnabled = prefs.isBlockingEnabled()
        
        enableSwitch.isChecked = isEnabled

        startTimeButton.text = formatMinutes(prefs.getStartMinutes())
        endTimeButton.text = formatMinutes(prefs.getEndMinutes())
        
        val blockedCount = prefs.getBlockedPackages().size
        whitelistSummary.text = getString(
            R.string.whitelist_summary,
            blockedCount,
            if (isEnabled) getString(R.string.enabled) else getString(R.string.disabled)
        )

        renderBlockedApps()
        updateAccessibilityButtonVisibility()
    }

    private fun showSetupPinDialog(onComplete: () -> Unit) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_disable_protection, null)
        val title = view.findViewById<TextView>(R.id.textProtectionTitle)
        val message = view.findViewById<TextView>(R.id.textProtectionMessage)
        val pinInput = view.findViewById<TextInputEditText>(R.id.editPin)
        val confirmBtn = view.findViewById<Button>(R.id.buttonConfirmDisable)
        val cancelBtn = view.findViewById<Button>(R.id.buttonCancelDisable)

        title.text = getString(R.string.setup_pin_title)
        message.text = getString(R.string.setup_pin_message)
        confirmBtn.text = getString(R.string.save)
        confirmBtn.isEnabled = false

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(false)
            .show()

        pinInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                confirmBtn.isEnabled = s?.length == 4
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        confirmBtn.setOnClickListener {
            prefs.setPinCode(pinInput.text.toString())
            dialog.dismiss()
            onComplete()
        }

        cancelBtn.setOnClickListener { 
            updateUIState()
            dialog.dismiss() 
        }
    }

    private fun showDisableProtectionDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_disable_protection, null)
        val pinInput = view.findViewById<TextInputEditText>(R.id.editPin)
        val pinLayout = view.findViewById<TextInputLayout>(R.id.pinInputLayout)
        val countdownText = view.findViewById<TextView>(R.id.textCountdown)
        val confirmBtn = view.findViewById<Button>(R.id.buttonConfirmDisable)
        val cancelBtn = view.findViewById<Button>(R.id.buttonCancelDisable)

        confirmBtn.text = getString(R.string.save) 
        confirmBtn.isEnabled = false

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(false)
            .show()

        pinInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                confirmBtn.isEnabled = s?.length == 4
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })

        confirmBtn.setOnClickListener {
            val enteredPin = pinInput.text.toString()
            if (enteredPin == prefs.getPinCode()) {
                pinLayout.visibility = View.GONE
                countdownText.visibility = View.VISIBLE
                confirmBtn.isEnabled = false
                
                object : CountDownTimer(10000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        countdownText.text = getString(R.string.wait_seconds, millisUntilFinished / 1000)
                    }

                    override fun onFinish() {
                        countdownText.visibility = View.GONE
                        confirmBtn.isEnabled = true
                        confirmBtn.text = getString(R.string.confirm_disable)
                        confirmBtn.setOnClickListener {
                            prefs.setOverrideActive(true)
                            prefs.setOverrideEndTime(System.currentTimeMillis() + 10 * 60 * 1000)
                            prefs.setBlockingEnabled(false)
                            updateUIState()
                            Toast.makeText(this@MainActivity, R.string.override_active_msg, Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }
                }.start()
            } else {
                pinLayout.error = getString(R.string.wrong_pin)
            }
        }

        cancelBtn.setOnClickListener { 
            updateUIState()
            dialog.dismiss() 
        }
    }

    private fun updateAccessibilityButtonVisibility() {
        accessibilityButton.visibility = if (isAccessibilityServiceEnabled()) View.GONE else View.VISIBLE
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
                    setTextColor(getColor(R.color.text_primary))
                    chipBackgroundColor = getColorStateList(R.color.app_primary_container)
                    chipStrokeColor = getColorStateList(R.color.app_primary)
                    chipStrokeWidth = resources.displayMetrics.density
                    chipIcon = item.icon
                    isChipIconVisible = item.icon != null
                    chipIconTint = null
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
        if (accessibilityPromptShown || isAccessibilityServiceEnabled()) return

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
        val expected = ComponentName(this, AppBlockAccessibilityService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}