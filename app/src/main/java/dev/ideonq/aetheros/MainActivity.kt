package dev.ideonq.aetheros

import android.content.Intent
import dev.ideonq.aetheros.agent.AssistantScreen
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    companion object {
        const val OPEN_ASSISTANT_ACTION = "dev.ideonq.aetheros.action.OPEN_ASSISTANT"
    }
    private var resumeCounter by mutableIntStateOf(0)
    private var assistantOpenCounter by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == OPEN_ASSISTANT_ACTION) assistantOpenCounter++
        setContent {
            AetherTheme {
                AetherApp(resumeCounter, assistantOpenCounter)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == OPEN_ASSISTANT_ACTION) assistantOpenCounter++
    }

    override fun onResume() {
        super.onResume()
        resumeCounter++
    }
}

private data class NavigationItem(val glyph: String, val name: String)
private val navigation = listOf(
    NavigationItem("◈", "Home"),
    NavigationItem("✧", "AI"),
    NavigationItem("✦", "Style"),
    NavigationItem("◉", "Optimize"),
    NavigationItem("▦", "Apps"),
    NavigationItem("⚙", "More")
)

@Composable
private fun AetherApp(resumeTick: Int, openAssistantRequest: Int) {
    val context = LocalContext.current
    val deviceRepo = remember(context) { DeviceRepository(context) }
    val preferences = remember(context) { AetherPreferences(context) }
    var tab by remember { mutableIntStateOf(if (openAssistantRequest > 0) 1 else 0) }
    LaunchedEffect(openAssistantRequest) { if (openAssistantRequest > 0) tab = 1 }
    var refresh by remember { mutableIntStateOf(0) }
    var profile by remember { mutableStateOf(preferences.profile) }
    var useGlass by remember { mutableStateOf(preferences.glassEffects) }
    val snapshot = remember(resumeTick, refresh) { deviceRepo.snapshot() }

    CompositionLocalProvider(LocalGlassEnabled provides useGlass) {
    Column(
        Modifier
            .fillMaxSize()
            .background(aetherBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 23.dp, end = 18.dp, top = 17.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("AETHER", fontSize = 12.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold,
                    color = AetherColors.blue)
                Text("AetherOS", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    color = AetherColors.ink)
            }
            Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp) {
                TextButton(onClick = { refresh++ }) {
                    Text("↻", fontSize = 23.sp, color = AetherColors.blue)
                }
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> OverviewScreen(snapshot, profile, {
                    context.startActivity(Intent(context, StandByActivity::class.java))
                }, { tab = 2 }, { tab = 3 })
                1 -> AssistantScreen(resumeTick)
                2 -> AppearanceScreen(deviceRepo, useGlass, onGlassChanged = {
                    useGlass = it
                    preferences.glassEffects = it
                }, onStandBy = {
                    context.startActivity(Intent(context, StandByActivity::class.java))
                }, onLauncher = {
                    context.startActivity(Intent(context, LauncherActivity::class.java))
                })
                3 -> OptimizeScreen(snapshot, profile, onProfileChange = {
                    profile = it
                    preferences.profile = it
                }, deviceRepo)
                4 -> AppInsightsScreen(deviceRepo, resumeTick + refresh)
                else -> MoreScreen(snapshot, deviceRepo, onReset = {
                    preferences.reset()
                    profile = preferences.profile
                    useGlass = preferences.glassEffects
                    refresh++
                })
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = Color.White.copy(alpha = .93f),
            shadowElevation = 9.dp
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp, vertical = 8.dp)) {
                navigation.forEachIndexed { index, item ->
                    val active = index == tab
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                            .clickable { tab = index }.padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(item.glyph, fontSize = 23.sp,
                            color = if (active) AetherColors.blue else AetherColors.muted)
                        Text(item.name, fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) AetherColors.blue else AetherColors.muted)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun ScreenScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, bottom = 25.dp),
        content = content
    )
}

@Composable
private fun OverviewScreen(
    snapshot: DeviceSnapshot,
    profile: ExperienceProfile,
    onStandby: () -> Unit,
    onAppearance: () -> Unit,
    onOptimize: () -> Unit
) {
    ScreenScroll {
        Text("A more fluid Galaxy.", fontSize = 14.sp, color = AetherColors.muted)
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF5F80F7), Color(0xFF7D61D7))))
                .padding(24.dp)
        ) {
            Column {
                Text("YOUR GALAXY, REFINED", fontSize = 11.sp, letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = .82f))
                Spacer(Modifier.height(13.dp))
                Text("${snapshot.batteryPercent.takeIf { it >= 0 } ?: "—"}%",
                    fontSize = 59.sp, fontWeight = FontWeight.Light, color = Color.White)
                Text(if (snapshot.isCharging) "Charging now" else "Battery level",
                    fontSize = 14.sp, color = Color.White.copy(alpha = .9f))
                Spacer(Modifier.height(19.dp))
                LinearProgressIndicator(
                    progress = { (snapshot.batteryPercent.coerceIn(0, 100)) / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = .24f)
                )
                Spacer(Modifier.height(15.dp))
                Text(snapshot.model, color = Color.White.copy(alpha = .95f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("RAM AVAILABLE", "${snapshot.freeRamMb} MB",
                "of ${snapshot.totalRamMb} MB", Modifier.weight(1f))
            MetricCard("POWER SAVER", if (snapshot.batterySaver) "On" else "Off",
                "Android system state", Modifier.weight(1f))
        }
        SectionHeading("Your experience", "디자인과 성능을 함께 관리해요")
        GlassCard(Modifier.fillMaxWidth()) {
            Text("✦  ${profile.title} mode", fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, color = AetherColors.ink)
            Spacer(Modifier.height(5.dp))
            Text(profile.note, color = AetherColors.muted)
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("스타일 설정", onAppearance)
                SmallAction("최적화 보기", onOptimize)
            }
        }
        SectionHeading("StandBy", "책상 위에서 사용하는 전체 화면 시계")
        GlassCard(Modifier.fillMaxWidth()) {
            Text("10:08", fontSize = 44.sp, fontWeight = FontWeight.Light,
                color = AetherColors.blue)
            Text("실제 시간은 StandBy 실행 시 표시됩니다.", color = AetherColors.muted,
                fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            SmallAction("StandBy 열기  ↗", onStandby)
        }
        Spacer(Modifier.height(12.dp))
        Text("실제 배터리 절감·성능 향상 수치는 측정 전에는 표시하지 않아요.",
            fontSize = 12.sp, color = AetherColors.muted)
    }
}

@Composable
private fun MetricCard(label: String, value: String, description: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.sp, color = AetherColors.muted)
        Spacer(Modifier.height(9.dp))
        Text(value, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = AetherColors.ink)
        Text(description, fontSize = 10.sp, color = AetherColors.muted)
    }
}

@Composable
private fun SmallAction(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp) }
}

@Composable
private fun FeatureTile(
    glyph: String,
    title: String,
    description: String,
    tag: String,
    onClick: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(47.dp).clip(RoundedCornerShape(15.dp))
                    .background(AetherColors.paleBlue),
                contentAlignment = Alignment.Center
            ) { Text(glyph, fontSize = 24.sp, color = AetherColors.blue) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = AetherColors.ink)
                Text(description, fontSize = 12.sp, lineHeight = 17.sp,
                    color = AetherColors.muted)
                Spacer(Modifier.height(3.dp))
                Text(tag, fontSize = 10.sp, color = AetherColors.blue)
            }
            TextButton(onClick = onClick) { Text("열기 ›") }
        }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun AppearanceScreen(
    repo: DeviceRepository,
    useGlass: Boolean,
    onGlassChanged: (Boolean) -> Unit,
    onStandBy: () -> Unit,
    onLauncher: () -> Unit
) {
    ScreenScroll {
        SectionHeading("Appearance", "iOS에서 영감을 받은 경험 · Dynamic Island 제외")
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Glass interface", fontWeight = FontWeight.SemiBold)
                    Text("AetherOS 내부의 유리 질감 스타일 설정",
                        fontSize = 12.sp, color = AetherColors.muted)
                }
                Switch(checked = useGlass, onCheckedChange = onGlassChanged)
            }
        }
        SectionHeading("Display & Lock")
        FeatureTile("◐", "Always On Display",
            "삼성 화면 설정에서 AOD를 구성해요. 네이티브 AOD를 대신하지 않아요.",
            "삼성/Android 설정", onClick = { repo.openSettings(Settings.ACTION_DISPLAY_SETTINGS) })
        FeatureTile("❖", "Lock screen & motion",
            "Good Lock의 LockStar, ClockFace, Wonderland 등을 사용해요.",
            "Good Lock 수동 설정", onClick = repo::openGoodLock)
        FeatureTile("✺", "Aether Flow wallpaper",
            "저부하 움직임과 홈 화면 패럴랙스를 제공하는 AetherOS 라이브 배경화면.",
            "Android 라이브 배경화면 선택", onClick = repo::openAetherWallpaper)
        FeatureTile("▣", "Wallpaper",
            "갤럭시 배경화면 선택기에서 직접 적용해요.",
            "사용자 승인 필요", onClick = repo::openWallpaper)
        SectionHeading("Home & Interaction")
        FeatureTile("▦", "Aether Launcher",
            "간결한 앱 그리드를 미리 보고 기본 홈 앱을 선택할 수 있어요.",
            "실제 런처 프리뷰", onClick = onLauncher)
        FeatureTile("⌂", "Default home app",
            "갤럭시 기본 홈을 변경하거나 다시 복원할 수 있어요.",
            "시스템 설정", onClick = { repo.openSettings(Settings.ACTION_HOME_SETTINGS) })
        FeatureTile("◌", "Animation & gesture",
            "Home Up으로 동작을 조정하거나 개발자 옵션을 직접 확인해요.",
            "Good Lock 수동 설정", onClick = repo::openGoodLock)
        FeatureTile("♫", "Haptics & sound",
            "진동 세기, 시스템 사운드와 터치 피드백을 확인해요.",
            "시스템 설정", onClick = { repo.openSettings(Settings.ACTION_SOUND_SETTINGS) })
        FeatureTile("☰", "Quick panel",
            "QuickStar에서 갤럭시 빠른 설정 영역을 꾸며요.",
            "Good Lock 수동 설정", onClick = repo::openGoodLock)
        SectionHeading("Desk experience")
        FeatureTile("◷", "StandBy clock",
            "실행 중에만 화면을 켜 두는 가로형 시계예요.",
            "앱 내부 기능", onClick = onStandBy)
        Text("참고: 시스템 잠금화면, AOD 및 타사 앱의 모션은 일반 APK에서 자동으로 교체할 수 없어요.",
            color = AetherColors.muted, fontSize = 12.sp)
    }
}

@Composable
private fun OptimizeScreen(
    snapshot: DeviceSnapshot,
    profile: ExperienceProfile,
    onProfileChange: (ExperienceProfile) -> Unit,
    repo: DeviceRepository
) {
    ScreenScroll {
        SectionHeading("Optimize", "위험한 강제 종료 대신 안전한 설정을 추천해요")
        Text("EXPERIENCE PROFILE", fontSize = 11.sp, color = AetherColors.muted,
            letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        ExperienceProfile.entries.forEach { option ->
            GlassCard(Modifier.fillMaxWidth().clickable { onProfileChange(option) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = option == profile, onClick = { onProfileChange(option) })
                    Spacer(Modifier.width(4.dp))
                    Column {
                        Text(option.title, fontWeight = FontWeight.SemiBold)
                        Text(option.note, fontSize = 12.sp, color = AetherColors.muted)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Text("프로필 선택은 AetherOS 환경설정에 저장됩니다. Android의 절전·주사율 설정은 직접 적용해야 해요.",
            fontSize = 12.sp, color = AetherColors.muted)
        SectionHeading("Recommended adjustments")
        OptimizationAdvisor.suggestions(profile, snapshot.batterySaver).forEach { advice ->
            GlassCard(Modifier.fillMaxWidth()) {
                Text("✓  ${advice.title}", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(advice.detail, fontSize = 13.sp, color = AetherColors.muted)
            }
            Spacer(Modifier.height(10.dp))
        }
        FilledTonalButton(onClick = { repo.openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS) },
            modifier = Modifier.fillMaxWidth()) {
            Text("Android 배터리 설정 열기  ↗")
        }
        Text("사전 설치 앱은 자동으로 끄지 않아요. 시스템/금융/인증 기능 보호가 기본값입니다.",
            fontSize = 12.sp, color = AetherColors.muted)
    }
}

@Composable
private fun AppInsightsScreen(repo: DeviceRepository, refreshKey: Int) {
    val permitted = remember(refreshKey) { repo.hasUsageAccess() }
    val usage = remember(refreshKey, permitted) {
        if (permitted) repo.usageLast24Hours() else emptyList()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        SectionHeading("App insights", "앱 사용 시간 참고용 · 전력 소비량이 아닙니다")
        if (!permitted) {
            GlassCard(Modifier.fillMaxWidth()) {
                Text("Usage Access 필요", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Android 설정에서 AetherOS에 사용 정보 접근을 허용해야 앱 사용 시간 목록을 볼 수 있어요. 선택 사항이며 사용 기록은 외부로 전송하지 않습니다.",
                    color = AetherColors.muted, fontSize = 13.sp)
                Spacer(Modifier.height(13.dp))
                SmallAction("접근 권한 설정 열기", {
                    repo.openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                })
            }
        } else {
            Text("최근 하루의 Android 집계 구간 기준. 일부 앱 사용량은 실제 24시간과 차이가 있을 수 있어요.",
                color = AetherColors.muted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            if (usage.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("표시할 사용 기록이 없어요.", color = AetherColors.muted)
                }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(usage, key = { it.packageName }) { app ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold)
                                Text("${app.minutes}분 · 포그라운드 사용",
                                    fontSize = 12.sp, color = AetherColors.muted)
                            }
                            TextButton(onClick = { repo.openAppDetails(app.packageName) }) {
                                Text("설정 ›")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreScreen(
    snapshot: DeviceSnapshot,
    repo: DeviceRepository,
    onReset: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    ScreenScroll {
        SectionHeading("Device & safety", "변경 내역과 시스템 제약을 명확히 안내해요")
        GlassCard(Modifier.fillMaxWidth()) {
            Text(snapshot.model, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(snapshot.version, color = AetherColors.muted)
            Spacer(Modifier.height(8.dp))
            Text("사용 가능한 RAM: ${snapshot.freeRamMb} MB / ${snapshot.totalRamMb} MB",
                fontSize = 12.sp, color = AetherColors.muted)
        }
        SectionHeading("Permissions & shortcuts")
        FeatureTile("◈", "Default launcher", "기본 홈 앱 선택과 원래 런처 복원",
            "Android 설정", onClick = { repo.openSettings(Settings.ACTION_HOME_SETTINGS) })
        FeatureTile("◷", "Usage Access", "앱 사용 시간 읽기 권한을 해제할 수 있어요.",
            "Android 설정", onClick = { repo.openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS) })
        SectionHeading("Safety first")
        GlassCard(Modifier.fillMaxWidth()) {
            Text("AetherOS는 루팅, 백그라운드 서비스 강제 종료, 시스템 앱 삭제를 실행하지 않습니다.",
                color = AetherColors.ink, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("AOD 및 Good Lock 설정은 시스템 화면에서 사용자가 직접 변경합니다. 복원하려면 동일한 화면으로 돌아가야 합니다. AetherOS에는 시스템 수준 백업 권한이 없습니다.",
                color = AetherColors.muted, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showConfirm = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("AetherOS 앱 설정 초기화") }
        if (showConfirm) {
            AlertDialog(
                onDismissRequest = { showConfirm = false },
                title = { Text("앱 설정만 초기화할까요?") },
                text = { Text("선택한 최적화 프로필과 유리 효과 옵션만 초기화합니다. 삼성 시스템 설정은 변경하지 않습니다.") },
                confirmButton = {
                    TextButton(onClick = { onReset(); showConfirm = false }) { Text("초기화") }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirm = false }) { Text("취소") }
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Text("AetherOS 0.1.0 · Non-root · Local-first",
            fontSize = 12.sp, color = AetherColors.muted)
    }
}
