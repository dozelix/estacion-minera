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
val IndustrialAmber = Color(0xFFF59E0B)      // ámbar suave de operación / botón primario
val IndustrialAmberLight = Color(0xFFFDE68A) // ámbar claro suave sobre contenedores dark

val IndustrialCyan = Color(0xFF38BDF8)       // acento cian suave cielo
val IndustrialGreen = Color(0xFF4ADE80)      // acento verde suave equilibrado
val IndustrialRed = Color(0xFFF87171)        // acento rojo coral suave, descansado a la vista
val IndustrialYellow = Color(0xFFFBBF24)     // acento amarillo/ámbar cálido

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
      success = Color(0xFF4ADE80),           // verde suave armónico, no estridente (8:1 contraste)
      alert = Color(0xFFFBBF24),             // ámbar suave cálido
      info = Color(0xFF38BDF8),              // cian suave cielo
      danger = Color(0xFFF87171),            // rojo coral suave, descansado a la vista
      muted = Color(0xFF94A3B8)              // slate neutro equilibrado
    )
  } else {
    IndustrialStatusColors(
      success = Color(0xFF15803D),           // verde oscuro elegante
      alert = Color(0xFFB45309),             // ámbar oscuro legible
      info = Color(0xFF0369A1),              // cian oscuro legible
      danger = Color(0xFFB91C1C),            // rojo oscuro suave
      muted = Color(0xFF64748B)
    )
  }
}
