package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.IndustrialGreen
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.IndustrialYellow
import com.example.ui.theme.ScopeChip
import com.example.ui.theme.ScopeFrameworkText
import com.example.ui.theme.ScopeMarker
import com.example.ui.theme.ScopePanel
import com.example.ui.theme.Dimens

/* =============================================================================
 * VibrationWaveform — osciloscopio de vibración RMS (mm/s) en Canvas.
 *
 * Lectura semántica del instrumento (criterio A1):
 *  - El panel SIEMPRE es oscuro (ScopePanel): look de HMI/osciloscopio
 *    industrial idéntico en tema claro u oscuro.
 *  - Las líneas guía (4,5 / 7,1 mm/s ISO 10816-3) y sus leyendas se calculan
 *    con la MISMA escala: la etiqueta queda alineada a su línea, no flotada.
 *  - Sin datos: guion central 'ESPERANDO TELEMETRÍA...' con marcador muted.
 * ========================================================================== */
private const val MAX_ESCALA = 10f        // mm/s en la escala vertical
private const val UMBRAL_ALERTA = 4.5f
private const val UMBRAL_CRITICO = 7.1f

@Composable
fun VibrationWaveform(
  vibrationHistory: List<Float>,
  currentRms: Float?,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .height(Dimens.ChartHeight)
      .background(ScopePanel, shape = MaterialTheme.shapes.medium)
      .padding(Dimens.Spacing10)
  ) {
    // Altura del área trazable el por padding superior/inferior (10 dp) — es la
    // MISMA altura que usa el Canvas, por eso las leyendas calzan con las líneas.
    val plotHeight: Dp = maxHeight - (Dimens.Spacing10 * 2)
    fun leyendaY(valor: Float): Dp =
      Dimens.Spacing10 + plotHeight * (1f - valor / MAX_ESCALA) - 6.dp

    Canvas(modifier = Modifier.fillMaxSize()) {
      val width = size.width
      val height = size.height

      fun getY(value: Float): Float {
        val clamped = value.coerceIn(0f, MAX_ESCALA)
        return height - (clamped / MAX_ESCALA * height)
      }

      val alertY = getY(UMBRAL_ALERTA)
      val criticalY = getY(UMBRAL_CRITICO)

      // Línea de alerta (4.5 mm/s — ISO 10816 zona C)
      drawLine(
        color = IndustrialYellow.copy(alpha = 0.4f),
        start = Offset(0f, alertY),
        end = Offset(width, alertY),
        strokeWidth = 1.dp.toPx()
      )

      // Línea crítica (7.1 mm/s — ISO 10816 zona D)
      drawLine(
        color = IndustrialRed.copy(alpha = 0.45f),
        start = Offset(0f, criticalY),
        end = Offset(width, criticalY),
        strokeWidth = 1.dp.toPx()
      )

      if (vibrationHistory.size >= 2 && currentRms != null) {
        val stepX = width / (vibrationHistory.size - 1).coerceAtLeast(1)
        val strokePath = Path()
        val fillPath = Path()

        fillPath.moveTo(0f, height)

        vibrationHistory.forEachIndexed { index, sample ->
          val x = index * stepX
          val y = getY(sample)
          if (index == 0) {
            strokePath.moveTo(x, y)
            fillPath.lineTo(x, y)
          } else {
            strokePath.lineTo(x, y)
            fillPath.lineTo(x, y)
          }
        }

        fillPath.lineTo((vibrationHistory.size - 1) * stepX, height)
        fillPath.close()

        val lineColor = when {
          currentRms >= UMBRAL_CRITICO -> IndustrialRed
          currentRms >= UMBRAL_ALERTA -> IndustrialYellow
          else -> IndustrialGreen
        }

        // Relleno bajo la curva
        drawPath(
          path = fillPath,
          brush = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
            startY = 0f,
            endY = height
          )
        )

        // Trazo de línea
        drawPath(
          path = strokePath,
          color = lineColor,
          style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Punto del último dato
        val latestX = (vibrationHistory.size - 1) * stepX
        val latestY = getY(vibrationHistory.last())
        drawCircle(
          color = Color.White,
          radius = 5.dp.toPx(),
          center = Offset(latestX, latestY)
        )
        drawCircle(
          color = lineColor,
          radius = 3.5.dp.toPx(),
          center = Offset(latestX, latestY)
        )
      }
    }

    if (vibrationHistory.isEmpty() || currentRms == null) {
      Text(
        text = "ESPERANDO RECEPCIÓN DE TELEMETRÍA...",
        color = ScopeFrameworkText,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        maxLines = 2,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
          .align(Alignment.Center)
          .padding(horizontal = 8.dp)
      )
    } else {
      // Leyendas ALINEADAS a sus líneas de umbral (posición derivada de la escala)
      Text(
        text = "CRÍTICO (7.1 mm/s)",
        color = IndustrialRed.copy(alpha = 0.6f),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = leyendaY(UMBRAL_CRITICO))
      )

      Text(
        text = "ALERTA (4.5 mm/s)",
        color = IndustrialYellow.copy(alpha = 0.6f),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(top = leyendaY(UMBRAL_ALERTA))
      )

      Text(
        text = "RANGO NORMAL",
        color = IndustrialGreen.copy(alpha = 0.6f),
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(bottom = Dimens.Spacing4)
      )
    }

    val rmsText = if (currentRms != null) "%.2f mm/s".format(currentRms) else "-- mm/s"
    val rmsColor = if (currentRms != null) {
      when {
        currentRms >= UMBRAL_CRITICO -> IndustrialRed
        currentRms >= UMBRAL_ALERTA -> IndustrialYellow
        else -> IndustrialGreen
      }
    } else {
      ScopeMarker
    }

    Text(
      text = rmsText,
      color = rmsColor,
      fontSize = 18.sp,
      fontWeight = FontWeight.Black,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .background(ScopeChip, shape = MaterialTheme.shapes.extraSmall)
        .padding(horizontal = Dimens.Spacing8, vertical = 2.dp)
    )
  }
}
