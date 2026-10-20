package dev.ideonq.aetheros.agent

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Opt-in, deterministic, single-purpose KakaoTalk draft helper.
 *
 * - Runs ONLY after a confirmed request from AetherOS.
 * - Reads ONLY com.kakao.talk windows when explicitly enabled.
 * - Uses exact recipient matching and cancels ambiguous candidates.
 * - NEVER presses Send. A user must verify the chat header and tap Send.
 * - Clears task state on timeout/error; no screen data is recorded.
 *
 * KakaoTalk UI changes can break this best-effort script.
 */
data class KakaoDraftTask(
    val recipient: String,
    val body: String,
    val created: Long = SystemClock.elapsedRealtime(),
    var phase: KakaoPhase = KakaoPhase.SEARCH
)

enum class KakaoPhase { SEARCH, SEARCH_TEXT, MATCH_RECIPIENT, FILL_MESSAGE }

object KakaoDraftSession {
    @Volatile var task: KakaoDraftTask? = null
        private set
    fun begin(recipient: String, body: String) {
        require(AgentCommandParser.validRecipient(recipient) && body.length in 1..400)
        task = KakaoDraftTask(recipient, body)
    }
    fun cancel() { task = null }
}

object KakaoAccessibility {
    fun enabled(context: Context): Boolean {
        val service = context.getSystemService(AccessibilityManager::class.java) ?: return false
        return service.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
                it.resolveInfo.serviceInfo.name.endsWith("KakaoAssistedService")
        }
    }
    fun settings(context: Context) {
        context.startActivity(android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

class KakaoAssistedService : AccessibilityService() {
    private var lastAttempt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val job = KakaoDraftSession.task ?: return
        if (event?.packageName?.toString() != "com.kakao.talk") return
        if (SystemClock.elapsedRealtime() - job.created > 45_000) {
            abort("시간이 지나 작업을 종료했어요. 직접 확인해 주세요.")
            return
        }
        val now = SystemClock.elapsedRealtime()
        if (now - lastAttempt < 450) return
        lastAttempt = now
        val root = rootInActiveWindow ?: return
        try {
            when (job.phase) {
                KakaoPhase.SEARCH -> {
                    val search = allNodes(root).firstOrNull {
                        val text = it.text?.toString().orEmpty()
                        val desc = it.contentDescription?.toString().orEmpty()
                        (desc == "검색" || desc == "Search" || text == "검색") &&
                            !it.className?.toString().orEmpty().contains("EditText")
                    }
                    if (search != null && clickParent(search)) job.phase = KakaoPhase.SEARCH_TEXT
                }
                KakaoPhase.SEARCH_TEXT -> {
                    val fields = allNodes(root).filter {
                        it.className?.toString()?.contains("EditText") == true && it.isEditable
                    }
                    if (fields.size == 1 && fill(fields.first(), job.recipient)) {
                        job.phase = KakaoPhase.MATCH_RECIPIENT
                    }
                }
                KakaoPhase.MATCH_RECIPIENT -> {
                    val matching = allNodes(root).filter {
                        it.text?.toString()?.trim() == job.recipient &&
                            it.className?.toString()?.contains("EditText") != true &&
                            clickableParent(it) != null
                    }
                    // No guessing when more than one contact/room shares the name.
                    if (matching.size > 1) {
                        abort("동일한 이름이 여러 개 있어 자동 선택을 중단했어요.")
                    } else if (matching.size == 1 && clickParent(matching.first())) {
                        job.phase = KakaoPhase.FILL_MESSAGE
                    }
                }
                KakaoPhase.FILL_MESSAGE -> {
                    val visible = allNodes(root)
                    val height = resources.displayMetrics.heightPixels
                    val verifiedTitle = visible.any { node ->
                        if (node.text?.toString()?.trim() != job.recipient ||
                            node.className?.toString()?.contains("EditText") == true) false
                        else {
                            val bounds = Rect()
                            node.getBoundsInScreen(bounds)
                            bounds.top >= 0 && bounds.centerY() < height * 0.33f
                        }
                    }
                    val editFields = visible.filter {
                        it.className?.toString()?.contains("EditText") == true && it.isEditable
                    }
                    // Header must match the intended recipient, composer near screen bottom.
                    val editors = editFields.filter {
                        val bounds = Rect()
                        it.getBoundsInScreen(bounds)
                        bounds.centerY() > height * 0.55f
                    }
                    if (verifiedTitle && editors.size == 1 && fill(editors.first(), job.body)) {
                        KakaoDraftSession.cancel()
                        Toast.makeText(this, "입력했어요. 상대방과 내용을 확인한 뒤 전송하세요.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (_: Exception) {
            abort("카카오톡 화면이 달라 자동 준비를 중단했어요.")
        } finally {
            root.recycle()
        }
    }

    private fun allNodes(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty() && nodes.size < 350) {
            val node = queue.removeFirst()
            nodes.add(node)
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let(queue::add)
            }
        }
        return nodes
    }

    private fun clickableParent(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        repeat(4) {
            val n = current ?: return null
            if (n.isClickable) return n
            current = n.parent
        }
        return null
    }
    private fun clickParent(node: AccessibilityNodeInfo): Boolean =
        clickableParent(node)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true

    private fun fill(node: AccessibilityNodeInfo, value: String): Boolean =
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        })

    private fun abort(reason: String) {
        KakaoDraftSession.cancel()
        Toast.makeText(this, reason, Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() { KakaoDraftSession.cancel() }
    override fun onDestroy() {
        KakaoDraftSession.cancel()
        super.onDestroy()
    }
}
