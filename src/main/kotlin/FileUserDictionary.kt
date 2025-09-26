import java.io.File

class FileUserDictionary(
    private val fileName: String = "words.txt",
    private val learningThreshold: Int = 3
) : IUserDictionary {

    private val dictionary = try {
        loadDictionary()
    } catch (e: Exception) {
        throw IllegalArgumentException("Некорректный файл")
    }

    override fun getNumOfLearnedWords(): Int {
        return dictionary.count { it.correctAnswerCount &gt;= learningThreshold }
    }

    override fun getSize(): Int {
        return dictionary.size
    }

    override fun getLearnedWords(): List&lt;Word&gt; {
        return dictionary.filter { it.correctAnswerCount &gt;= learningThreshold }
    }

    override fun getUnlearnedWords(): List&lt;Word&gt; {
        return dictionary.filter { it.correctAnswerCount &lt; learningThreshold }
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        dictionary.find { it.questionWord == word }?.correctAnswerCount = correctAnswersCount
        saveDictionary()
    }

    override fun resetUserProgress() {
        dictionary.forEach { it.correctAnswerCount = 0 }
        saveDictionary()
    }

    private fun loadDictionary(): MutableList&lt;Word&gt; {
        val wordFile = File(fileName)
        if (!wordFile.exists()) {
            File("word.txt").copyTo(wordFile)
        }
        val lines = wordFile.readLines()
        val dictionary = mutableListOf&lt;Word&gt;()

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
    }

    private fun saveDictionary() {
        val fileWord = File(fileName)
        val lines = dictionary.map { "${it.questionWord}|${it.translate}|${it.correctAnswerCount}" }
        fileWord.writeText(lines.joinToString("\n"))
    }
}