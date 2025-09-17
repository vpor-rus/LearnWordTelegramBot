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
    @SerialName("reply_markup") val replyMarkup: ReplyMarkup? = null
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
    val json = Json { ignoreUnknownKeys = true }
    val trainers = HashMap<Long, LearnWordTrainer>()
    val trainer = LearnWordTrainer()

    while (true) {
        Thread.sleep(TIME_SLEEP)
        val responseString: String = getUpdates(botToken, lastUpdateId)
        println(responseString)

        val response: Response = json.decodeFromString(responseString)
        if (response.result.isEmpty()) continue
        val sortedUpdates = response.result.sortedBy { it.updateId }
        sortedUpdates.forEach { handleUpdate(it, json, botToken, trainers) }
        lastUpdateId = sortedUpdates.last().updateId + 1


    }
}

fun handleUpdate(update: Update, json: Json, botToken: String, trainers: HashMap<Long, LearnWordTrainer>) {

    val message = update.message?.text
    val chatId = update.message?.chat?.id ?: update.callbackQuery?.message?.chat?.id ?: return
    val data = update.callbackQuery?.data

    val translate = trainers.getOrPut (chatId) {
        LearnWordTrainer("$chatId.txt")
    }

    if (message?.lowercase() == "/start") {
        sendMenu(json, botToken, chatId)
    }

    if (message?.lowercase() == "menu") {
        sendMenu(json, botToken, chatId)
    }

    if (data == LEARN_WORDS_CLICKED) {
        checkNextQuestionAndSend(json, trainer, botToken, chatId)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX)) {
        val answerId = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(answerId)) {
            sendMessage(json, botToken, chatId, "Правильно")
        } else {
            sendMessage(
                json,
                botToken,
                chatId,
                "Не правильно: ${trainer.question?.correctAnswer?.questionWord} - ${trainer.question?.correctAnswer?.translate}"
            )
        }
        checkNextQuestionAndSend(json, trainer, botToken, chatId)
    }

    if (data == STATISTIC_CLICKED) {
        val statistics: Statistics = trainer.getStatistics()
        sendMessage(
            json, botToken, chatId,
            "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percentCount}%"
        )
    }
}

fun checkNextQuestionAndSend(json: Json, trainer: LearnWordTrainer, botToken: String, chatId: Long) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(json, botToken, chatId, "Вы выучили все слова в базе")
    } else {
        sendQuestion(json, botToken, chatId, question)
    }
}

fun getUpdates(botToken: String, updateid: Long): String {
    val urlGetUpdate = "https://api.telegram.org/bot$botToken/getUpdates?offset=$updateid"
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(urlGetUpdate)).build()
    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendMessage(json: Json, botToken: String, chatId: Long, message: String): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
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
    return response.body()
}

fun sendMenu(json: Json, botToken: String, chatId: Long): String {
    val sendMessage = "https://api.telegram.org/bot$botToken/sendMessage"
    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = "Основное меню",
        replyMarkup = ReplyMarkup(
            inlineKeyboard = listOf(
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
    val requestBodyString = json.encodeToString(requestBody)

    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder().uri(URI.create(sendMessage)) // Убрали лишние скобки
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString)).build()

    val response: HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

fun sendQuestion(json: Json, botToken: String, chatId: Long, question: Question): String {
    val urlGetUpdate = "https://api.telegram.org/bot$botToken/sendMessage"

    val keyboardLayout = question.variants.mapIndexed { index: Int, word: Word ->
        "{ \"text\": \"${word.translate}\", \"callback_data\": \"$CALLBACK_DATA_ANSWER_PREFIX$index\" }"
    }.joinToString(",")

    println(keyboardLayout)

    val requestBody = SendMessageRequest(
        chatId = chatId,
        text = question.correctAnswer.questionWord,
        replyMarkup = ReplyMarkup(
            inlineKeyboard = listOf(question.variants.mapIndexed { index, word ->
                InlineKeyBoard(
                    callbackData = "$CALLBACK_DATA_ANSWER_PREFIX$index",
                    text = word.translate
                )
            })
        )
    )
    val requestBodyString = json.encodeToString(requestBody)
    val client: HttpClient = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder().uri(URI.create(urlGetUpdate))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString)).build()

    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()
}

