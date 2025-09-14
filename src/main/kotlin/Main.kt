import additional.LearnWordTrainer
import additional.Question
import kotlinx.serialization.Serializable

@Serializable
 data class Word(
    val questionWord: String,
    val translate: String,
    var correctAnswerCount: Int = 0,
)

fun Question.asConsoleString(): String {
    val variants = this.variants.mapIndexed { index, word ->
        "${index + 1} - ${word.translate}"
    }.joinToString("\n")

    return """
        ${this.correctAnswer.questionWord}
        $variants
        
        0 - выйти в меню
    """.trimIndent()
}

fun main() {
    val trainer = try {
        LearnWordTrainer()
    } catch (e: Exception) {
        println ("Невозможно загрузить словарь")
        return
    }

    println("Программа предназначена для изучения иностранных слов\n")

    while (true) {
        println(
            "выберите ваше действие\n" + "1 - Учить слова\n2 - статистика\n0 - выход"
        )
        val choice = readLine()?.toIntOrNull()
        if( choice == null ) {
            println("Некорректный ввод\nВведите 1 или 2 или 0 ")
            continue
        }

        when (choice) {
            1 -> {
                while (true) {

                    val question = trainer.getNextQuestion()

                    if (question == null) {
                        println("Вы выучили все слова, поздравляем")
                        break
                    }

                    println("Как переводится слово: ")
                    println(
                        question.asConsoleString()
                    )

                    println("Введите номер ответа: ")
                    val userAnswerInput = readLine()?.toIntOrNull()

                    if (userAnswerInput == 0) break

                    if (trainer.checkAnswer(userAnswerInput?.minus(1))) {

                        println("Правильно!")
                    } else {
                        println("Неправильно!")
                    }
                }
            }

            2 -> {

                val statistics = trainer.getStatistics()

                println("результат изучения: ${statistics.learnedCount}/${statistics.totalCount} ${statistics.percentCount}%")

            }

            0 -> {
                println("выбрал выход")
                break
            }

            else -> println("некорректный ввод, выберите вариант 0 или 1 или 2")

        }
    }
}

