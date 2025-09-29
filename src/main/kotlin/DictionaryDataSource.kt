import java.io.File
import java.sql.DriverManager

class DatabaseUserDictionary(
    private val dbPath: String = "data.db",
    private val learningThreshold: Int = 3
) : IUserDictionary {

    init {

        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.createStatement()

            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL UNIQUE,
                    translate TEXT NOT NULL,
                    correctAnswersCount INTEGER DEFAULT 0
                )
            """.trimIndent()
            )
        }
    }

    override fun getNumOfLearnedWords(): Int {
        var count = 0
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM words WHERE correctAnswersCount &gt;= ?"
            )
            statement.setInt(1, learningThreshold)
            val resultSet = statement.executeQuery()
            if (resultSet.next()) {
                count = resultSet.getInt(1)
            }
        }
        return count
    }

    override fun getSize(): Int {
        var count = 0
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery("SELECT COUNT(*) FROM words")
            if (resultSet.next()) {
                count = resultSet.getInt(1)
            }
        }
        return count
    }

    override fun getLearnedWords(): List<Word> {
        val learnedWords = mutableListOf<Word>()
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount >= ?"
            )
            statement.setInt(1, learningThreshold)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val word = Word(
                    questionWord = resultSet.getString("text"),
                    translate = resultSet.getString("translate"),
                    correctAnswerCount = resultSet.getInt("correctAnswersCount")
                )
                learnedWords.add(word)
            }
        }
        return learnedWords
    }

    override fun getUnlearnedWords(): List<Word> {
        val unlearnedWords = mutableListOf<Word>()
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement(
                "SELECT text, translate, correctAnswersCount FROM words WHERE correctAnswersCount < ?"
            )
            statement.setInt(1, learningThreshold)
            val resultSet = statement.executeQuery()
            while (resultSet.next()) {
                val word = Word(
                    questionWord = resultSet.getString("text"),
                    translate = resultSet.getString("translate"),
                    correctAnswerCount = resultSet.getInt("correctAnswersCount")
                )
                unlearnedWords.add(word)
            }
        }
        return unlearnedWords
    }

    override fun setCorrectAnswersCount(word: String, correctAnswersCount: Int) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement(
                "UPDATE words SET correctAnswersCount = ? WHERE text = ?"
            )
            statement.setInt(1, correctAnswersCount)
            statement.setString(2, word)
            statement.executeUpdate()
        }
    }

    override fun resetUserProgress() {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate("UPDATE words SET correctAnswersCount = 0")
        }
    }

    fun importFromFile(file: File) {
        if (!file.exists()) {
            throw IllegalArgumentException("Файл не существует: ${file.absolutePath}")
        }

        val words = file.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                val original = parts[0].trim()
                val translate = parts[1].trim()
                val correctAnswersCount = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0

                Triple(original, translate, correctAnswersCount)
            } else null
        }

        if (words.isEmpty()) {
            return
        }

        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val insertStatement = connection.prepareStatement(
                "INSERT OR REPLACE INTO words (text, translate, correctAnswersCount) VALUES (?, ?, ?)"
            )

            connection.autoCommit = false
            try {
                words.forEach { (original, translate, correctAnswersCount) ->
                    insertStatement.setString(1, original)
                    insertStatement.setString(2, translate)
                    insertStatement.setInt(3, correctAnswersCount)
                    insertStatement.addBatch()
                }

                insertStatement.executeBatch()
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw e
            } finally {
                connection.autoCommit = true
            }
        }
    }

    /**
     * Добавляет новое слово в словарь
     */
    fun addWord(original: String, translate: String, correctAnswersCount: Int = 0) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement(
                "INSERT OR REPLACE INTO words (text, translate, correctAnswersCount) VALUES (?, ?, ?)"
            )
            statement.setString(1, original)
            statement.setString(2, translate)
            statement.setInt(3, correctAnswersCount)
            statement.executeUpdate()
        }
    }

    /**
     * Удаляет слово из словаря
     */
    fun removeWord(original: String) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.prepareStatement("DELETE FROM words WHERE text = ?")
            statement.setString(1, original)
            statement.executeUpdate()
        }
    }

    /**
     * Очищает весь словарь
     */
    fun clearDictionary() {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate("DELETE FROM words")
        }
    }
}