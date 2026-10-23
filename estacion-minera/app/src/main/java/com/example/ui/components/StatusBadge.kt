package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Dimens

/* =============================================================================
 * StatusBadge — entagrama de estado semántico reutilizable.
 * Doble codificación (criterio accesibilidad): COLOR + ICONO + TEXTO siempre
 * juntos, cursillo con el contenedor a la línea del estado. Se usa en la
 * conexión, el relé y la severidad de vibración; siystone nuevo estado usa
 * este mismo componente para mantener la jerarquía visual consistente.
 * ========================================================================== */
@Composable
fun StatusBadge(
  text: String,
  color: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  isLarge: Boolean = false,
  pill: Boolean = false,
) {
  val shape = if (pill) RoundedCornerShape(20.dp) else MaterialTheme.shapes.small
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .background(
        color = color.copy(alpha = 0.12f),
        shape = shape
      )
      .border(
        width = 1.dp,
        color = color.copy(alpha = 0.28f),
        shape = shape
      )
      .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,                     // el TEXTO ya codifica el estado (no duplicar)
      tint = color,
      modifier = Modifier.size(Dimens.IconSm)
    )
    Spacer(modifier = Modifier.width(Dimens.Spacing6))
    Text(
      text = text,
      color = color,
      fontSize = if (isLarge) 12.sp else 11.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      maxLines = 1,
      softWrap = false,
      letterSpacing = 0.2.sp
    )
  }
}
