package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.ConnectionState
import com.example.model.RelayState
import com.example.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the TCP socket connection and coroutine scope active
 * even when the screen is locked or the device enters Doze Mode.
 * Displays a continuous notification showing active role and motor telemetry status.
 */
class MonitoringForegroundService : Service() {

  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private val repository = TelemetryRepository.getInstance()

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    startAsForeground(buildNotification("Iniciando estación de supervisión...", "Conectando al motor..."))

    // Observe telemetry and update notification dynamically
    serviceScope.launch {
      repository.currentPacket.collect { packet ->
        val statusText = if (packet != null) {
          val relay = if (packet.relayState == RelayState.CLOSED) "Relé: ACTIVO" else "Relé: DISPARADO"
          "Vib RMS: %.2f mm/s | Temp: %.1f°C | %s".format(packet.vibrationRms, packet.temperatureC, relay)
        } else {
          "Esperando telemetría del sensor..."
        }

        val connState = repository.connectionState.value
        val title = when (connState) {
          ConnectionState.CONNECTED -> "Supervisión Activa (Socket TCP)"
          ConnectionState.SIMULATING -> "Supervisión Activa (Simulación)"
          ConnectionState.RECONNECTING -> "Reconectando tras microcorte Wi-Fi..."
          ConnectionState.CONNECTING -> "Conectando a nodo sensor..."
          ConnectionState.DISCONNECTED -> "Estación Minera Desconectada"
        }

        updateNotification(title, statusText)
      }
    }
  }

  private fun startAsForeground(notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      } else {
        startForeground(
          NOTIFICATION_ID,
          notification,
          ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
      }
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun buildNotification(title: String, content: String): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    return NotificationCompat.Builder(this, CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(content)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setOngoing(true)
      .setContentIntent(pendingIntent)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .build()
  }

  private fun updateNotification(title: String, content: String) {
    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.notify(NOTIFICATION_ID, buildNotification(title, content))
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "Supervisión Motor Minero (IoT)",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Mantiene el enlace TCP con el sensor MPU6050 y relé con pantalla apagada"
        setShowBadge(false)
      }
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      manager.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    serviceScope.cancel()
    super.onDestroy()
  }

  companion object {
    const val CHANNEL_ID = "channel_mining_iot_monitor"
    const val NOTIFICATION_ID = 1001

    fun start(context: Context) {
      val intent = Intent(context, MonitoringForegroundService::class.java)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      val intent = Intent(context, MonitoringForegroundService::class.java)
      context.stopService(intent)
    }
  }
}
