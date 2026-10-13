package com.alameen.fullquran

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF05241A)
val Surface1 = Color(0xFF0B3A29)
val CardColor = Color(0xFF0F4B35)
val Gold = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF3DC8E)
val TextMain = Color(0xFFF4F1E4)
val TextDim = Color(0xFFB7C9BE)

@Composable
fun QuranTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            onPrimary = Color(0xFF1B1400),
            secondary = GoldLight,
            background = Bg,
            onBackground = TextMain,
            surface = Surface1,
            onSurface = TextMain,
            surfaceVariant = CardColor,
            onSurfaceVariant = TextDim
        ),
        content = content
    )
}
