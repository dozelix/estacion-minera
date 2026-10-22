package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/* =============================================================================
 * PALETA INDUSTRIAL — identidad visual de la Estación Minera (fija en ambos
 * temas). Los estados (success/alert/info/danger) tienen variante por tema en
 * "industrialStatusColors()" para garantizar contraste AA en modo claro.
 * ========================================================================== */

// --- Acentos de marca (se usan en contenedores/bordes seguros en ambos temas)
val IndustrialAmber = Color(0xFFFF8F00)      // ámbar de operación / botón primario
val IndustrialAmberLight = Color(0xFFFFB74D) // ámbar sobre contenedores dark

val IndustrialCyan = Color(0xFF00B0FF)       // acento de datos/terminal (dark)
val IndustrialGreen = Color(0xFF00E676)      // acento de éxito (dark)
val IndustrialRed = Color(0xFFFF3D00)        // acento de emergencia (dark)
val IndustrialYellow = Color(0xFFFFD600)     // acento de alerta (dark)

// --- Superficies del tema oscuro industrial
val IndustrialDarkBg = Color(0xFF0B1017)
val IndustrialDarkSurface = Color(0xFF141D29)
val IndustrialDarkSurfaceVariant = Color(0xFF1F2B3B)

// --- Superficies del tema claro industrial (escala slate)
val SlateLightBg = Color(0xFFF1F5F9)
val SlateLightSurface = Color(0xFFFFFFFF)
val SlateLightSurfaceVariant = Color(0xFFE2E8F0)

// --- Panel osciloscopio: instrumento SIEMPRE oscuro dentro de la tarjeta
//     (look de HMI industrial; legible idéntico en tema claro u oscuro)
val ScopePanel = Color(0xFF0A0E14)           // fondo del gráfico
val ScopeFrameworkText = Color(0xFF64748B)   // texto "esperando telemetría"
val ScopeChip = Color(0xFF141D29)            // píldora del valor RMS actual
val ScopeMarker = Color(0xFF94A3B8)          // marcador sin datos

// --- Consola de tramas JSON: SIEMPRE oscura (como un simulador/guía de red)
val ConsoleBg = Color(0xFF070B10)

/* =============================================================================
 * COLORES DE ESTADO SEMÉNTICOS — se resuelven según el tema activo.
 * Métrica de contraste: pares seleccionados para legibilidad ≥ AA en 12–14sp.
 * ========================================================================== */
@Immutable
data class IndustrialStatusColors(
  val success: Color,   // NORMAL / relay cerrado
  val alert: Color,     // ALERTA zona C / reconectando
  val info: Color,      // datos de sensor / modo prueba
  val danger: Color,    // PELIGRO zona D / relay disparado
  val muted: Color      // estado neutro / sin datos
)

@Composable
fun industrialStatusColors(): IndustrialStatusColors {
  val isDark = isSystemInDarkTheme()
  return if (isDark) {
    IndustrialStatusColors(
      success = IndustrialGreen,             // 00E676 sobre 141D29
      alert = IndustrialYellow,              // FFD600 sobre 141D29
      info = IndustrialCyan,                 // 00B0FF sobre 141D29
      danger = IndustrialRed,                // FF3D00 sobre 141D29
      muted = Color(0xFF94A3B8)
    )
  } else {
    IndustrialStatusColors(
      success = Color(0xFF1B5E20),           // verde oscuro sobre blanco/slate
      alert = Color(0xFFB26A00),             // ámbar oscuro legible
      info = Color(0xFF0277BD),              // cian oscuro legible
      danger = Color(0xFFC62828),            // rojo oscuro legible
      muted = Color(0xFF64748B)
    )
  }
}
