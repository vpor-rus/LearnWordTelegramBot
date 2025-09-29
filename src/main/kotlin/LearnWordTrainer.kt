import java.io.File

data class Statistics(
    val learnedCount: Int,
    val totalCount: Int,
    val percentCount: Int,
)

data class Question(
    val variants: List<Word>,
    val correctAnswer: Word,
)

class LearnWordTrainer(
    private  val fileName: String = "words.txt",
    private val learnedAnswerCounter: Int = 3,
    val countOfQuestionWords: Int = 4
) {
    internal var question: Question? = null
    val dictionary = DatabaseUserDictionary(dbPath = "data.db")

    private fun getStatistics(): Statistics {
        val totalCount = dictionary.getSize()
        val learnedCount = dictionary.getNumOfLearnedWords()
        val percentCount = if (totalCount> 0) learnedCount * 100 / totalCount else 0
        return Statistics(learnedCount, totalCount, percentCount)
    }

    internal fun getNextQuestion(): Question {
        val unlearnedWords = dictionary.getUnlearnedWords()
        if (unlearnedWords.isEmpty()) {
            val allWords = dictionary.getLearnedWords() + dictionary.getUnlearnedWords()
            if (allWords.isEmpty()) {
                throw IllegalStateException("Словарь пуст!")
            }
            val questionWord = allWords.random()

            val variants = mutableSetOf<Word>()
            variants.add(questionWord)

            while (variants.size < countOfQuestionWords && variants.size < allWords.size) {
                val randomWord = allWords.random()
                variants.add(randomWord)
            }

            return Question(variants.shuffled(), questionWord)
        }

        val questionWord = unlearnedWords.random()

        val variants = mutableSetOf<Word>()
        variants.add(questionWord)

        val allWords = dictionary.getLearnedWords() + dictionary.getUnlearnedWords()
        while (variants.size < countOfQuestionWords && variants.size < allWords.size) {
            val randomWord = allWords.random()
            variants.add(randomWord)
        }

        return Question(variants.shuffled(), questionWord)
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        return if (userAnswerIndex != null && question.variants.getOrNull(userAnswerIndex) == question.correctAnswer) {
            val correctWord = question.correctAnswer.questionWord
            val currentCorrectAnswers = question.correctAnswer.correctAnswerCount
            dictionary.setCorrectAnswersCount(correctWord, currentCorrectAnswers + 1)
            true
        } else {
            false
        }
    }


    fun resetProgress() {
        dictionary.forEach { it.correctAnswerCount = 0 }
        saveDictionary()
    }
}