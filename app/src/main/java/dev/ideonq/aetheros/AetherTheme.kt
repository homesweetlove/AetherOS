package dev.ideonq.aetheros

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object AetherColors {
    val ink = Color(0xFF17243A)
    val muted = Color(0xFF718096)
    val blue = Color(0xFF426BEA)
    val background = Color(0xFFF5F7FC)
    val paleBlue = Color(0xFFDFEAFF)
    val mint = Color(0xFFD9F5F0)
}

val aetherBackground = Brush.verticalGradient(
    listOf(Color(0xFFECF4FF), Color(0xFFF9FAFF), Color(0xFFF2F6FF))
)

@Composable
fun AetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AetherColors.blue,
            onPrimary = Color.White,
            background = AetherColors.background,
            surface = Color.White,
            onSurface = AetherColors.ink,
            onBackground = AetherColors.ink
        ),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(27.dp),
        color = Color.White.copy(alpha = .86f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .98f)),
        shadowElevation = 7.dp
    ) {
        Column(Modifier.padding(19.dp), content = content)
    }
}

@Composable
fun SectionHeading(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 11.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = AetherColors.ink)
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AetherColors.muted)
        }
    }
}
