package additional

import Word
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

class LearnWordTrainer(private val learnedAnswerCounter: Int = 3, val numberVariants: Int = 4) {

    private var question: Question? = null
    val dictionary = loadDictionary()


    fun loadDictionary(): List<Word> {
        try {

            val fileWord = File("word.txt")
            val lines = fileWord.readLines()

            val dictionary = mutableListOf<Word>()

            for (line in lines) {
                val separateCell = line.split("|")
                val original = separateCell.getOrNull(0) ?: ""
                val translate = separateCell.getOrNull(1) ?: ""
                val correctAnswerCount = separateCell.getOrNull(2)?.toIntOrNull() ?: 0

                val word = Word(
                    questionWord = original, translate = translate, correctAnswerCount = correctAnswerCount
                )
                dictionary.add(word)
            }
            return dictionary
        } catch (e: IndexOutOfBoundsException) {
            throw IllegalStateException("Некорректный файл")
        }
    }

    fun saveDictionary(dictionary: List<Word>) {

        val fileWord = File("word.txt")
        val lines = dictionary.map { "${it.questionWord}|${it.translate}|${it.correctAnswerCount}" }
        fileWord.writeText(lines.joinToString("\n"))
    }

    fun getStatistics(): Statistics {
        val learnedCount = dictionary.count { it.correctAnswerCount >= learnedAnswerCounter }
        val totalCount = dictionary.size
        val percentCount = if (totalCount != 0) {
            (learnedCount * 100) / totalCount
        } else 0

        return Statistics(
            learnedCount, totalCount, percentCount
        )
    }

    fun getNextQuestion(): Question? {
        val notLearnedList = dictionary.filter { it.correctAnswerCount < learnedAnswerCounter }
        if (notLearnedList.isEmpty()) return null
        val questionWord = if (notLearnedList.size < numberVariants) {
            val learnedList = dictionary.filter { it.correctAnswerCount >= learnedAnswerCounter }.shuffled()
            notLearnedList.shuffled() + learnedList.take(numberVariants - notLearnedList.size)
        } else {
            notLearnedList.shuffled().take(numberVariants)
        }.shuffled()

        val correctAnswerWord = questionWord.random()

        question = Question(
            variants = questionWord, correctAnswer = correctAnswerWord
        )
        return question
    }

    fun checkAnswer(userAnswerIndex: Int?): Boolean {

        return question?.let {
            val correctAnswerId = it.variants.indexOf(it.correctAnswer)

            if (correctAnswerId == userAnswerIndex) {
                it.correctAnswer.correctAnswerCount++
                saveDictionary(dictionary)
                true
            } else {
                false
            }
        } ?: false
    }
}