import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

const val TIME_SLEEP: Long = 2000
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTIC_CLICKED = "statistic_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"

@Serializable
data class Update(
    @SerialName("update_id") val updateId: Long,
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
    val botToken = args[0]
    var lastUpdateId = 0L

    val json = Json {
        ignoreUnknownKeys = true
    }

    val trainer = LearnWordTrainer()

    while (true) {
        Thread.sleep(TIME_SLEEP)
        val responseString: String = getUpdates(botToken, lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        val updates = response.result
        val firstUpdate = updates.firstOrNull() ?: continue
        val updateId = firstUpdate.updateId
        lastUpdateId = updateId + 1

        val message = firstUpdate.message?.text
        val chatId = firstUpdate.message?.chat?.id ?: firstUpdate.callbackQuery?.message?.chat?.id
        val data = firstUpdate.callbackQuery?.data

        if (message?.lowercase() == "/start" && chatId != null) {
            sendMenu(json, botToken, chatId)
        }

        if (data == LEARN_WORDS_CLICKED && chatId != null) {
            checkNextQuestionAndSend(json, trainer, botToken, chatId)
        }

        if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true && chatId != null) {
            val answerId = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
            if (trainer.checkAnswer(answerId)) {
                sendMessage(json, botToken, chatId, "Правильно")
            } else {
                sendMessage(
                    json,
                    botToken,
                    chatId,
                    "Не правильно: ${ trainer.question?.correctAnswer?.questionWord } - ${ trainer.question?.correctAnswer?.translate }"
                )
            }
            checkNextQuestionAndSend(json, trainer, botToken, chatId)
        }
    }

    if (data?.lowercase() == STATISTIC_CLICKED && chatId != null) {
        val statistics: Statistics = trainer.getStatistics()
        sendMessage(
            json, botToken, chatId,
            "Выучено ${statistics.learned} из ${statistics.total} слов | ${statistics.percent}%"
        )
    }
}

private fun checkNextQuestionAndSend(json: Json, trainer: LearnWordTrainer, botToken: String, chatId: Long) {
    val  question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(json, botToken, chatId, "Вы выучили все слова в базе")
    } else {
        sendQuestion(json, botToken, chatId, question)
    }
}

fun getUpdates(botToken: String, updateid: Long,): String {
    val urlGetUpdate = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateid"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdate)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return  response.body()
}

fun sendMessage(json: Json, botToken: String, chatId: Long, message: String): String {
    val  sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = message,
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return  response.body()
}

fun sendMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Основное меню",
        replyMarkup = ReplyMarkup(
            listOf(listOf(
                InlineKeyBoard("Изучать слова", LEARN_WORDS_CLICKED),
                InlineKeyBoard("Статистика", STATISTIC_CLICKED)
            ))
        )
    )
    val requestBodyString = json.encodeToString(requestBody)

    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage()))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString)).build()

    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return  response.body()
}

fun sendQuestion(json: Json, botToken: String, chatId: Long, question: Question): String {
    val  urlGetUpdate = "https://api.telegram.org/bot$botToken/sendMessage"
    val keyboardLayout = question.variants.mapIndexed { index: Int, word: Word -> "{ \"text\": \"${word.translate}\", " +
            "\"callback_data\": \"$CALLBACK_DATA_ANSWER_PREFIX$index" }.joinToString(",")
    println(keyboardLayout)

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = question.correctAnswer.questionWord,
        replyMarkup = ReplyMarkup(
            listOf(question.variants.mapIndexed { index, word ->
                InlineKeyBoard(word.translate, "$CALLBACK_DATA_ANSWER_PREFIX$index"
                )
            })
        )
    )
    val reqestBodyString = json.encodeToString(requestBody)

    val client: HttpClient = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder(URI.create(urlGetUpdate))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(reqestBodyString)).build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

