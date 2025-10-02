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
}