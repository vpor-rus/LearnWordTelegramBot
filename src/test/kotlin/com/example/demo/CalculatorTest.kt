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

    @Test
    fun testWithEmptyVariants() {
        val variants = emptyList<Word>()
        val correctAnswer = Word("apple", "яблоко")

        assertThrows<IllegalArgumentException> {
            Question(variants = variants, correctAnswer = correctAnswer).asConsoleString()
        }
    }

    @Test
    fun testWithTenVariants() {
        val variants = listOf(
            Word("apple", "яблоко"),
            Word("banana", "банан"),
            Word("orange", "апельсин"),
            Word("pear", "груша"),
            Word("grape", "виноград"),
            Word("lemon", "лимон"),
            Word("cherry", "вишня"),
            Word("plum", "слива"),
            Word("peach", "персик"),
            Word("apricot", "абрикос")
        )

        val correctAnswer = Word("apple", "яблоко")

        val question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        val result = question.asConsoleString()

        println("Результат asConsoleString() с 10 вариантами: '$result'")

        println("Содержит 'apple': ${result.contains("apple")}")
        println("Содержит 'banana': ${result.contains("banana")}")
        println("Содержит 'grape': ${result.contains("grape")}")
        println("Содержит 'apricot': ${result.contains("apricot")}")

        println("Содержит '1)': ${result.contains("1)")}")
        println("Содержит '5)': ${result.contains("5)")}")
        println("Содержит '10)': ${result.contains("10)")}")
    }

    @Test
    fun testWithTooManyVariants() {
        val variants = List(200) { index ->
            Word("word$index", "слово$index")
        }

        val correctAnswer = Word("word50", "слово50")

        try {
            val question = Question(
                variants = variants,
                correctAnswer = correctAnswer
            )

            val result = question.asConsoleString()

            val lines = result.split("\n").filter { it.isNotEmpty() }

            assertTrue(lines.size < 200,
            "Должно быть ограничение на количество отображаемых вариантов")

            assertTrue(result.contains("word50"),
                "Правильный ответ должен присутствовать даже при большом количестве вариантов")

            println("Метод обработал 200 вариантов без исключения. Количество строк: ${lines.size}")
        } catch (e: IllegalArgumentException) {
            println("Выброшено исключение при 200 вариантах: ${e.message}")
            assertTrue(e.message?.contains("too many") == true ||
                    e.message?.contains("слишком много") == true,
                "Сообщение об ошибке должно указывать на слишком большое количество вариантов")
        }
    }
}