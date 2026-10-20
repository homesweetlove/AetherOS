package dev.ideonq.aetheros.agent

import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.common.FeatureStatus
import org.json.JSONObject

/**
 * Gemini Nano runs locally through Android AICore, ONLY on supported devices.
 * Unavailable devices use a deterministic Korean rule parser. The model can
 * only propose a validated action; it is never allowed to execute anything.
 */
class NanoInterpreter {
    suspend fun interpret(text: String): Pair<AgentCommand, String> {
        val exact = AgentCommandParser.parse(text)
        if (exact != AgentCommand.Unrecognized) return exact to "온디바이스 규칙 분석"
        if (text.length > 600 || text.isBlank()) return AgentCommand.Unrecognized to "지원되지 않는 요청"

        return try {
            val client = Generation.getClient()
            try {
                if (client.checkStatus() != FeatureStatus.AVAILABLE) {
                    AgentCommand.Unrecognized to "Gemini Nano 사용 불가 · 지원 기종 확인 필요"
                } else {
                    val prompt = """Convert the Korean user's request into exactly one JSON object. Supported actions:
{"type":"message","channel":"kakao|sms","recipient":"name","body":"text"}
{"type":"open_app","app":"name"}
{"type":"open_settings","setting":"name"}
{"type":"none"}
No other actions. Do not invent recipients or message content. If ambiguous, output {"type":"none"}.
User request: ${text.take(600)}"""
                    val response = client.generateContent(prompt)
                    val output = response.candidates.firstOrNull()?.text.orEmpty().trim()
                    parseModelOutput(output) to "Gemini Nano · 기기 내부 처리"
                }
            } finally {
                client.close()
            }
        } catch (_: Exception) {
            AgentCommand.Unrecognized to "온디바이스 모델 사용 불가 · 직접 명령으로 다시 시도"
        }
    }

    internal fun parseModelOutput(text: String): AgentCommand {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start || end - start > 1000) return AgentCommand.Unrecognized
        val obj = runCatching { JSONObject(text.substring(start, end + 1)) }.getOrNull()
            ?: return AgentCommand.Unrecognized
        return when (obj.optString("type")) {
            "message" -> {
                val recipient = obj.optString("recipient").trim()
                val body = obj.optString("body").trim()
                val channel = when (obj.optString("channel")) {
                    "kakao" -> Channel.KAKAO
                    "sms" -> Channel.SMS
                    else -> return AgentCommand.Unrecognized
                }
                if (AgentCommandParser.validRecipient(recipient) && body.length in 1..400) {
                    AgentCommand.Message(channel, recipient, body)
                } else AgentCommand.Unrecognized
            }
            "open_app" -> obj.optString("app").trim().takeIf { it.length in 1..40 }
                ?.let(AgentCommand::OpenApp) ?: AgentCommand.Unrecognized
            "open_settings" -> obj.optString("setting").trim().takeIf { it.length in 1..40 }
                ?.let(AgentCommand::OpenSettings) ?: AgentCommand.Unrecognized
            else -> AgentCommand.Unrecognized
        }
    }
}
