package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionState
import com.example.model.RelayState
import com.example.model.VibrationSeverity
import com.example.security.EncryptedAuthStore
import com.example.ui.components.StatusBadge
import com.example.ui.components.VibrationWaveform
import com.example.ui.theme.ConsoleBg
import com.example.ui.theme.Dimens
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.IndustrialCyan
import com.example.ui.theme.IndustrialGreen
import com.example.ui.theme.IndustrialRed
import com.example.ui.theme.industrialStatusColors
import com.example.ui.theme.IndustrialYellow

/* =============================================================================
 * MonitoringScreen — Estación de Supervisión (rol visible, telemetría en vivo).
 * Semántica (criterios A1/A4/A5):
 *  - Todos los estados usan tokens de tema → correcto en claro y oscuro.
 *  - Doble codificación accesible: color + icono + texto (nunca solo color).
 *  - Confirmaciones de seguridad para acciones destructivas (disparo/rearme).
 *  - Scaffold maneja insets: cero padding duplicado y rotación tolerada por
 *    el ViewModel (se recrea solo el Composable, no el socket ni el estado).
 *  - Botones ≥ 48 dp; parada de emergencia destacada con confirmación.
 * ========================================================================== */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoringScreen(
  viewModel: MonitoringViewModel,
  activeUser: EncryptedAuthStore.UserProfile?,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val packet by viewModel.currentPacket.collectAsState()
  val connectionState by viewModel.connectionState.collectAsState()
  val logs by viewModel.terminalLogs.collectAsState()
  val isAnomalyActive by viewModel.isAnomalyActive.collectAsState()
  val status = industrialStatusColors()

  val isLarge = uiState.isLargeFontMode

  // Dynamic typography sizing based on accessibility mode
  val titleFontSize = if (isLarge) 16.sp else 13.sp
  val bodyFontSize = if (isLarge) 15.sp else 12.sp
  val metricFontSize = if (isLarge) 22.sp else 17.sp
  val buttonHeight = if (isLarge) 56.dp else 50.dp

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                .padding(horizontal = Dimens.Spacing10, vertical = Dimens.Spacing4)
            ) {
              Box(
                modifier = Modifier
                  .size(Dimens.Spacing10)
                  .background(MaterialTheme.colorScheme.onPrimaryContainer, CircleShape)
              )
              Spacer(modifier = Modifier.width(Dimens.Spacing8))
              Text(
                text = "ROL: ESTACIÓN DE SUPERVISIÓN",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Black,
                fontSize = if (isLarge) 13.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
              )
            }
            Text(
              text = "Motor Minero #01",
              style = if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        actions = {
          // Accessibility: Large Text Mode Toggle
          IconButton(
            onClick = viewModel::toggleLargeFontMode,
            modifier = Modifier
              .size(Dimens.TouchMin)
              .testTag("accessibility_font_button")
          ) {
            Icon(
              imageVector = Icons.Default.FormatSize,
              contentDescription = if (isLarge) "Desactivar texto grande" else "Activar texto grande para mayor legibilidad",
              tint = if (isLarge) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }

          // Terminal Inspector Button
          IconButton(
            onClick = { viewModel.toggleTerminalDialog(true) },
            modifier = Modifier
              .size(Dimens.TouchMin)
              .testTag("terminal_button")
          ) {
            Icon(Icons.Default.Terminal, contentDescription = "Consola de Tramas", tint = MaterialTheme.colorScheme.secondary)
          }

          // Connection Settings Button
          IconButton(
            onClick = { viewModel.toggleConnectionDialog(true) },
            modifier = Modifier
              .size(Dimens.TouchMin)
              .testTag("settings_button")
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Configuración de Red", tint = MaterialTheme.colorScheme.onSurface)
          }

          // Logout Button
          IconButton(
            onClick = onLogout,
            modifier = Modifier
              .size(Dimens.TouchMin)
              .testTag("logout_button")
          ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = status.danger)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(Dimens.Spacing16),
      verticalArrangement = Arrangement.spacedBy(Dimens.Spacing16)
    ) {
      // Operator Session & Connection Status Banner
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(Dimens.Spacing16)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Operator info with high legibility & flex containment
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.Spacing10)
            ) {
              Text(
                text = activeUser?.name ?: "Ing. Operador Minero",
                fontWeight = FontWeight.Bold,
                fontSize = if (isLarge) 16.sp else 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = activeUser?.role ?: "Supervisor de Mantenimiento",
                fontSize = if (isLarge) 13.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Connection State Chip (garantiza espacio fluido en una sola línea)
            Box(modifier = Modifier.wrapContentWidth()) {
              ConnectionBadge(
                connectionState = connectionState,
                hasRecentRecord = packet != null,
                isLarge = isLarge
              )
            }
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing12))

          // Resiliency Indicator (Doze / Screen off service) (FIX 3)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.Spacing8)
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = if (uiState.isForegroundServiceActive) status.success else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.IconMd)
              )
              Spacer(modifier = Modifier.width(Dimens.Spacing8))
              Text(
                text = "Servicio con Pantalla Apagada",
                fontSize = bodyFontSize,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }

            Switch(
              checked = uiState.isForegroundServiceActive,
              onCheckedChange = { viewModel.toggleForegroundService(context) },
              modifier = Modifier.testTag("foreground_service_switch"),
              colors = SwitchDefaults.colors(checkedThumbColor = status.success)
            )
          }
        }
      }

      // RELAY CONTROL BOARD
      val hasPacket = packet != null
      val isRelayOpen = packet?.relayState == RelayState.OPEN
      val (relayBadgeText, relayBadgeColor, relayIcon) = when {
        !hasPacket -> Triple("SIN ENLACE", status.muted, Icons.Default.PowerOff)
        isRelayOpen -> Triple("ABIERTO (DISPARADO)", status.danger, Icons.Default.PowerOff)
        else -> Triple("CERRADO (OPERATIVO)", status.success, Icons.Default.CheckCircle)
      }

      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (hasPacket && isRelayOpen) {
            MaterialTheme.colorScheme.errorContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
          }
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .border(
            width = Dimens.BorderStrong,
            color = when {
              !hasPacket -> MaterialTheme.colorScheme.outlineVariant
              isRelayOpen -> status.danger
              else -> status.success.copy(alpha = 0.6f)
            },
            shape = MaterialTheme.shapes.large
          )
      ) {
        Column(modifier = Modifier.padding(Dimens.Spacing18)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = "Estado del Relé",
                tint = relayBadgeColor,
                modifier = Modifier.size(Dimens.IconXl)
              )
              Spacer(modifier = Modifier.width(Dimens.Spacing10))
              Text(
                text = "RELÉ PRINCIPAL",
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                color = if (hasPacket && isRelayOpen) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurface
              )
            }

            // Relay Status Badge (Double coded with color + text + icon)
            StatusBadge(
              text = relayBadgeText,
              color = relayBadgeColor,
              icon = relayIcon,
              isLarge = isLarge
            )
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing16))

          // Action Buttons: Disparo Preventivo / Rearmar Relé (FIX 1: altura simétrica e intrinsic height)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Button(
              onClick = { viewModel.setTripConfirmDialog(true) },
              colors = ButtonDefaults.buttonColors(
                containerColor = IndustrialAmber,     // marca: alarma ámbar con texto negro
                contentColor = Color.Black
              ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("trip_relay_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(Icons.Default.PowerOff, contentDescription = null, modifier = Modifier.size(Dimens.IconMd))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Disparo Preventivo",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }

            Button(
              onClick = { viewModel.resetRelay(activeUser?.email ?: "operador@faena.cl") },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color.White
              ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 50.dp)
                .testTag("reset_relay_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(Dimens.IconMd))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Rearmar Relé",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing12))

          // EMERGENCY SHUTDOWN BUTTON (High visibility, protegido con confirmación)
          Button(
            onClick = { viewModel.setEmergencyConfirmDialog(true) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
              .fillMaxWidth()
              .height(if (isLarge) 60.dp else Dimens.EmergencyHeight)
              .testTag("emergency_shutdown_button")
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(Dimens.IconLg))
            Spacer(modifier = Modifier.width(Dimens.Spacing10))
            Text(
              text = "PARADA DE EMERGENCIA GENERAL",
              fontWeight = FontWeight.Black,
              fontSize = if (isLarge) 15.sp else 14.sp,
              letterSpacing = 0.5.sp
            )
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing12))

          // Auto-Trip Protection Switch (FIX 2: contraerse, nunca empujar fuera)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(end = Dimens.Spacing12)
            ) {
              Text(
                text = "Protección Auto-Trip (Desconexión Automática)",
                fontSize = bodyFontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Apertura automática de relé si vibración crítica >= 7.1 mm/s",
                fontSize = if (isLarge) 12.sp else 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
              )
            }
            Switch(
              checked = uiState.autoTripEnabled,
              onCheckedChange = viewModel::toggleAutoTrip,
              modifier = Modifier.testTag("auto_trip_switch"),
              colors = SwitchDefaults.colors(checkedThumbColor = IndustrialAmber)
            )
          }

          if (uiState.lastCommandFeedback != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = uiState.lastCommandFeedback!!,
              color = MaterialTheme.colorScheme.secondary,
              fontSize = if (isLarge) 13.sp else 12.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // VIBRATION WAVEFORM & SENSOR METRICS
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(Dimens.Spacing18)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "REGISTRO DE VIBRACIÓN",
                fontWeight = FontWeight.Bold,
                fontSize = titleFontSize,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Sensor MPU6050",
                fontSize = bodyFontSize,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            // Severity Chip (reutiliza StatusBadge)
            val (sevColor, sevText, sevIcon) = when {
              packet == null -> Triple(status.muted, "SIN DATOS", Icons.Default.Refresh)
              packet!!.severity == VibrationSeverity.CRITICAL -> Triple(status.danger, "PELIGRO", Icons.Default.Warning)
              packet!!.severity == VibrationSeverity.ALERT -> Triple(status.alert, "ALERTA", Icons.Default.Warning)
              else -> Triple(status.success, "NORMAL", Icons.Default.CheckCircle)
            }

            StatusBadge(
              text = sevText,
              color = sevColor,
              icon = sevIcon,
              isLarge = isLarge
            )
          }

          Spacer(modifier = Modifier.height(Dimens.Spacing14))

          // Dynamic Canvas Waveform (muestra estado en blanco si packet es null)
          VibrationWaveform(
            vibrationHistory = uiState.vibrationHistory,
            currentRms = packet?.vibrationRms
          )

          Spacer(modifier = Modifier.height(Dimens.Spacing12))

          // Anomaly simulation button (Only visible in Test mode)
          if (uiState.isSimulationMode) {
            Button(
              onClick = viewModel::toggleAnomaly,
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isAnomalyActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                contentColor = Color.White
              ),
              shape = MaterialTheme.shapes.medium,
              modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight)
                .testTag("toggle_anomaly_button")
            ) {
              Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(Dimens.IconMd))
              Spacer(modifier = Modifier.width(Dimens.Spacing8))
              Text(
                text = if (isAnomalyActive) "DETENER PRUEBA DE ANOMALÍA" else "INYECTAR ANOMALÍA EN PRUEBA",
                fontWeight = FontWeight.Bold,
                fontSize = if (isLarge) 14.sp else 13.sp
              )
            }
          }
        }
      }

      // TRIAXIAL ACCELERATION & GYROSCOPE (guiones '--' si no hay paquete)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
      ) {
        // Accelerometer
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(Dimens.Spacing14)) {
            Text(
              text = "ACELERACIÓN",
              fontWeight = FontWeight.Bold,
              fontSize = titleFontSize,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "Ejes m/s²",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.Spacing10))
            TelemetryRow(label = "Ax", value = packet?.accel?.let { "%.2f".format(it.x) } ?: "--", isLarge = isLarge)
            TelemetryRow(label = "Ay", value = packet?.accel?.let { "%.2f".format(it.y) } ?: "--", isLarge = isLarge)
            TelemetryRow(label = "Az", value = packet?.accel?.let { "%.2f".format(it.z) } ?: "--", isLarge = isLarge)
          }
        }

        // Gyroscope
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f)
        ) {
          Column(modifier = Modifier.padding(Dimens.Spacing14)) {
            Text(
              text = "GIRÓSCOPO",
              fontWeight = FontWeight.Bold,
              fontSize = titleFontSize,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "Ejes °/s",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.Spacing10))
            TelemetryRow(label = "Gx", value = packet?.gyro?.let { "%.2f".format(it.x) } ?: "--", isLarge = isLarge)
            TelemetryRow(label = "Gy", value = packet?.gyro?.let { "%.2f".format(it.y) } ?: "--", isLarge = isLarge)
            TelemetryRow(label = "Gz", value = packet?.gyro?.let { "%.2f".format(it.z) } ?: "--", isLarge = isLarge)
          }
        }
      }

      // MOTOR STATS (Temperature & RPM - High visibility typography)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
      ) {
        // Temperature
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(Dimens.Spacing14),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Thermostat,
              contentDescription = "Temperatura del motor",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(Dimens.IconStatusLg)
            )
            Spacer(modifier = Modifier.width(Dimens.Spacing10))
            Column {
              Text(
                text = "TEMPERATURA",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = packet?.let { "%.1f °C".format(it.temperatureC) } ?: "-- °C",
                fontWeight = FontWeight.Black,
                fontSize = metricFontSize,
                color = if (packet != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
              )
            }
          }
        }

        // RPM
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
          shape = MaterialTheme.shapes.medium,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            modifier = Modifier.padding(Dimens.Spacing14),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Speed,
              contentDescription = "Velocidad del motor",
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(Dimens.IconStatusLg)
            )
            Spacer(modifier = Modifier.width(Dimens.Spacing10))
            Column {
              Text(
                text = "VELOCIDAD",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = packet?.let { "${it.rpm} RPM" } ?: "-- RPM",
                fontWeight = FontWeight.Black,
                fontSize = metricFontSize,
                color = if (packet != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
              )
            }
          }
        }
      }
    }
  }

  // SAFETY CONFIRMATION DIALOG: EMERGENCY SHUTDOWN
  if (uiState.showEmergencyConfirmDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.setEmergencyConfirmDialog(false) },
      icon = {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.IconHero))
      },
      title = {
        Text("¿CONFIRMAR PARADA DE EMERGENCIA?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
      },
      text = {
        Text(
          text = "Esta acción desenergizará el motor principal inmediatamente para evitar fallas o accidentes. ¿Estás seguro de que deseas detenerlo?",
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.emergencyShutdown(activeUser?.email ?: "operador@faena.cl")
            viewModel.setEmergencyConfirmDialog(false)
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text("SÍ, DETENER MOTOR", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { viewModel.setEmergencyConfirmDialog(false) },
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text("Cancelar")
        }
      }
    )
  }

  // SAFETY CONFIRMATION DIALOG: PREVENTIVE TRIP
  if (uiState.showTripConfirmDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.setTripConfirmDialog(false) },
      icon = {
        Icon(Icons.Default.PowerOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(Dimens.IconHero))
      },
      title = {
        Text("¿Confirmar Disparo Preventivo de Relé?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
      },
      text = {
        Text(
          text = "Se enviará la orden de apertura al módulo de relé, desconectando la alimentación eléctrica del motor.",
          fontSize = 14.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.tripRelay(activeUser?.email ?: "operador@faena.cl")
            viewModel.setTripConfirmDialog(false)
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = Color.Black),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text("Confirmar Disparo", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { viewModel.setTripConfirmDialog(false) },
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text("Cancelar")
        }
      }
    )
  }

  // CONNECTION SETTINGS DIALOG (TCP o Modo Prueba)
  if (uiState.showConnectionDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.toggleConnectionDialog(false) },
      title = {
        Text("Configuración de Enlace", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.Spacing14)) {
          Text(
            text = "Selecciona el origen de los datos de telemetría. En modo TCP los datos se leen directamente desde el socket de red. En modo prueba se leen desde el generador de pruebas.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Activar Modo Prueba / Simulación", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Switch(
              checked = uiState.isSimulationMode,
              onCheckedChange = viewModel::toggleSimulationMode
            )
          }

          if (!uiState.isSimulationMode) {
            OutlinedTextField(
              value = uiState.hostInput,
              onValueChange = viewModel::onHostChange,
              label = { Text("IP del Nodo Sensor (ej: 10.0.2.2 o red local)", fontSize = 13.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
              value = uiState.portInput,
              onValueChange = viewModel::onPortChange,
              label = { Text("Puerto TCP (ej: 8080)", fontSize = 13.sp) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.connectToTarget()
            viewModel.toggleConnectionDialog(false)
          },
          colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber, contentColor = Color.Black),
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text(if (uiState.isSimulationMode) "Iniciar Prueba" else "Conectar TCP", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        Row {
          if (connectionState != ConnectionState.DISCONNECTED) {
            OutlinedButton(
              onClick = {
                viewModel.disconnect()
                viewModel.toggleConnectionDialog(false)
              },
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.height(buttonHeight)
            ) {
              Text("Desconectar")
            }
            Spacer(modifier = Modifier.width(Dimens.Spacing8))
          }
          TextButton(
            onClick = { viewModel.toggleConnectionDialog(false) },
            modifier = Modifier.height(buttonHeight)
          ) {
            Text("Cerrar")
          }
        }
      }
    )
  }

  // TERMINAL LOGS DIALOG (consola SIEMPRE oscura: legibilidad de logs igual en ambos temas)
  if (uiState.showTerminalDialog) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) {
      if (logs.isNotEmpty()) {
        listState.animateScrollToItem(logs.size - 1)
      }
    }

    AlertDialog(
      onDismissRequest = { viewModel.toggleTerminalDialog(false) },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Consola de Tramas JSON", fontWeight = FontWeight.Bold, fontSize = 18.sp)
          IconButton(onClick = viewModel::clearLogs, modifier = Modifier.size(Dimens.TouchMin)) {
            Icon(Icons.Default.Refresh, contentDescription = "Limpiar consola")
          }
        }
      },
      text = {
        Column {
          Text(
            text = "Inspección de tramas en vivo (Downlink telemetría y Uplink comandos):",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Spacer(modifier = Modifier.height(Dimens.Spacing8))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(Dimens.ConsoleHeight)
              .background(ConsoleBg, MaterialTheme.shapes.medium)
              .padding(Dimens.Spacing10)
          ) {
            if (logs.isEmpty()) {
              Text(
                text = "Sin tramas registradas aún. Conéctese a un nodo para iniciar el flujo.",
                color = Color.LightGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
              )
            } else {
              LazyColumn(state = listState) {
                items(logs) { entry ->
                  val color = when {
                    entry.startsWith("TX") -> IndustrialAmber
                    entry.startsWith("RX") -> IndustrialCyan
                    entry.startsWith("TEST-RX") -> IndustrialYellow
                    entry.startsWith("[SYS]") -> Color.LightGray
                    else -> Color.White
                  }
                  Text(
                    text = entry,
                    color = color,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                  )
                  Spacer(modifier = Modifier.height(Dimens.Spacing4))
                }
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { viewModel.toggleTerminalDialog(false) },
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.height(buttonHeight)
        ) {
          Text("Cerrar")
        }
      }
    )
  }
}

/* =============================================================================
 * Reutilizables de la pantalla
 * ========================================================================== */
@Composable
fun TelemetryRow(label: String, value: String, isLarge: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.Spacing4),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      fontFamily = FontFamily.Monospace,
      fontSize = if (isLarge) 15.sp else 13.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = value,
      fontFamily = FontFamily.Monospace,
      fontSize = if (isLarge) 16.sp else 14.sp,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}

@Composable
fun ConnectionBadge(connectionState: ConnectionState, hasRecentRecord: Boolean, isLarge: Boolean = false) {
  val status = industrialStatusColors()
  val (color, text, icon) = when (connectionState) {
    ConnectionState.CONNECTED -> Triple(status.success, "CONECTADO", Icons.Default.Wifi)
    ConnectionState.SIMULATING -> Triple(status.info, "MODO TEST", Icons.Default.Speed)
    ConnectionState.CONNECTING -> Triple(status.alert, "CONECTANDO...", Icons.Default.Refresh)
    ConnectionState.RECONNECTING -> Triple(status.alert, "RECONECTANDO...", Icons.Default.Refresh)
    ConnectionState.DISCONNECTED -> {
      if (hasRecentRecord) {
        Triple(status.muted, "ÚLTIMO REGISTRO", Icons.Default.WifiOff)
      } else {
        Triple(status.danger, "DESCONECTADO", Icons.Default.WifiOff)
      }
    }
  }

  StatusBadge(
    text = text,
    color = color,
    icon = icon,
    isLarge = isLarge,
    pill = true
  )
}
