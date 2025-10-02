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
}