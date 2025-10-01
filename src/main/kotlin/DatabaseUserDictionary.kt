import java.sql.DriverManager
import java.io.File

fun main() {
    DriverManager.getConnection("jdbc:sqlite:data.db")
        .use { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate(
                """
                      CREATE TABLE IF NOT EXISTS 'words' (
                          'id' integer PRIMARY KEY,
                          'text' varchar,
                          'translate' varchar,
                          'correctAnswersCount' integer DEFAULT 0
                      );
              """.trimIndent()
            )
            statement.executeUpdate("insert into words values(0, 'hello', 'привет', 0)")
        }
}

fun updateDictionary(wordsFile: File) {
    if (!wordsFile.exists()) {
        throw IllegalArgumentException("Файл не существует: ${wordsFile.absolutePath}")
    }

    try {
        val words = wordsFile.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size >= 2) {
                val original = parts[0].trim()
                val translate = parts[1].trim()
                val correctAnswersCount = if (parts.size > 2) parts[2].toIntOrNull() ?: 0 else 0

                Triple(original, translate, correctAnswersCount)
            } else null
        }

        if (words.isEmpty()) {
            println("Предупреждение: файл не содержит корректных слов")
            return
        }

        DriverManager.getConnection("jdbc:sqlite:data.db").use { connection ->
            val alterTableStatement = connection.createStatement()
            try {
                alterTableStatement.executeUpdate(
                    "ALTER TABLE words ADD COLUMN correctAnswersCount INTEGER DEFAULT 0"
                )
                println("Добавлен столбец correctAnswersCount в таблицу words")
            } catch (e: Exception) {

            }

            val insertStatement = connection.prepareStatement(
                """
                INSERT OR REPLACE INTO words (text, translate, correctAnswersCount)
                VALUES (?, ?, ?)
                """.trimIndent()
            )

            connection.autoCommit = false
            words.forEach { (original, translate, correctAnswersCount) ->
                insertStatement.setString(1, original)
                insertStatement.setString(2, translate)
                insertStatement.setInt(3, correctAnswersCount)
                insertStatement.addBatch()
            }

            val results = insertStatement.executeBatch()
            connection.commit()

            println("Успешно обновлено ${results.sum()} слов в словаре")
        }
    } catch (e: Exception) {
        throw RuntimeException("Ошибка при обновлении словаря: ${e.message}", e)
    }
}