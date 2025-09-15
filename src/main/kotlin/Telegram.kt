import additional.LearnWordTrainer
import additional.Question
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TIME_SLEEP: Long = 2000
const val BASE_URL = "https://api.telegram.org/bot"
const val CMD_HELLO = "hello"
const val CMD_MENU = "menu"
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTIC_CLICKED = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id") val update_id: Long,
    @SerialName("message") val message: Message? = null,
    @SerialName("callback_query") val callbackQuery: CallbackQuery? = null,
)

@Serializable
data class Response(@SerialName("result") val result: List<Update>)

@Serializable
data class Message(
    @SerialName("text") val text: String? = null,
    @SerialName("chat") val chat: Chat,
)

@Serializable
data class CallbackQuery(
    @SerialName("data") val data: String? = null,
    @SerialName("message") val message: Message? = null,
)

@Serializable
data class Chat(@SerialName("id") val id: Long)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id") val chatId: Long,
    @SerialName("text") val text: String,
    @SerialName("reply_markup") val replyMarkup: ReplyMarkup,
)

@Serializable
data class ReplyMarkup(
    @SerialName("inline_keyboard") val inlineKeyboard: List<List<InlineKeyBoard>>,
)

@Serializable
data class InlineKeyBoard(
    @SerialName("callback_data") val callbackData: String,
    @SerialName("text") val text: String,
)

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Not found token")
        return
    }

    val botToken = args[0]
    val trainer = LearnWordTrainer()
    val client = HttpClient.newBuilder().build()
    val json = Json { ignoreUnknownKeys = true }
    var lastUpdateId = 0L

    fun String.encodeUrl() = URLEncoder.encode(this, Charsets.UTF_8.name())

    fun sendMessage(chatId: Long, text: String) {
        val url = "$BASE_URL$botToken/sendMessage?chat_id=$chatId&text=${text.encodeUrl()}"
        val request = HttpRequest.newBuilder().uri(URI.create(url)).build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendMenu(chatId: Long) {
        val url = "$BASE_URL$botToken/sendMessage"
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = "Основное меню",
            replyMarkup = ReplyMarkup(
                listOf(
                    listOf(
                        InlineKeyBoard(LEARN_WORDS_CLICKED, "Изучать слова"),
                        InlineKeyBoard(STATISTIC_CLICKED, "Статистика")
                    )
                )
            )
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(requestBody)))
            .build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun sendQuestion(chatId: Long, question: Question) {
        val requestBody = SendMessageRequest(
            chatId = chatId,
            text = question.correctAnswer.questionWord,
            replyMarkup = ReplyMarkup(
                listOf(question.variants.mapIndexed { index, word ->
                    InlineKeyBoard("$CALLBACK_DATA_ANSWER_PREFIX$index", word.translate)
                })
            )
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$BASE_URL$botToken/sendMessage"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.encodeToString(requestBody)))
            .build()
        client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    fun checkNextQuestionAndSend(chatId: Long) {
        trainer.getNextQuestion()?.let {
            sendQuestion(chatId, it)
        } ?: sendMessage(chatId, "Все слова в словаре выучены")
    }

    while (true) {
        Thread.sleep(TIME_SLEEP)

        val updatesUrl = "$BASE_URL$botToken/getUpdates?offset=${lastUpdateId + 1}"
        val updatesRequest = HttpRequest.newBuilder().uri(URI.create(updatesUrl)).build()
        val updatesResponse = client.send(updatesRequest, HttpResponse.BodyHandlers.ofString())
        val response: Response = json.decodeFromString(updatesResponse.body())

        for (update in response.result) {
            lastUpdateId = update.update_id
            val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: continue
            val text = update.message?.text
            val callbackData = update.callbackQuery?.data

            when {
                text == CMD_HELLO -> sendMessage(chatId, "hello")
                text == CMD_MENU -> sendMenu(chatId)
                callbackData == LEARN_WORDS_CLICKED -> checkNextQuestionAndSend(chatId)
                callbackData == STATISTIC_CLICKED -> {
                    val stats = trainer.getStatistics()
                    sendMessage(chatId,
                        "Результат изучения: ${stats.learnedCount}/${stats.totalCount} (${stats.percentCount}%)")
                }
                callbackData?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true -> {
                    val index = callbackData.removePrefix(CALLBACK_DATA_ANSWER_PREFIX).toIntOrNull()
                    val isCorrect = trainer.checkAnswer(index)
                    sendMessage(chatId, if (isCorrect) "Правильно!" else "Неправильно")
                    checkNextQuestionAndSend(chatId)
                }
            }
        }
    }
}