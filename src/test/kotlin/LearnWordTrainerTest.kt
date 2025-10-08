import kotlin.test.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LearnWordTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`()
    {
        val dictionary = FileUserDictionary("src/test/4_word_of_7.txt")
        val trainer = LearnWordTrainer(dictionary)
        kotlin.test.assertEquals(
            Statistics(learnedCount = 0, totalCount = 28, percentCount = 0),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val dictionary = FileUserDictionary("src/test/statistics_with_corrupted_file.txt")
        val trainer = LearnWordTrainer(dictionary)

        kotlin.test.assertEquals(
            Statistics(learnedCount = 0, totalCount = 28, percentCount = 0),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test getNextQuestion with 5 unlearned words`() {
        val dictionary = FileUserDictionary("src/test/getNextQuestion_with_5_unlearned_words.txt")

        val trainer = LearnWordTrainer(dictionary)
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")
        kotlin.test.assertTrue(question.variants.size >= 4, "Должно быть минимум 4 варианта ответа")
        kotlin.test.assertTrue(question.variants.contains(question.correctAnswer),
            "Варианты должны содержать правильный ответ")
    }

    @Test
    fun `test getNextQuestion with 1 unlearned word`()  {
        val dictionary = FileUserDictionary("src/test/getNextQuestion_with_1_unlearned_word.txt")
        val trainer = LearnWordTrainer(dictionary)
        val question = trainer.getNextQuestion()

        assertNotNull(question, "Вопрос не должен быть null")

        assertTrue(question.variants.isNotEmpty(), "Должен быть хотя бы один вариант")

        kotlin.test.assertTrue(question.variants.contains(question.correctAnswer),
            "Правильный ответ должен быть в списке вариантов")

        kotlin.test.assertEquals(
            question.correctAnswer,
            question.variants.find { it == question.correctAnswer },
            "Единственный вариант должен быть правильным ответом"
        )
    }

    @Test
    fun `test getNextQuestion with empty dictionary`()  {

        val trainer = LearnWordTrainer(EMPTY_DICTIONARY)

        val exception = assertThrows(IllegalStateException::class.java) {
            trainer.getNextQuestion()
        }
        assertEquals("Словарь пуст!", exception.message)
    }

    @Test
    fun `test checkAnswer with true`() {
        val dictionary = FileUserDictionary("src/test/checkAnswer_with_true.txt")
        val trainer = LearnWordTrainer(dictionary)

        try {
            val question = trainer.getNextQuestion()

            val correctIndex = question?.variants?.indexOf(question.correctAnswer) ?: 0
            kotlin.test.assertTrue(correctIndex >= 0, "Правильный ответ должен быть в списке вариантов")

            val result = trainer.checkAnswer(correctIndex)
            kotlin.test.assertTrue(result, "Результат должен быть true для правильного ответа")
        } catch (e: Exception) {
            e.printStackTrace()
            kotlin.test.fail("Ошибка при тестировании: ${e.message}")
        }
    }

    @Test
    fun `test checkAnswer with false`() {
        val dictionary = FileUserDictionary("src/test/checkAnswer_with_false.txt")

        val trainer = LearnWordTrainer(dictionary)
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")
        val correctIndex = question.variants.indexOf(question.correctAnswer)

        val wrongIndex = if (correctIndex == 0) 1 else 0

        assertNotEquals(correctIndex, wrongIndex, "Индексы должны быть разными")

        val result = trainer.checkAnswer(wrongIndex)

        assertFalse(result, "Результат должен быть false для неправильного ответа")
    }

    @Test
    fun `test checkAnswer with null index`() {
        val dictionary = FileUserDictionary("src/test/checkAnswer_with_null_index.txt")

        val trainer = LearnWordTrainer(dictionary)
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")

        val result = trainer.checkAnswer(null)

        assertFalse(result, "Результат должен быть false для null индекса")
    }companion object{
        val EMPTY_DICTIONARY = object : IUserDictionary {
            override fun getNumOfLearnedWords() = 0
            override fun getSize() = 0
            override fun getLearnedWords() = emptyList<Word>()
            override fun getUnlearnedWords() = emptyList<Word>()
            override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {}
            override fun resetUserProgress() {}
        }
    }
}