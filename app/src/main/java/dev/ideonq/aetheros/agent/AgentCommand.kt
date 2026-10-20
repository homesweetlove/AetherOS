package dev.ideonq.aetheros.agent

/**
 * A restricted action vocabulary. A language model is never permitted to output
 * executable shell commands, arbitrary URLs or accessibility selectors.
 */
sealed interface AgentCommand {
    data class Message(val channel: Channel, val recipient: String, val body: String) : AgentCommand
    data class OpenApp(val app: String) : AgentCommand
    data class OpenSettings(val setting: String) : AgentCommand
    data object Unrecognized : AgentCommand
}

enum class Channel { KAKAO, SMS }

object AgentCommandParser {
    private val messagePatterns = listOf(
        Regex("""^(.+?)(?:한테|에게)\s*(카카오톡으로|카톡으로|문자로|문자메시지로|SMS로|메시지로)\s*(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^(카카오톡으로|카톡으로|문자로|문자메시지로|SMS로|메시지로)\s*(.+?)(?:한테|에게)\s*(.+)$""", RegexOption.IGNORE_CASE),
        Regex("""^(.+?)(?:한테|에게)\s*(.+?)\s*(카카오톡|카톡|문자|SMS)(?:으로)?\s*(?:보내줘|보내|전송해줘|전송)$""", RegexOption.IGNORE_CASE)
    )
    private val messageTail = Regex("""\s*(?:라고|이라고)?\s*(?:보내줘|전송해줘|보내|전송|말해줘|써줘)[.!?\s]*$""")

    fun parse(raw: String): AgentCommand {
        val text = raw.trim().removeSuffix(".").trim()
        if (text.isBlank() || text.length > 600) return AgentCommand.Unrecognized

        for ((index, pattern) in messagePatterns.withIndex()) {
            val match = pattern.matchEntire(text) ?: continue
            val name = when (index) {
                0, 2 -> match.groupValues[1]
                else -> match.groupValues[2]
            }.trim()
            val mode = when (index) {
                0 -> match.groupValues[2]
                1 -> match.groupValues[1]
                else -> match.groupValues[3]
            }
            val content = when (index) {
                0, 1 -> match.groupValues[3]
                else -> match.groupValues[2]
            }.replace(messageTail, "").trim().trim('"', '“', '”', ' ')
            val channel = if (mode.contains("카톡") || mode.contains("카카오")) Channel.KAKAO else Channel.SMS
            if (validRecipient(name) && content.isNotBlank() && content.length <= 400) {
                return AgentCommand.Message(channel, name, content)
            }
        }
        val open = Regex("""^(.+?)(?:\s*앱)?\s*(?:열어줘|실행해줘|켜줘|열어|실행해)$""").matchEntire(text)
        if (open != null) {
            val name = open.groupValues[1].trim()
            if (name in listOf("설정", "배터리 설정", "화면 설정", "와이파이 설정")) {
                return AgentCommand.OpenSettings(name)
            }
            if (name.length in 1..40) return AgentCommand.OpenApp(name)
        }
        return AgentCommand.Unrecognized
    }

    fun validRecipient(name: String): Boolean =
        name.length in 1..40 && name.none { it == '\n' || it == '\r' }
}
