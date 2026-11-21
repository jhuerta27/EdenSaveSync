package com.jotasoft.edensavesync.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B1120)
val CardDark = Color(0xFF151F32)
val CardBorder = Color(0xFF263554)
val AccentCyan = Color(0xFF38BDF8)
val AccentPurple = Color(0xFFA855F7)
val AccentGreen = Color(0xFF22C55E)
val AccentAmber = Color(0xFFF59E0B)
val EdenPurple = Color(0xFFA253FE)
val EdenAmber = Color(0xFFE8A83D)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val EdenDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    tertiary = AccentGreen,
    background = BgDark,
    surface = CardDark,
    onPrimary = Color(0xFF082F49),
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)
