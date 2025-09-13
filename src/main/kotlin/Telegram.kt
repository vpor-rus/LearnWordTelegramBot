import additional.LearnWordTrainer
import additional.Question
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Not found token")
        return
    }

    val botToken = args[0]
    val trainer = LearnWordTrainer()
    val botService = TelegramBotService(botToken, trainer)

    while (true) {
        Thread.sleep(TIME_SLEEP)
        botService.processUpdates()
    }
}

const val TIME_SLEEP: Long = 2000

class TelegramBotService(private val botToken: String, private val trainer: LearnWordTrainer) {

    companion object {
        const val BASE_URL = "https://api.telegram.org/bot"

        const val CMD_HELLO = "hello"
        const val CMD_MENU = "menu"

        const val CALLBACK_LEARN_WORDS = "learn_words_clicked"
        const val CALLBACK_STATISTIC = "statistic_clicked"
        const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
    }

    private val client = HttpClient.newBuilder().build()
    private var lastUpdateId = 0

    val messageTextRegex: Regex = "\"text\":\"(.+?)\"".toRegex()
    val updateIdRegex = "\"update_id\":(\\d+)".toRegex()
    val chatIdRegex = "\"chat\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*(-*\\d+)".toRegex()
    val dataRegex: Regex = "\"data\":\"(.+?)\"".toRegex()


    fun getUpdates(): String {
        val url = "$BASE_URL$botToken/getUpdates?offset=$lastUpdateId"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun processUpdates() {
        val updates = getUpdates()
        println("Updates: $updates")

        val updateIds = updateIdRegex.findAll(updates).map { it.groupValues[1].toInt() }.toList()
        if (updateIds.isNotEmpty()) {
            lastUpdateId = updateIds.maxOrNull()!! + 1
        }

        val texts = messageTextRegex.findAll(updates).map { it.groupValues[1] }.toList()
        val chatIds = chatIdRegex.findAll(updates).map { it.groupValues[1].toLong() }.toList()
        val dataList = dataRegex.findAll(updates).map { it.groupValues[1] }.toList()

        for ((index, updateId) in updateIds.withIndex()) {
            val chatId = chatIds.getOrNull(index)
            val text = texts.getOrNull(index)
            val callbackData = dataList.getOrNull(index)
            if (chatId != null && text != null) {
                handleUpdate(chatId, text, updateId, callbackData)
            }
        }
    }

    fun handleUpdate(chatId: Long, text: String, updateId: Int, callbackData: String?) {
        if (updateId >= this.lastUpdateId) {
            this.lastUpdateId = updateId + 1
        }

        if (text == CMD_HELLO) {
            sendMessage(chatId, "hello")
        }

        if (text == CMD_MENU) {
            sendMenu(chatId)
        }

        if (callbackData != null) {
            when {
                callbackData == CALLBACK_LEARN_WORDS -> {
                    checkNextQuestionAndSend(trainer, this, chatId)
                }

                callbackData == CALLBACK_STATISTIC -> {
                    val stats = trainer.getStatistics()
                    val message =
                        "Результат изучения: ${stats.learnedCount}/${stats.totalCount} (${stats.percentCount}%)"
                    sendMessage(chatId, message)
                }

                callbackData.startsWith(CALLBACK_DATA_ANSWER_PREFIX) -> {
                    val indexStr = callbackData.removePrefix(CALLBACK_DATA_ANSWER_PREFIX)
                    val userAnswerIndex = indexStr.toIntOrNull()
                    val isCorrect = trainer.checkAnswer(userAnswerIndex)
                    val response = if (isCorrect) "Правильно!" else "Неправильно"
                    sendMessage(chatId, response)
                    checkNextQuestionAndSend(trainer, this, chatId)
                }

                else -> sendMessage(chatId, "Неизвестная команда: $callbackData")
            }
        }
    }

    fun sendMessage(chatId: Long, text: String) {
        val url = "$BASE_URL$botToken/sendMessage?chat_id=$chatId&text=${text.encodeUrl()}"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendMenu(chatId: Long) {
        val url = "$BASE_URL$botToken/sendMessage"
        val sendMenuBody = """
            {
              "chat_id": "$chatId",
              "text": "Основное меню",
              "reply_markup": {
                "inline_keyboard": [
                  [
                    {
                      "text": "Изучить слова",
                      "callback_data": "$CALLBACK_LEARN_WORDS"
                    },
                    {
                      "text": "Статистика",
                      "callback_data": "$CALLBACK_STATISTIC"
                    }
                  ]
                ]
              }
            }
        """.trimIndent()
        val request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(sendMenuBody)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendQuestion(chatId: Long, question: Question) {
        val optionsJson = question.variants.mapIndexed { index, word ->
            """
        {
          "text": "${word.translate}",
          "callback_data": "${CALLBACK_DATA_ANSWER_PREFIX}$index"
        }
        """.trimIndent()
        }.joinToString(separator = ",")

        val body = """
        {
          "chat_id": "$chatId",
          "text": "${question.correctAnswer.original}",
          "reply_markup": {
            "inline_keyboard": [
              [ $optionsJson ]
            ]
          }
        }
    """.trimIndent()

        val url = "$BASE_URL$botToken/sendMessage"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun checkNextQuestionAndSend(
        trainer: LearnWordTrainer, telegramBotService: TelegramBotService, chatId: Long
    ) {
        val question = trainer.getNextQuestion()
        if (question == null) {
            telegramBotService.sendMessage(chatId, "Все слова в словаре выучены")
        } else {
            telegramBotService.sendQuestion(chatId, question)
        }
    }

}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())
