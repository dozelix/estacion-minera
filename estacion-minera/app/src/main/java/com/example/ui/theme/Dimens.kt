package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/* =============================================================================
 * DIMENSIONES SEMÁNTICAS — única fuente de verdad para espaciados, radios,
 * alturas táctiles y bordes de toda la app. Nada de dp literales dispersos:
 * la retícula base es de 4 dp y todos los toques cumplen ≥ 48 dp (Material
 * Accessibility y ISO 27400 ergonomía de sala de control).
 * ========================================================================== */
object Dimens {
  // --- Retícula de espaciado (base 4 dp)
  val Spacing4 = 4.dp
  val Spacing6 = 6.dp
  val Spacing8 = 8.dp
  val Spacing10 = 10.dp
  val Spacing12 = 12.dp
  val Spacing14 = 14.dp
  val Spacing16 = 16.dp
  val Spacing18 = 18.dp
  val Spacing20 = 20.dp
  val Spacing24 = 24.dp
  val Spacing32 = 32.dp

  // --- Alturas táctiles:
  val TouchMin = 48.dp        // mínimo WCAG/Material para elementos interactivos
  val ButtonHeight = 52.dp    // botones estándar (demo / secundarios)
  val ButtonHeightLg = 56.dp  // acción principal (login)
  val EmergencyHeight = 54.dp // parada de emergencia (60 en modo texto grande)

  // --- Iconografía (escalas Material estándar)
  val IconSm = 16.dp
  val IconMd = 20.dp
  val IconLg = 24.dp
  val IconXl = 28.dp
  val IconStatusLg = 32.dp    // iconos de métrica (temp/RPM)
  val IconHero = 44.dp        // icono de marca del login

  // --- Bordes de tarjetas de estado
  val BorderRegular = 1.dp
  val BorderStrong = 2.dp     // tarjeta del relé (señal de seguridad)

  // --- Componentes
  val BrandBadge = 76.dp      // contenedor del icono de marca (login)
  val ChartHeight = 170.dp    // osciloscopio de vibración
  val ConsoleHeight = 280.dp  // altura de la consola de tramas
}

/* =============================================================================
 * FORMAS M3 — jerarquía de esquinas coherente: pills (full), chips/badges
 * (small), tarjetas (large), contenedores hero (extraLarge).
 * ========================================================================== */
val IndustrialShapes = Shapes(
  extraSmall = RoundedCornerShape(6.dp),   // píldoras de datos del chart
  small = RoundedCornerShape(8.dp),        // badges de estado / mensajes
  medium = RoundedCornerShape(12.dp),      // campos de texto / botones secundarios
  large = RoundedCornerShape(16.dp),       // tarjetas de sección
  extraLarge = RoundedCornerShape(20.dp)   // contenedor hero / badge RX
)
