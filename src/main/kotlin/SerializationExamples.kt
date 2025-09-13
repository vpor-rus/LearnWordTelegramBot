import kotlinx.serialization.json.Json

fun main() {

    val word = Json.encodeToString(
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
    println(wordObject)
    
}