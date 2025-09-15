import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Serializable
data class Update(
    @SerialName("update_id")
    val updateId: Long,
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
    val replyMarkup: ReplyMarkup? = null,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard")
    val inlineKeyboard: List<List<InlineKeyBoard>>,
)

@Serializable
data class InlineKeyBoard(
    @SerialName("callback_data")
    val callbackData: String,
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
        const val LEARN_WORDS_CLICKED = "learn_words_clicked"
        const val STATISTIC_CLICKED = "statistic_clicked"
        const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
    }

    private val client = HttpClient.newBuilder().build()
    private var lastUpdateId = 0L
    private val json = Json {
        ignoreUnknownKeys = true
    }

    private fun getUpdates(): Response {
        val url = "$BASE_URL$botToken/getUpdates?offset=${lastUpdateId + 1}"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        return json.decodeFromString(response.body())
    }

    fun processUpdates() {
        val response = getUpdates()
        println("Updates: $response")

        if (response.result.isEmpty()) return

        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, botToken, trainer) }
        lastUpdateId = sortedUpdates.last().updateId
    }

    fun handleUpdate(update: Update, json: Json, botToken: String, trainer: LearnWordTrainer,) {
        val chatId = update.message?.chat?.id ?: update.callbackQuery?.message
            ?.chat?.id ?: return
        val message = update.message?.text
        val data = update.callbackQuery?.data

        when {
            message == CMD_HELLO -> sendMessage(chatId, "hello")
            message == CMD_MENU -> sendMenu(chatId)
            data != null -> handleCallback(chatId, data)
        }
    }

    private fun handleCallback(chatId: Long, callbackData: String) {
        when {
            callbackData == LEARN_WORDS_CLICKED -> {
                checkNextQuestionAndSend(chatId)
            }
            callbackData == STATISTIC_CLICKED -> {
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

    private fun sendMessage(chatId: Long, text: String) {
        val url = "$BASE_URL$botToken/sendMessage?chat_id=$chatId&text=${text.encodeUrl()}"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun sendMenu(chatId: Long) {
        val url = "$BASE_URL$botToken/sendMessage"

        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyBoard(
                            callbackData = LEARN_WORDS_CLICKED,
                            text = "Изучать слова"
                        ),
                        InlineKeyBoard(
                            callbackData = STATISTIC_CLICKED,
                            text = "Статистика"
                        )
                    )
                )
            )
        )

        val requestBodyString = json.encodeToString(SendMessageRequest.serializer(), requestBody)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun sendQuestion(chatId: Long, question: Question) {
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.questionWord,
            replyMarkup = ReplyMarkup(
                listOf(question.variants.mapIndexed { index, word ->
                    InlineKeyBoard(
                        callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index",
                        text = word.translate
                    )
                })
            )
        )

        val requestBodyString = json.encodeToString(SendMessageRequest.serializer(), requestBody)
        val url = "$BASE_URL$botToken/sendMessage"

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
            .build()

        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun checkNextQuestionAndSend(chatId: Long) {
        val question = trainer.getNextQuestion()
        if (question == null) {
            sendMessage(chatId, "Все слова в словаре выучены")
        } else {
            sendQuestion(chatId, question)
        }
    }
}

fun String.encodeUrl(): String = URLEncoder.encode(this, Charsets.UTF_8.name())