package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* =============================================================================
 * TEMA INDUSTRIAL — Modo claro y modo oscuro (HMI de sala de control).
 * - dynamicColor = false: la identidad ámbar/slate NUNCA es sobrescrita por
 *   Material You (Android 12+), quitando el bug visible del original.
 * - darkTheme sigue al sistema: no forzarlo ni raparlo (criterio A5 UX).
 * - Los estados semánticos viven en industrialStatusColors() (Color.kt).
 * ========================================================================== */

private val DarkColorScheme =
  darkColorScheme(
    primary = IndustrialAmber,
    onPrimary = Color.Black,
    primaryContainer = IndustrialDarkSurfaceVariant,
    onPrimaryContainer = IndustrialAmberLight,
    secondary = IndustrialCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF103A54),
    onSecondaryContainer = IndustrialCyan,
    tertiary = IndustrialGreen,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF0B3B24),
    onTertiaryContainer = IndustrialGreen,
    background = IndustrialDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = IndustrialDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = IndustrialDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color(0xFF0A0E14),   // consola de tramas
    surfaceContainerLow = Color(0xFF121922),
    surfaceContainer = Color(0xFF18222E),
    surfaceContainerHigh = Color(0xFF1D2A38),
    surfaceContainerHighest = Color(0xFF223346),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF334155),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = IndustrialDarkBg,
    error = IndustrialRed,
    onError = Color.Black,
    errorContainer = Color(0xFF25191C),
    onErrorContainer = Color(0xFFFECDD3),
    scrim = Color(0xFF000000),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFFB26A00),                  // ámbar oscuro legible en claro
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFECB3),         // badges ámbar suaves
    onPrimaryContainer = Color(0xFFE65100),
    secondary = Color(0xFF0277BD),                // cian oscuro legible
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCAE8FF),
    onSecondaryContainer = Color(0xFF014A73),
    tertiary = Color(0xFF1B5E20),                 // verde oscuro legible
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB8F2C4),
    onTertiaryContainer = Color(0xFF0B3B24),
    background = SlateLightBg,
    onBackground = Color(0xFF0F172A),
    surface = SlateLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = SlateLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFEFF3F7),
    surfaceContainerHigh = Color(0xFFE8EDF2),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF1F5F9),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD2),
    onErrorContainer = Color(0xFF93000A),
    scrim = Color(0xFF000000),
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = IndustrialShapes,
    content = content,
  )
}
