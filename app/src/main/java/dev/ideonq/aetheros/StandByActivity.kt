package dev.ideonq.aetheros

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Intentional foreground-only clock. This is NOT a system Always On Display,
 * lock-screen replacement, or background wake-up service.
 */
class StandByActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        setContent { StandByClock(onClose = ::finish) }
    }
}

@Composable
private fun StandByClock(onClose: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { DeviceRepository(context) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
    val date = SimpleDateFormat("EEEE · MMMM d", Locale.getDefault()).format(Date(now))
    val battery = remember(now / 60_000) { repo.snapshot() }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF080D19), Color(0xFF101F31), Color(0xFF122F31))
            )
        )
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AETHER STANDBY", fontSize = 11.sp,
                letterSpacing = 4.sp, color = Color(0xFF9FC8D7))
            Spacer(Modifier.height(5.dp))
            Text(time, fontSize = 103.sp, lineHeight = 108.sp,
                fontWeight = FontWeight.ExtraLight, color = Color(0xFFE8F7F8))
            Text(date, fontSize = 19.sp, color = Color(0xFFC7D5DD))
            Spacer(Modifier.height(17.dp))
            Text(
                "${if (battery.isCharging) "⚡ " else ""}${battery.batteryPercent}%",
                fontSize = 16.sp, color = Color(0xFF9AE0CC)
            )
        }
        FilledTonalButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(15.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.White.copy(alpha = .12f),
                contentColor = Color.White
            )
        ) { Text("Close ×") }
    }
}
