import kotlin.test.Test

class LearnWordTrainerTest {

    @Test
    fun `test statistics with 4 words of 7`()
    {
        val trainer = LearnWordTrainer("src/test/4_word_of_7.txt")
        kotlin.test.assertEquals(
            Statistics(learnedCount = 4, totalCount = 7, percentCount = 57),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test statistics with corrupted file`() {
        val trainer = LearnWordTrainer("src/test/corrupted_file.txt")
        kotlin.test.assertEquals(
            Statistics(learnedCount = 0, totalCount = 0, percentCount = 0),
            trainer.getStatistics()
        )
    }

    @Test
    fun `test getNextQuestion with 5 unlearned words`() {
        val trainer = LearnWordTrainer("src/test/5_unlearned_words.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")
        kotlin.test.assertTrue(question.variants.size >= 4, "Должно быть минимум 4 варианта ответа")
        kotlin.test.assertTrue(question.variants.contains(question.correctAnswer),
            "Варианты должны содержать правильный ответ")
    }

    @Test
    fun `test getNextQuestion with 1 unlearned word`() {
        val trainer = LearnWordTrainer("src/test/1_unlearned_word.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")
        kotlin.test.assertTrue(question.variants.size >= 1, "Должен быть минимум 1 вариант ответа")
        kotlin.test.assertEquals(question.variants[0], question.correctAnswer,
            "Единственный вариант должен быть правильным ответом")
    }

    @Test
    fun `test getNextQuestion with all words learned`() {
        val trainer = LearnWordTrainer("src/test/all_words_learned.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNull(question, "Вопрос должен быть null, когда все слова изучены")
    }

    @Test
    fun `test checkAnswer with true`() {
        val trainer = LearnWordTrainer("src/test/words_for_check.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")

        val correctIndex = question.variants.indexOf(question.correctAnswer)
        kotlin.test.assertTrue(correctIndex >= 0, "Правильный ответ должен быть в списке вариантов")

        val result = trainer.checkAnswer(correctIndex)

        kotlin.test.assertTrue(result, "Результат должен быть true для правильного ответа")
    }

    @Test
    fun `test checkAnswer with false`() {
        val trainer = LearnWordTrainer("src/test/words_for_check.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")
        val correctIndex = question.variants.indexOf(question.correctAnswer)

        val wrongIndex = if (correctIndex == 0) 1 else 0

        kotlin.test.assertNotEquals(correctIndex, wrongIndex, "Индексы должны быть разными")

        val result = trainer.checkAnswer(wrongIndex)

        kotlin.test.assertFalse(result, "Результат должен быть false для неправильного ответа")
    }

    @Test
    fun `test checkAnswer with null index`() {
        val trainer = LearnWordTrainer("src/test/words_for_check.txt")
        val question = trainer.getNextQuestion()

        kotlin.test.assertNotNull(question, "Вопрос не должен быть null")

        val result = trainer.checkAnswer(null)

        kotlin.test.assertFalse(result, "Результат должен быть false для null индекса")
    }
}