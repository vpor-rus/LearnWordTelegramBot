import java.sql.DriverManager

class DatabaseUserDictionary(
    private val dbPath: String = "data.db",
    private val learningThreshold: Int = 3
) : IUserDictionary {

    init {

        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection -&gt;
            val statement = connection.createStatement()
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS words (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    text TEXT NOT NULL,
                    translate TEXT NOT NULL,
                    correctAnswersCount INTEGER DEFAULT 0
                )
            """.trimIndent())
        }
    }

    override fun getNumOfLearnedWords(): Int {
        var count = 0
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection -&gt;
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
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection -&gt;
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
}