package dev.ideonq.aetheros.agent

import android.content.Context
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.HttpsURLConnection
import java.security.KeyStore

enum class AgentMode(val label: String) {
    LOCAL("Local (온디바이스)"), HYBRID("Hybrid (필요 시 GPT)"), GPT("GPT (외부 서버)")
}

data class AgentPlan(val reply: String, val actions: List<AgentCommand>)

class GatewaySettings(context: Context) {
    private val prefs = context.getSharedPreferences("aether_gateway", Context.MODE_PRIVATE)
    private val keyAlias = "aether_device_token_v1"

    var endpoint: String
        get() = prefs.getString("endpoint", "") ?: ""
        set(v) { prefs.edit().putString("endpoint", v.trim()).apply() }

    var mode: AgentMode
        get() = runCatching { AgentMode.valueOf(prefs.getString("mode", "LOCAL")!!) }
            .getOrDefault(AgentMode.LOCAL)
        set(v) { prefs.edit().putString("mode", v.name).apply() }

    fun hasToken(): Boolean = prefs.contains("token_cipher") && prefs.contains("token_iv")

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun saveToken(token: String) {
        require(token.length in 32..512)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        prefs.edit()
            .putString("token_iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("token_cipher", Base64.encodeToString(cipher.doFinal(token.toByteArray()), Base64.NO_WRAP))
            .apply()
    }

    fun token(): String {
        val iv = prefs.getString("token_iv", null) ?: error("device_token_not_configured")
        val data = prefs.getString("token_cipher", null) ?: error("device_token_not_configured")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
        return String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), Charsets.UTF_8)
    }

    fun clearToken() {
        prefs.edit().remove("token_iv").remove("token_cipher").apply()
    }

    companion object {
        fun validEndpoint(value: String): Boolean {
            return try {
                val uri = Uri.parse(value.trim())
                uri.scheme == "https" && !uri.host.isNullOrBlank() &&
                    uri.userInfo == null && uri.encodedQuery == null &&
                    uri.fragment == null && uri.path == "/v1/plan"
            } catch (_: Exception) { false }
        }
    }
}

/** No OpenAI API key in this APK; only a per-device bearer credential. */
class GptGatewayClient(private val settings: GatewaySettings) {
    suspend fun plan(text: String): AgentPlan = withContext(Dispatchers.IO) {
        if (!GatewaySettings.validEndpoint(settings.endpoint)) throw IllegalArgumentException("HTTPS gateway 주소를 확인해 주세요.")
        if (text.isBlank() || text.length > 1000) throw IllegalArgumentException("명령이 너무 길거나 비어 있어요.")
        val url = URL(settings.endpoint)
        val connection = (url.openConnection() as HttpsURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 9_000
            readTimeout = 28_000
            instanceFollowRedirects = false
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${settings.token()}")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val data = JSONObject().put("prompt", text).toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(data) }
            if (connection.responseCode != 200) {
                throw IllegalStateException("서버 연결 실패 (HTTP ${connection.responseCode})")
            }
            if ((connection.contentLengthLong) > 32_000) throw IllegalStateException("응답이 너무 커요.")
            val response = connection.inputStream.bufferedReader().use { it.readText().take(32_001) }
            if (response.length > 32_000) throw IllegalStateException("응답이 너무 커요.")
            decodePlan(response)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun decodePlan(response: String): AgentPlan {
            val json = JSONObject(response)
            val reply = json.optString("reply").take(700)
            val array = json.optJSONArray("actions") ?: return AgentPlan(reply, emptyList())
            if (array.length() > 4) throw IllegalStateException("지원되지 않는 작업 계획")
            val parsed = mutableListOf<AgentCommand>()
            for (i in 0 until array.length()) {
                val a = array.optJSONObject(i) ?: throw IllegalStateException("잘못된 작업 계획")
                val command: AgentCommand = when (a.optString("type")) {
                    "message" -> {
                        val name = a.optString("recipient").trim()
                        val body = a.optString("body").trim()
                        val channel = when (a.optString("channel")) {
                            "kakao" -> Channel.KAKAO
                            "sms" -> Channel.SMS
                            else -> throw IllegalStateException("지원되지 않는 메시지 채널")
                        }
                        if (!AgentCommandParser.validRecipient(name) || body.length !in 1..400)
                            throw IllegalStateException("유효하지 않은 메시지")
                        AgentCommand.Message(channel, name, body)
                    }
                    "open_app" -> a.optString("app").trim().takeIf { it.length in 1..40 }
                        ?.let(AgentCommand::OpenApp) ?: throw IllegalStateException("유효하지 않은 앱")
                    "open_settings" -> {
                        val name = a.optString("setting").trim()
                        if (name !in setOf("설정", "배터리 설정", "화면 설정", "와이파이 설정"))
                            throw IllegalStateException("지원되지 않는 설정")
                        AgentCommand.OpenSettings(name)
                    }
                    else -> throw IllegalStateException("지원되지 않는 작업")
                }
                parsed.add(command)
            }
            return AgentPlan(reply, parsed)
        }
    }
}
