package dev.ideonq.aetheros.agent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.content.ContextCompat
import dev.ideonq.aetheros.AetherColors
import dev.ideonq.aetheros.GlassCard
import kotlinx.coroutines.launch

/**
 * User-directed, preview-before-action command interface. No wake-word
 * microphone, hidden UI interaction, or unconfirmed external messages.
 */
@Composable
fun AssistantScreen(resumeTick: Int) {
    val context = LocalContext.current
    val actions = remember(context) { PhoneActions(context) }
    val interpreter = remember { NanoInterpreter() }
    val gatewaySettings = remember(context) { GatewaySettings(context) }
    val gateway = remember(gatewaySettings) { GptGatewayClient(gatewaySettings) }
    val voice = remember(context) { LocalVoice(context) }
    DisposableEffect(voice) { onDispose { voice.cancel() } }
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<AgentCommand?>(null) }
    var plan by remember { mutableStateOf<AgentPlan?>(null) }
    var agentMode by remember { mutableStateOf(gatewaySettings.mode) }
    var gatewayAddress by remember { mutableStateOf(gatewaySettings.endpoint) }
    var deviceTokenText by remember { mutableStateOf("") }
    var hasDeviceToken by remember { mutableStateOf(gatewaySettings.hasToken()) }
    var engine by remember { mutableStateOf("기본 명령은 오프라인으로 분석합니다.") }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }
    var allowKakaoScript by remember { mutableStateOf(false) }
    val accessibilityEnabled = remember(resumeTick) { KakaoAccessibility.enabled(context) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted ->
        if (!granted) notice = "마이크 권한이 없어요. 텍스트 명령은 계속 사용할 수 있어요."
        else {
            listening = true
            voice.listen(
                onText = { prompt = it; listening = false },
                onError = { notice = it; listening = false }
            )
        }
    }
    val contactPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) notice = "연락처 권한이 없으면 이름을 번호로 변환할 수 없어요."
        else notice = "연락처 권한이 허용됐어요. 메시지 준비 버튼을 다시 눌러 주세요."
    }

    fun prepareSms(cmd: AgentCommand.Message) {
        when (val match = actions.resolveContact(cmd.recipient)) {
            is ContactMatch.Found -> {
                notice = if (actions.smsDraft(match.number, cmd.body))
                    "문자 앱에 내용을 입력했어요. 받는 사람과 내용을 확인해 전송하세요."
                else "문자 앱을 열 수 없어요."
                pending = null
            }
            ContactMatch.NeedsPermission -> contactPermission.launch(Manifest.permission.READ_CONTACTS)
            ContactMatch.Missing -> notice = "연락처에서 '${cmd.recipient}'을 찾지 못했어요. 전화번호를 직접 입력해 주세요."
            ContactMatch.Ambiguous -> notice = "연락처 이름이 여러 개예요. 전화번호로 명령을 다시 입력해 주세요."
        }
    }

    fun runCommand(cmd: AgentCommand, assisted: Boolean = false) {
        when (cmd) {
            is AgentCommand.Message -> {
                if (cmd.channel == Channel.SMS) {
                    prepareSms(cmd)
                } else if (assisted) {
                    if (!accessibilityEnabled || !allowKakaoScript) {
                        notice = "먼저 접근성 권한을 활성화하고 자동 입력 동의에 체크해 주세요."
                    } else {
                        KakaoDraftSession.begin(cmd.recipient, cmd.body)
                        val intent = context.packageManager.getLaunchIntentForPackage("com.kakao.talk")
                        if (intent == null) {
                            KakaoDraftSession.cancel()
                            notice = "카카오톡을 설치하거나 활성화해야 해요."
                        } else {
                            context.startActivity(intent)
                            notice = "카카오톡을 열었어요. 자동 입력이 안 되면 직접 채팅방을 선택해 주세요."
                            pending = null
                        }
                    }
                } else {
                    notice = if (actions.kakaoShare(cmd.body))
                        "카카오톡에서 '${cmd.recipient}'을 직접 선택한 뒤 전송하세요."
                    else "카카오톡 공유 기능을 열 수 없어요."
                    pending = null
                }
            }
            is AgentCommand.OpenApp -> {
                notice = if (actions.openApp(cmd.app)) "앱을 열었어요."
                    else "정확히 일치하는 앱을 찾지 못했어요. 앱 이름을 확인해 주세요."
                pending = null
            }
            is AgentCommand.OpenSettings -> {
                actions.openSetting(cmd.setting)
                pending = null
            }
            AgentCommand.Unrecognized -> notice = "이 명령은 아직 지원하지 않아요."
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(Color(0xFF596AEF), Color(0xFF8C6AE5))),
                RoundedCornerShape(30.dp)
            ).padding(21.dp)
        ) {
            Column {
                Text("✦ AETHER AI", fontSize = 11.sp,
                    letterSpacing = 2.4.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(9.dp))
                Text("Your phone,\njust one request away.",
                    fontSize = 25.sp, fontWeight = FontWeight.Light, lineHeight = 31.sp,
                    color = Color.White)
                Spacer(Modifier.height(12.dp))
                Text("카카오톡 · 문자 · 앱 실행 · 시스템 설정",
                    fontSize = 12.sp, color = Color.White.copy(alpha = .8f))
            }
        }
        Spacer(Modifier.height(14.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("명령 입력", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(600); pending = null; plan = null },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                placeholder = { Text("엄마한테 카톡으로 오늘 늦는다고 보내줘") },
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("AI 엔진", fontSize = 12.sp, color = AetherColors.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                AgentMode.entries.forEach { option ->
                    FilterChip(
                        selected = agentMode == option,
                        onClick = { agentMode = option; gatewaySettings.mode = option },
                        label = { Text(option.name, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        busy = true
                        pending = null
                        plan = null
                        notice = ""
                        scope.launch {
                            try {
                                val rule = AgentCommandParser.parse(prompt)
                                val localFirst = agentMode == AgentMode.LOCAL ||
                                    (agentMode == AgentMode.HYBRID && rule != AgentCommand.Unrecognized)
                                if (localFirst) {
                                    val result = interpreter.interpret(prompt)
                                    pending = result.first
                                    engine = result.second
                                } else if (GatewaySettings.validEndpoint(gatewaySettings.endpoint) && gatewaySettings.hasToken()) {
                                    val result = gateway.plan(prompt)
                                    plan = result
                                    engine = "GPT · 본인 서버를 통해 명령 분석"
                                    if (result.actions.isEmpty() && result.reply.isBlank()) {
                                        notice = "AI의 응답이 비어 있어요."
                                    }
                                } else if (agentMode == AgentMode.HYBRID) {
                                    val result = interpreter.interpret(prompt)
                                    pending = result.first
                                    engine = result.second + " · 서버 미설정"
                                } else {
                                    notice = "먼저 아래에서 HTTPS 서버 주소와 디바이스 토큰을 설정해 주세요."
                                }
                                if (pending == AgentCommand.Unrecognized && notice.isBlank()) {
                                    notice = "지원되는 명령으로 바꿔서 다시 말해 주세요."
                                }
                            } catch (e: Exception) {
                                if (agentMode == AgentMode.HYBRID) {
                                    val result = interpreter.interpret(prompt)
                                    pending = result.first
                                    engine = result.second + " · GPT 연결 실패"
                                } else {
                                    notice = "GPT 연결 오류: " + (e.message ?: "네트워크를 확인해 주세요.").take(130)
                                }
                            } finally { busy = false }
                        }
                    },
                    enabled = prompt.isNotBlank() && !busy
                ) { Text(if (busy) "분석 중…" else "명령 분석") }
                FilledTonalButton(
                    onClick = {
                        if (!voice.available()) {
                            notice = "온디바이스 음성 인식을 지원하지 않는 기기예요."
                        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED) {
                            micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            listening = true
                            voice.listen(
                                onText = { prompt = it; listening = false },
                                onError = { notice = it; listening = false }
                            )
                        }
                    }
                ) { Text(if (listening) "듣는 중…" else "🎤 음성") }
            }
            Spacer(Modifier.height(8.dp))
            Text(engine, fontSize = 11.sp, color = AetherColors.muted)
        }
        if (plan != null) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("GPT의 응답", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(plan?.reply.orEmpty(), color = AetherColors.ink)
                if (!plan?.actions.isNullOrEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("제안된 작업 · 각 항목은 개별 승인 필요",
                        fontSize = 12.sp, color = AetherColors.muted)
                    plan?.actions?.forEachIndexed { index, command ->
                        OutlinedButton(
                            onClick = { pending = command },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${index + 1}. " + when (command) {
                                is AgentCommand.Message -> "${command.recipient} · ${if (command.channel == Channel.KAKAO) "카톡" else "문자"} 초안"
                                is AgentCommand.OpenApp -> "${command.app} 실행"
                                is AgentCommand.OpenSettings -> "${command.setting} 열기"
                                else -> "지원하지 않음"
                            })
                        }
                    }
                }
            }
        }
        if (pending != null && pending != AgentCommand.Unrecognized) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text("실행 전 확인", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                when (val action = pending) {
                    is AgentCommand.Message -> {
                        Text("앱: ${if (action.channel == Channel.KAKAO) "카카오톡" else "문자"}")
                        Text("받는 사람: ${action.recipient}")
                        Spacer(Modifier.height(9.dp))
                        Text("내용", fontSize = 12.sp, color = AetherColors.muted)
                        Text(action.body)
                    }
                    is AgentCommand.OpenApp -> Text("앱 열기: ${action.app}")
                    is AgentCommand.OpenSettings -> Text("설정 열기: ${action.setting}")
                    else -> Unit
                }
                Spacer(Modifier.height(15.dp))
                Button(onClick = { pending?.let { runCommand(it) } },
                    modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if ((pending as? AgentCommand.Message)?.channel == Channel.KAKAO)
                            "카카오톡 공유에서 수신자 선택"
                        else if ((pending as? AgentCommand.Message)?.channel == Channel.SMS)
                            "문자 앱에 초안 준비"
                        else "확인하고 실행"
                    )
                }
                if ((pending as? AgentCommand.Message)?.channel == Channel.KAKAO) {
                    Spacer(Modifier.height(7.dp))
                    OutlinedButton(
                        onClick = { pending?.let { runCommand(it, assisted = true) } },
                        enabled = accessibilityEnabled && allowKakaoScript,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("접근성으로 채팅 초안 자동 입력 (실험적)") }
                }
                TextButton(onClick = { pending = null }, modifier = Modifier.fillMaxWidth()) {
                    Text("취소")
                }
            }
        }
        if (notice.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Text(notice, fontSize = 13.sp, color = AetherColors.ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("GPT 서버 연결 (선택)", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("GPT 요청은 설정한 HTTPS 서버를 통해 OpenAI로 전송됩니다. 휴대폰 연락처·화면은 자동 업로드하지 않아요.",
                fontSize = 12.sp, color = AetherColors.muted)
            Spacer(Modifier.height(9.dp))
            OutlinedTextField(
                value = gatewayAddress,
                onValueChange = { gatewayAddress = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS Gateway URL") },
                placeholder = { Text("https://ai.example.com/v1/plan") },
                singleLine = true
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = deviceTokenText,
                onValueChange = { deviceTokenText = it.take(512) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (hasDeviceToken) "새 디바이스 토큰 (현재 저장됨)" else "디바이스 토큰") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Button(onClick = {
                    if (!GatewaySettings.validEndpoint(gatewayAddress)) {
                        notice = "HTTPS 주소는 /v1/plan 경로로 끝나야 해요."
                    } else {
                        try {
                            if (deviceTokenText.isNotEmpty()) {
                                gatewaySettings.saveToken(deviceTokenText)
                                deviceTokenText = ""
                            }
                            gatewaySettings.endpoint = gatewayAddress
                            hasDeviceToken = gatewaySettings.hasToken()
                            notice = "서버 연결 설정을 저장했어요. GPT 모드로 전환해 사용하세요."
                        } catch (_: Exception) { notice = "디바이스 토큰은 32자 이상이어야 해요." }
                    }
                }) { Text("저장") }
                OutlinedButton(onClick = {
                    gatewaySettings.clearToken()
                    hasDeviceToken = false
                    deviceTokenText = ""
                    notice = "저장된 디바이스 토큰을 삭제했어요."
                }) { Text("토큰 삭제") }
            }
            Text("OpenAI API 키는 절대 이곳에 입력하지 마세요. Oracle 서버에만 보관하세요.",
                fontSize = 11.sp, color = AetherColors.muted)
        }
        Spacer(Modifier.height(12.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text("카카오톡 화면 조작 (선택)", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text("접근성 서비스를 켜면 카카오톡 화면에서 일치하는 이름을 찾아 초안 입력을 시도합니다. 화면이 바뀌거나 이름이 중복되면 중단합니다. 전송 버튼은 누르지 않습니다.",
                fontSize = 12.sp, lineHeight = 18.sp, color = AetherColors.muted)
            Spacer(Modifier.height(7.dp))
            Text("활성 상태: ${if (accessibilityEnabled) "연결됨" else "미연결"}",
                fontSize = 12.sp, color = AetherColors.blue)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allowKakaoScript, onCheckedChange = { allowKakaoScript = it })
                Text("화면의 상대·입력란을 읽고 조작하는 데 동의", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { KakaoAccessibility.settings(context) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("접근성 권한 설정 열기") }
        }
        Spacer(Modifier.height(14.dp))
        Text("Local 모드에서는 오프라인 명령 분석을 우선하고, Hybrid는 복잡한 요청에 GPT를 사용해요. GPT 모드에서는 명령 내용을 서버와 OpenAI로 전송하지만, 실행은 휴대폰에서 개별 확인 후 처리합니다.",
            color = AetherColors.muted, fontSize = 12.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(22.dp))
    }
}
