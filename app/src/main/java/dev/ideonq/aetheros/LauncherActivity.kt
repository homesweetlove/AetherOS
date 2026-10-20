package dev.ideonq.aetheros

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Optional HOME app. AetherOS never installs itself as default silently.
 * Users can restore One UI Home in Android's Default Apps settings.
 */
class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AetherTheme { AetherHome() } }
    }
}

@Composable
private fun AetherHome() {
    val context = LocalContext.current
    val repository = remember(context) { DeviceRepository(context) }
    val apps = remember { repository.launchableApps() }
    var search by remember { mutableStateOf("") }
    val results = remember(apps, search) {
        apps.filter { search.isBlank() || it.title.contains(search, ignoreCase = true) }
    }
    Column(
        Modifier.fillMaxSize().background(aetherBackground)
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(36.dp))
        Text(
            SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
            fontSize = 14.sp, color = AetherColors.muted
        )
        Text("Your space.", fontSize = 38.sp, fontWeight = FontWeight.Light,
            color = AetherColors.ink)
        Spacer(Modifier.height(16.dp))
        TextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search apps") },
            leadingIcon = { Text("⌕", fontSize = 24.sp) },
            shape = RoundedCornerShape(23.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = .9f),
                unfocusedContainerColor = Color.White.copy(alpha = .8f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("APPLICATIONS", fontSize = 11.sp, letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold, color = AetherColors.muted,
                modifier = Modifier.weight(1f))
            Text("${results.size}", fontSize = 12.sp, color = AetherColors.muted)
        }
        Spacer(Modifier.height(15.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(results, key = { it.packageName }) { app ->
                Column(
                    modifier = Modifier.clickable { repository.launchApp(app.packageName) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val icon = remember(app.packageName) {
                        runCatching {
                            context.packageManager.getApplicationIcon(app.packageName)
                                .toBitmap(100, 100).asImageBitmap()
                        }.getOrNull()
                    }
                    Box(
                        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = .9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Image(icon, contentDescription = app.title,
                                modifier = Modifier.size(54.dp))
                        } else {
                            Text(app.title.take(1).uppercase(), color = AetherColors.blue,
                                fontSize = 23.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(app.title, fontSize = 11.sp, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = AetherColors.ink)
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White.copy(alpha = .85f),
            shadowElevation = 5.dp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = {
                    context.startActivity(Intent(context, MainActivity::class.java))
                }) { Text("✦ AetherOS") }
                TextButton(onClick = {
                    repository.openSettings(Settings.ACTION_HOME_SETTINGS)
                }) { Text("기본 홈 선택") }
            }
        }
    }
}
