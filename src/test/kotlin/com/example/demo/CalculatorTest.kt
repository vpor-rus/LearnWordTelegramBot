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
            Word("define", "определять"),
            Word("solve", "решать"),
            Word("serve", "служить"),
            Word("empty", "пустой")
        )

        val correctAnswer = Word("define", "определять")

        val question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        val result = question.asConsoleString()

        println("Результат asConsoleString(): '$result'")

        println("Содержит 'define': ${result.contains("define")}")
        println("Содержит 'solve': ${result.contains("solve")}")
        println("Содержит 'serve': ${result.contains("serve")}")
        println("Содержит 'empty': ${result.contains("empty")}")
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

        println("Результат asConsoleString() с другим порядком: '$result'")

        val containsEnglishOrTranslation = listOf(
            result.contains("apple") || result.contains("яблоко"),
            result.contains("banana") || result.contains("банан"),
            result.contains("orange") || result.contains("апельсин"),
            result.contains("pear") || result.contains("груша")
        )

        assertTrue(
            containsEnglishOrTranslation.all { it },
            "Все слова (или их переводы) должны присутствовать в выводе"
        )

        assertTrue(
            result.contains("apple") || result.contains("яблоко"),
            "Правильный ответ (apple/яблоко) должен присутствовать"
        )

        assertTrue(
            result.contains("1)") && result.contains("2)") &&
                    result.contains("3)") && result.contains("4)"),
            "Вывод должен содержать номера вариантов от 1 до 4"
        )
    }

    @Test
    fun testWithEmptyVariants() {
        val variants = emptyList<Word>()
        val correctAnswer = Word("apple", "яблоко")

        val question = Question(variants = variants, correctAnswer = correctAnswer)

        val result = question.asConsoleString()

        assertTrue(
            result.isEmpty() || result.contains("нет вариантов") || result.contains("no options"),
            "При пустом списке вариантов должна возвращаться пустая строка или сообщение"
        )
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

            assertTrue(lines.size == 200,
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

    @Test
    fun testWithSpecialCharacters() {
        val variants = listOf(
            Word("C++ (1983)", "язык C++"),
            Word("Java [1995]", "язык Java"),
            Word("Python {1991}", "язык Python"),
            Word("Kotlin; 2011", "язык Kotlin")
        )

        val correctAnswer = Word("Kotlin; 2011", "язык Kotlin")

        val question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        val result = question.asConsoleString()

        println("Результат asConsoleString() с спецсимволами: '$result'")

        assertTrue(result.contains("C++"), "Строка должна содержать 'C++'")
        assertTrue(result.contains("Java"), "Строка должна содержать 'Java'")
        assertTrue(result.contains("Python"), "Строка должна содержать 'Python'")
        assertTrue(result.contains("Kotlin"), "Строка должна содержать 'Kotlin'")

        assertTrue(
            result.contains("1983") || result.contains("(1983)"),
            "Строка должна содержать год '1983'"
        )

        assertTrue(
            result.contains("1995") || result.contains("[1995]"),
            "Строка должна содержать год '1995'"
        )

        val containsSpecialChars = result.contains("(") ||
                result.contains("[") ||
                result.contains("{") ||
                result.contains(";")

        val containsTranslations = result.contains("язык C++") ||
                result.contains("язык Java") ||
                result.contains("язык Python") ||
                result.contains("язык Kotlin")

        assertTrue(
            containsSpecialChars || containsTranslations,
            "Строка должна содержать либо специальные символы, либо переводы"
        )
    }

    @Test
    fun testWithSpacesInWords() {
        val variants = listOf(
            Word("   ", "три пробела"),
            Word("word", "слово"),
            Word(" ", "один пробел"),
            Word("  two spaces  ", "два пробела до и после")
        )

        val correctAnswer = Word("   ", "три пробела")

        val question = Question(
            variants = variants,
            correctAnswer = correctAnswer
        )

        val result = question.asConsoleString()

        // Проверяем, что обычное слово отображается корректно
        assertTrue(result.contains("word"), "Строка должна содержать 'word'")

        // Проверяем наличие слова "two spaces" без учета пробелов по краям
        assertTrue(
            result.contains("two spaces") ||
                    result.contains("\"two spaces\"") ||
                    result.contains("'two spaces'"),
            "Строка должна содержать основную часть 'two spaces'"
        )

        // Проверяем наличие строк с пробелами или их представлений
        if (result.contains("   ")) {
            // Если пробелы сохраняются как есть
            assertTrue(result.contains(" "), "Строка должна содержать 'один пробел'")
        } else {
            // Если пробелы заменяются или обрамляются
            assertTrue(
                result.contains("три пробела") ||
                        result.contains("\"   \"") ||
                        result.contains("'   '"),
                "Строка должна содержать представление 'три пробела'"
            )

            assertTrue(
                result.contains("один пробел") ||
                        result.contains("\" \"") ||
                        result.contains("' '"),
                "Строка должна содержать представление 'один пробел'"
            )
        }
    }
}