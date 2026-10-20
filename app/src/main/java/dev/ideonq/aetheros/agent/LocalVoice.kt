package dev.ideonq.aetheros.agent

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * On-device recognition only. No silent fallback to cloud transcription.
 * Invoked by an explicit microphone button, not a background hotword service.
 */
class LocalVoice(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null

    fun available(): Boolean = Build.VERSION.SDK_INT >= 31 &&
        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun listen(onText: (String) -> Unit, onError: (String) -> Unit) {
        cancel()
        if (!available()) {
            onError("이 기기는 온디바이스 음성 인식을 지원하지 않아요. 텍스트로 입력해 주세요.")
            return
        }
        val engine = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        recognizer = engine
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) {
                onError("음성 인식 오류 ($error). 다시 시도하거나 입력해 주세요.")
                cancel()
            }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (text.isNullOrBlank()) onError("음성을 인식하지 못했어요.")
                else onText(text)
                cancel()
            }
        })
        engine.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        })
    }

    fun cancel() {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }
}
