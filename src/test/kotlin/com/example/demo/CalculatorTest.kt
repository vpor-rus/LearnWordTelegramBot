package com.example.demo

import Question
import Word
import asConsoleString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class CalculatorTest {
    @Test
    fun testWithFourVariants() {

        val variants = listOf(
            Word("apple", "яблоко"),
            Word("banana", "банан"),
            Word("orange", "апельсин"),
            Word("pear", "груша")
        )

        val correctAnswer = Word("apple", "яблоко")

        val question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        val result = question.asConsoleString()

        println("Результат asConsoleString(): '$result'")

        println("Содержит 'apple': ${result.contains("apple")}")
        println("Содержит 'banana': ${result.contains("banana")}")
        println("Содержит 'orange': ${result.contains("orange")}")
        println("Содержит 'pear': ${result.contains("pear")}")
        println("Содержит '1)': ${result.contains("1)")}")
        println("Содержит '2)': ${result.contains("2)")}")
        println("Содержит '3)': ${result.contains("3)")}")
        println("Содержит '4)': ${result.contains("4)")}")

    }

    @Test
    fun testWithDifferentOrder() {
        val variants = listOf(
            Word("pear", "груша"),
            Word("orange", "апельсин"),
            Word("banana", "банан"),
            Word("apple", "яблоко")
        )

        val correctAnswer = Word("apple", "яблоко")
        val question = Question(variants = variants, correctAnswer = correctAnswer)
        val result = question.asConsoleString()

        assertTrue(result.contains("apple"), "Строка должна содержать 'apple'")
        assertTrue(result.contains("banana"), "Строка должна содержать 'banana'")
        assertTrue(result.contains("orange"), "Строка должна содержать 'orange'")
        assertTrue(result.contains("pear"), "Строка должна содержать 'pear'")

        assertTrue(result.contains("apple"), "Правильный ответ 'apple' должен присутствовать")
    }
}