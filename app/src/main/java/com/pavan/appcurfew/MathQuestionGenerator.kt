package com.pavan.appcurfew

import kotlin.random.Random

data class MathQuestion(
    val question: String,
    val answer: Int,
    val secondsAllowed: Int
)

object MathQuestionGenerator {

    fun generate(tier: Int): MathQuestion {
        return when (tier) {
            1 -> generateTier1()
            2 -> generateTier2()
            else -> generateTier3()
        }
    }

    private fun generateTier1(): MathQuestion {
        val op = Random.nextInt(4)
        return when (op) {
            0 -> { // Addition: 2-digit + 2-digit
                val a = Random.nextInt(10, 90)
                val b = Random.nextInt(10, 90)
                MathQuestion("$a + $b", a + b, 10)
            }
            1 -> { // Subtraction: 2-digit - 2-digit (no negatives)
                val a = Random.nextInt(20, 99)
                val b = Random.nextInt(10, a)
                MathQuestion("$a - $b", a - b, 10)
            }
            2 -> { // Multiplication: 1-digit * 2-digit
                val a = Random.nextInt(2, 10)
                val b = Random.nextInt(10, 25)
                MathQuestion("$a × $b", a * b, 10)
            }
            else -> { // Division: Clean division
                val b = Random.nextInt(2, 10)
                val ans = Random.nextInt(2, 12)
                val a = b * ans
                MathQuestion("$a ÷ $b", ans, 10)
            }
        }
    }

    private fun generateTier2(): MathQuestion {
        val op = Random.nextInt(4)
        return when (op) {
            0 -> { // Addition: 3-digit + 2-digit
                val a = Random.nextInt(100, 900)
                val b = Random.nextInt(10, 99)
                MathQuestion("$a + $b", a + b, 8)
            }
            1 -> { // Subtraction: 3-digit - 2-digit
                val a = Random.nextInt(100, 999)
                val b = Random.nextInt(10, 99)
                MathQuestion("$a - $b", a - b, 8)
            }
            2 -> { // Multiplication: 2-digit * 2-digit
                val a = Random.nextInt(11, 20)
                val b = Random.nextInt(11, 20)
                MathQuestion("$a × $b", a * b, 8)
            }
            else -> { // Division: Slightly harder clean division
                val b = Random.nextInt(11, 20)
                val ans = Random.nextInt(5, 15)
                val a = b * ans
                MathQuestion("$a ÷ $b", ans, 8)
            }
        }
    }

    private fun generateTier3(): MathQuestion {
        // Two-step questions: e.g., (a * b) + c or (a * b) - c
        val a = Random.nextInt(3, 10)
        val b = Random.nextInt(3, 15)
        val c = Random.nextInt(5, 30)
        
        return if (Random.nextBoolean()) {
            MathQuestion("$a × $b + $c", (a * b) + c, 6)
        } else {
            val product = a * b
            val finalC = if (product > 10) Random.nextInt(5, product) else 2
            MathQuestion("$a × $b - $finalC", (a * b) - finalC, 6)
        }
    }
}