package com.jotasoft.edensavesync.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0B1120)
val CardDark = Color(0xFF151F32)
val CardBorder = Color(0xFF263554)
val SurfaceDark = Color(0xFF0F172A)
val TrackInactive = Color(0xFF1E293B)
val AccentCyan = Color(0xFF38BDF8)
val OnAccentCyan = Color(0xFF082F49)
val AccentPurple = Color(0xFFA855F7)
val AccentGreen = Color(0xFF22C55E)
val AccentAmber = Color(0xFFF59E0B)
val EdenPurple = Color(0xFFA253FE)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)

val StatusConnectedBg = Color(0xFF0E2A25)
val StatusDisconnectedBg = Color(0xFF271F16)
val BannerErrorBg = Color(0xFF7F1D1D)
val BannerErrorTint = Color(0xFFFCA5A5)
val BannerSuccessBg = Color(0xFF14532D)
val DangerRed = Color(0xFFEF4444)

val HeroActiveGradient = listOf(Color(0xFF1E3A8A), Color(0xFF4C1D95))
val HeroIdleGradient = listOf(Color(0xFF132A3E), TrackInactive)

val EdenDarkColorScheme = darkColorScheme(
    primary = AccentCyan,
    secondary = AccentPurple,
    tertiary = AccentGreen,
    background = BgDark,
    surface = CardDark,
    onPrimary = OnAccentCyan,
    onSecondary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)
