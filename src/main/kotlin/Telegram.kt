import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.sql.DriverManager

const val TIME_SLEEP: Long = 2000
const val LEARN_WORDS_CLICKED = "learn_words_clicked"
const val STATISTIC_CLICKED = "statistic_clicked"
const val RESET_CLICKED = "reset_clicked"
const val CALLBACK_DATA_ANSWER_PREFIX = "answer_"
private const val BOT_FILE_URL = "https://api.telegram.org/file/bot"

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
    @SerialName("text")
    val text: String? = null,
    @SerialName("chat")
    val chat: Chat,
    @SerialName("document")
    val document: Document? = null,
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

@Serializable
data class Document(
    @SerialName("file_name")
    val fileName: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
)

@Serializable
data class GetFileResponse(
    @SerialName("ok")
    val ok: Boolean,
    @SerialName(
        "result")
    val result: TelegramFile? = null,
)

@Serializable
data class GetFileRequest(
    @SerialName("file_id")
    val fileId: String
)

@Serializable
data class TelegramFile(
    @SerialName("file_id")
    val fileId: String,
    @SerialName("file_unique_id")
    val fileUniqueId: String,
    @SerialName("file_size")
    val fileSize: Long,
    @SerialName("file_path")
    val filePath: String,
)

fun main(args: Array<String>) {
    val botToken = args[0]
    var lastUpdateId = 0L
    val json = Json { ignoreUnknownKeys = true }
    val trainers = HashMap<Long, LearnWordTrainer>()


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

    val trainer = trainers.getOrPut(chatId) {
        val dictionary = DatabaseUserDictionary(dbPath = "user_$chatId.db")
        LearnWordTrainer(dictionary)
    }

    if (message?.lowercase() == "/start" || message?.lowercase() == "menu") {
        sendMenu(json, botToken, chatId)
    }
    if (data == LEARN_WORDS_CLICKED) {
        checkNextQuestionAndSend(json, trainer, botToken, chatId)
    }

    if (data?.startsWith(CALLBACK_DATA_ANSWER_PREFIX) == true) {
        val answerId = data.substringAfter(CALLBACK_DATA_ANSWER_PREFIX).toInt()
        if (trainer.checkAnswer(answerId)) {
            sendMessage(json, botToken, chatId, "Правильно")
        } else {
            sendMessage(
                json,
                botToken,
                chatId,
                "Неправильно: ${trainer.question?.correctAnswer?.questionWord} - ${trainer.question?.correctAnswer?.translate}"
            )
        }
        checkNextQuestionAndSend(json, trainer, botToken, chatId)
    }

    if (data == STATISTIC_CLICKED) {
        val statistics = trainer.getStatistics()
        sendMessage(
            json, botToken, chatId,
            "Выучено ${statistics.learnedCount} из ${statistics.totalCount} слов | ${statistics.percentCount}%"
        )
    }

    if (data == RESET_CLICKED) {
        trainer.resetProgress()
        sendMessage(
            json,
            botToken,
            chatId,
            "Прогресс сброшен")
    }

    if (update.message?.document != null) {
        val document = update.message.document
        val fileId = document.fileId

        try {
            // Получаем информацию о файле
            val fileResponse = json.decodeFromString<GetFileResponse>(getFile(fileId, json, botToken))

            if (fileResponse.ok && fileResponse.result != null) {
                val filePath = fileResponse.result.filePath
                val fileName = "dictionary_${chatId}.txt"

                downloadFile(filePath, fileName, botToken)
                try {
                    val file = File(fileName)
                    updateDictionary(file)
                    sendMessage(json, botToken, chatId, "Словарь успешно обновлен!")
                } catch (e: Exception) {
                    sendMessage(json, botToken, chatId, "Ошибка при обновлении словаря: ${e.message}")
                }
            } else {
                sendMessage(json, botToken, chatId, "Не удалось получить информацию о файле")
            }
        } catch (e: Exception) {
            sendMessage(json, botToken, chatId, "Произошла ошибка: ${e.message}")
        }
    }
}

fun checkNextQuestionAndSend(json: Json, trainer: LearnWordTrainer, botToken: String, chatId: Long) {
    val question = trainer.getNextQuestion()
    if (question == null) {
        sendMessage(
            json,
            botToken,
            chatId,
            "Вы выучили все слова в базе")
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
                    InlineKeyBoard( callbackData = LEARN_WORDS_CLICKED, text = "Изучать слова"),
                    InlineKeyBoard( callbackData = STATISTIC_CLICKED,   text = "Статистика"),
                ),
                listOf(
                    InlineKeyBoard(
                        callbackData = RESET_CLICKED,
                        text = "Сбросить прогресс"
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

fun getFile(fileId: String, json: Json, botToken: String): String {
    val urlGetFile = "https://api.telegram.org/bot$botToken/getFile"
    val requestBody = GetFileRequest(fileId = fileId)
    val requestBodyString = json.encodeToString(requestBody)
    val client: HttpClient = HttpClient.newBuilder().build()
    val request: HttpRequest = HttpRequest.newBuilder()
        .uri(URI.create(urlGetFile))
        .header("Content-type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
        .build()
    val response: HttpResponse<String> = client.send(
    request,
    HttpResponse.BodyHandlers.ofString()
    )
    return response.body()
}

fun downloadFile(filePath: String, fileName: String, botToken: String) {
    val urlGetFile = "$BOT_FILE_URL$botToken/$filePath"
    println(urlGetFile)
    val request = HttpRequest
        .newBuilder()
        .uri(URI.create(urlGetFile))
        .GET()
        .build()

    val response: HttpResponse<InputStream> = HttpClient
    .newHttpClient()
        .send(request, HttpResponse.BodyHandlers.ofInputStream())

    println("status code: " + response.statusCode())
    val body: InputStream = response.body()
    body.copyTo(File(fileName).outputStream(), 16 * 1024)
}

fun updateDictionary(wordsFile: File, dbPath: String = "data.db") {
    if (!wordsFile.exists()) {
        throw IllegalArgumentException("Файл не существует: ${wordsFile.absolutePath}")
    }

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

    DriverManager.getConnection("jdbc:sqlite:$dbPath").use { connection ->
        val createTableStatement = connection.createStatement()
        createTableStatement.executeUpdate(
            """
            CREATE TABLE IF NOT EXISTS 'words' (
                'id' INTEGER PRIMARY KEY,
                'text' VARCHAR,
                'translate' VARCHAR,
                'correctAnswersCount' INTEGER DEFAULT 0
            );
            """.trimIndent()
        )

        val clearStatement = connection.createStatement()
        clearStatement.executeUpdate("DELETE FROM words")

        val insertStatement = connection.prepareStatement(
            "INSERT INTO words (text, translate, correctAnswersCount) VALUES (?, ?, ?)"
        )

        words.forEach { (original, translate, count) ->
            insertStatement.setString(1, original)
            insertStatement.setString(2, translate)
            insertStatement.setInt(3, count)
            insertStatement.executeUpdate()
        }
    }
}