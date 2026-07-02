package com.pavan.appcurfew

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.random.Random

class BlockedWarningActivity : AppCompatActivity() {

    private lateinit var prefs: BedtimePrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_blocked_warning)

        prefs = BedtimePrefs(this)
        
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        
        setupUI(packageName)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI(packageName: String) {
        val harshMessages = listOf(
            getString(R.string.harsh_message_1),
            getString(R.string.harsh_message_2),
            getString(R.string.harsh_message_3),
            getString(R.string.harsh_message_4),
            getString(R.string.harsh_message_5),
            getString(R.string.harsh_message_6),
            getString(R.string.harsh_message_7),
            getString(R.string.harsh_message_8),
            getString(R.string.harsh_message_9),
            getString(R.string.harsh_message_10)
        )

        findViewById<TextView>(R.id.textHarshMessage).text = harshMessages[Random.nextInt(harshMessages.size)]
        
        val appAttempts = if (packageName.isNotEmpty()) prefs.getAttemptCount(packageName) else 0
        findViewById<TextView>(R.id.textAppAttemptCount).text = getString(R.string.app_attempt_count, appAttempts)
        
        val totalAttempts = prefs.getTotalAttemptCount()
        findViewById<TextView>(R.id.textTotalAttemptCount).text = getString(R.string.total_attempt_count, totalAttempts)

        val endMinutes = prefs.getEndMinutes()
        val hour24 = endMinutes / 60
        val minute = endMinutes % 60
        val suffix = if (hour24 >= 12) "PM" else "AM"
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val endTimeStr = String.format("%d:%02d %s", hour12, minute, suffix)
        
        val remaining = prefs.getRemainingMinutesUntilUnlock()
        findViewById<TextView>(R.id.textSessionStatus).text = getString(R.string.session_status, endTimeStr, remaining)

        findViewById<Button>(R.id.buttonEmergencyOverride).setOnClickListener {
            startActivity(Intent(this, EmergencyOverrideActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.buttonReturnHome).setOnClickListener {
            val startMain = Intent(Intent.ACTION_MAIN)
            startMain.addCategory(Intent.CATEGORY_HOME)
            startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(startMain)
            finish()
        }
    }

    override fun onBackPressed() {
        // Disable back button to force the user to go home
        super.onBackPressed()
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }
}