package com.fkarach.arquibancada.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fkarach.arquibancada.R

/** Tokens do design system "Arquibancada" (tema único, noite de estádio). */
object Cor {
    val ground = Color(0xFF0B1712)
    val surface = Color(0xFF13241B)
    val raised = Color(0xFF1C3326)
    val border = Color(0xFF22392C)
    val borderStrong = Color(0xFF2C4436)
    val borderDashed = Color(0xFF3B5A48)
    val ink = Color(0xFFEEF3EE)
    val inkSoft = Color(0xFFD9E3DC)
    val inkMuted = Color(0xFFB7C7BC)
    val inkSubtle = Color(0xFF93A89A)
    val accent = Color(0xFFFF7A59)
    val onAccent = ground
    val inverse = ink
    val onInverse = ground
}

val Display = FontFamily(
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
)

val Corpo = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_bold, FontWeight.Bold),
    Font(R.font.barlow_bold, FontWeight.ExtraBold),
)

object Tipo {
    val appTitle = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 28.sp)
    val displayL = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 38.sp)
    val timeL = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp, lineHeight = 30.sp)
    val timeM = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 20.sp)
    val section = TextStyle(fontFamily = Display, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 22.sp)
    val event = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp)
    val body = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val channel = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 17.sp)
    val meta = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
    val eyebrow = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.2.sp)
    val badge = TextStyle(fontFamily = Corpo, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 0.6.sp)
}

@Composable
fun TemaArquibancada(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cor.accent,
            onPrimary = Cor.onAccent,
            background = Cor.ground,
            onBackground = Cor.ink,
            surface = Cor.surface,
            onSurface = Cor.ink,
            surfaceVariant = Cor.raised,
            onSurfaceVariant = Cor.inkMuted,
            outline = Cor.borderStrong,
        ),
        content = content,
    )
}
