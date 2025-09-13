import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    val text: String,
)

@Serializable
data class CallbackQuery(
    @SerialName("data")
    val data: String,
)

fun main() {

    val json = Json {
        ignoreUnknownKeys = true
    }

    val responseString = """
        {
           "ok":true,
           "result":[
              {
                 "update_id":708547862,
                 "message":{
                    "message_id":292,
                    "from":{
                       "id":1534297299,
                       "is_bot":false,
                       "first_name":"Kot",
                       "username":"sobakakott",
                       "language_code":"ru"
                    },
                    "chat":{
                       "id":1534297299,
                       "first_name":"Kot",
                       "username":"sobakakott",
                       "type":"private"
                    },
                    "date":1757760444,
                    "text":"start"
                 }
              }
           ]
        }
    """.trimIndent()

    /*val word = Json.encodeToString(
        Word(
            original = "Hello",
            translate = "Привет",
            correctAnswerCount = 0,
        )
    )
    println(word)

    val wordObject = Json.decodeFromString<Word>(
        """{"original": "Hello","translate": "привет"}"""
    )
    println(wordObject)*/

    val response = json.decodeFromString<Response>(responseString)
    println(response)
    
}