import additional.LearnWordTrainer
import additional.Question
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class Update(
    @SerialName("update_id")
    val update_id: Long,
    @SerialName("message")
    val message: Message? = null,
    @SerialName("callback_query")
    val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(
    @SerialName("result")
    val result: List<Update>,
)

@Serializable
data class Message(
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String? = null,
    @SerialName("message")
    val message: Message? = null,
)

@Serializable
data class Chat(
    @SerialName("id")
    val id: Long,
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id")
    val chatId: Long,
    @SerialName("text")
    val text: String,
    @SerialName("reply_markup")
    val replyMarkup: ReplyMarkup,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyBoard>>,
)

@Serializable
data class InlineKeyBoard(
    @SerialName("callback_data")
    val calbackData: String,
    @SerialName("text")
    val text: String,
)

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
    private var lastUpdateId = 0L

    val json = Json {
        ignoreUnknownKeys = true
    }

    fun getUpdates(): String {
        val url = "$BASE_URL$botToken/getUpdates?offset=${lastUpdateId + 1}"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return response.body()
    }

    fun processUpdates() {
        val responseString = getUpdates()
        println("Updates: $responseString")
        val response: Response = json.decodeFromString(responseString)
        val updates = response.result

        for (update in updates) {
            if (update.update_id >= lastUpdateId) {
                lastUpdateId = update.update_id
            }

            val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id
            val text = update.message?.text
            val callbackData = update.callbackQuery?.data

            if (chatId != null) {
                handleUpdate(chatId, text, callbackData)
            }
        }
    }

    fun handleUpdate(chatId: Long, text: String?, callbackData: String?) {
        when {
            text == CMD_HELLO -> sendMessage(chatId, "hello")
            text == CMD_MENU -> sendMenu(chatId)
            callbackData != null -> handleCallback(chatId, callbackData)
        }
    }

    fun handleCallback(chatId: Long, callbackData: String) {
        when {
            callbackData == CALLBACK_LEARN_WORDS -> {
                checkNextQuestionAndSend(chatId)
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
                checkNextQuestionAndSend(chatId)
            }

            else -> sendMessage(chatId, "Неизвестная команда: $callbackData")
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

    fun checkNextQuestionAndSend(chatId: Long) {
        val question = trainer.getNextQuestion()
        if (question == null) {
            sendMessage(chatId, "Все слова в словаре выучены")
        } else {
            sendQuestion(chatId, question)
        }
    }
}

fun String.encodeUrl(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())