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

    private val dictionary: IUserDictionary = DatabaseUserDictionary(dbPath = "data.db"),
    private val countOfQuestionWords: Int = 4
) {
    var question: Question? = null

    fun getStatistics(): Statistics {
        val totalCount = dictionary.getSize()
        val learnedCount = dictionary.getNumOfLearnedWords()
        val percentCount = if (totalCount> 0) learnedCount * 100 / totalCount else 0
        return Statistics(learnedCount, totalCount, percentCount)
    }

    fun getNextQuestion(): Question? {
        val unlearnedWords = dictionary.getUnlearnedWords()
        if (unlearnedWords.isEmpty()) {
            return null
        }

        var variants = unlearnedWords.shuffled().take(countOfQuestionWords)
        val questionWord = variants.random()

        if (variants.size < countOfQuestionWords) {
            variants = (variants + dictionary.getLearnedWords()
                .shuffled()
                .take(countOfQuestionWords - variants.size)).shuffled()
        }

        question = Question(variants.shuffled(), questionWord)
        return  question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {
        val currentQuestion = question ?: return false

        return if (userAnswerIndex != null && currentQuestion.variants.getOrNull(userAnswerIndex) == currentQuestion.correctAnswer) {
            val correctWord = currentQuestion.correctAnswer.questionWord
            val currentCorrectAnswers = currentQuestion.correctAnswer.correctAnswerCount
            dictionary.setCorrectAnswersCount(correctWord, currentCorrectAnswers + 1)
            true
        } else {
            false
        }
    }

    fun resetProgress() {
        dictionary.resetUserProgress()
    }
}