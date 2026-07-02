package com.pavan.appcurfew

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText

class EmergencyOverrideActivity : AppCompatActivity() {

    private lateinit var prefs: BedtimePrefs
    private lateinit var textProgress: TextView
    private lateinit var textDifficulty: TextView
    private lateinit var textMath: TextView
    private lateinit var textCountdownSeconds: TextView
    private lateinit var editAnswer: TextInputEditText
    private lateinit var progressTimer: LinearProgressIndicator

    private var currentQuestionIndex = 0
    private var totalQuestions = 5
    private var currentTier = 1
    private var currentQuestion: MathQuestion? = null
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_emergency_override)

        prefs = BedtimePrefs(this)
        textProgress = findViewById(R.id.textQuestionProgress)
        textDifficulty = findViewById(R.id.textDifficulty)
        textMath = findViewById(R.id.textMathQuestion)
        textCountdownSeconds = findViewById(R.id.textCountdownSeconds)
        editAnswer = findViewById(R.id.editAnswer)
        progressTimer = findViewById(R.id.progressTimer)

        val attemptCount = prefs.getOverrideAttemptCount()
        currentTier = when {
            attemptCount == 0 -> 1
            attemptCount == 1 -> 2
            else -> 3
        }
        totalQuestions = if (currentTier >= 3) 7 else 5

        setupAnswerListener()
        
        // Only increment when starting the activity (the intention to override)
        prefs.incrementOverrideAttemptCount()

        startQuiz()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startQuiz() {
        currentQuestionIndex = 0
        showNextQuestion()
    }

    private fun showNextQuestion() {
        if (currentQuestionIndex >= totalQuestions) {
            completeQuiz()
            return
        }

        currentQuestion = MathQuestionGenerator.generate(currentTier)
        textProgress.text = getString(R.string.question_progress, currentQuestionIndex + 1, totalQuestions)
        textDifficulty.text = getString(R.string.difficulty_tier, currentTier)
        textMath.text = currentQuestion?.question
        editAnswer.text?.clear()
        
        startTimer(currentQuestion?.secondsAllowed ?: 10)
    }

    private fun setupAnswerListener() {
        editAnswer.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val input = s?.toString() ?: ""
                if (input == currentQuestion?.answer.toString()) {
                    timer?.cancel()
                    currentQuestionIndex++
                    showNextQuestion()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        })
    }

    private fun startTimer(seconds: Int) {
        timer?.cancel()
        val totalMillis = seconds * 1000L
        progressTimer.max = totalMillis.toInt()
        
        timer = object : CountDownTimer(totalMillis, 20) {
            override fun onTick(millisUntilFinished: Long) {
                progressTimer.progress = (totalMillis - millisUntilFinished).toInt()
                val secondsLeft = (millisUntilFinished / 1000) + 1
                textCountdownSeconds.text = "${secondsLeft}s"
            }

            override fun onFinish() {
                textCountdownSeconds.text = "0s"
                failQuiz("Timeout!")
            }
        }.start()
    }

    private fun failQuiz(message: String) {
        timer?.cancel()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        
        // Reset current attempt progress
        currentQuestionIndex = 0
        
        // Keep the difficulty escalation as punishment, but reset the quiz itself
        startQuiz()
    }

    private fun completeQuiz() {
        timer?.cancel()
        // Reset override attempts on successful completion
        prefs.resetOverrideAttemptCount()

        prefs.setOverrideActive(true)
        prefs.setOverrideEndTime(System.currentTimeMillis() + 10 * 60 * 1000)
        Toast.makeText(this, getString(R.string.emergency_access_granted), Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}