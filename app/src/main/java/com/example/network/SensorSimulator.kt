package com.example.network

import com.example.model.RelayCommand
import com.example.model.RelayState
import com.example.model.TelemetryPacket
import com.example.model.Vector3D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/**
 * Built-in industrial MPU6050 sensor & motor simulator for testing and evaluation.
 * Generates realistic physical telemetry and responds to Relay control commands.
 */
class SensorSimulator(private val scope: CoroutineScope) {

  private var job: Job? = null
  private val _telemetryFlow = MutableSharedFlow<TelemetryPacket>(replay = 1)
  val telemetryFlow: SharedFlow<TelemetryPacket> = _telemetryFlow.asSharedFlow()

  private var currentRelayState = RelayState.CLOSED
  private var anomalyMode = false
  private var stepCount = 0L

  fun start() {
    stop()
    job = scope.launch(Dispatchers.Default) {
      while (isActive) {
        stepCount++
        val packet = generatePacket(stepCount)
        _telemetryFlow.emit(packet)
        delay(600) // Emits every 600ms
      }
    }
  }

  fun stop() {
    job?.cancel()
    job = null
  }

  fun handleCommand(command: RelayCommand) {
    currentRelayState = command.state
  }

  fun setAnomalyMode(enabled: Boolean) {
    anomalyMode = enabled
  }

  fun isAnomalyMode(): Boolean = anomalyMode

  private fun generatePacket(t: Long): TelemetryPacket {
    val isMotorRunning = currentRelayState == RelayState.CLOSED

    val baseRms = if (!isMotorRunning) {
      0.15f + Random.nextFloat() * 0.1f // Motor off / relay tripped
    } else if (anomalyMode) {
      7.5f + (sin(t * 0.4).toFloat() * 1.8f) + Random.nextFloat() * 0.8f // Critical vibration > 7.1
    } else {
      2.3f + (sin(t * 0.15).toFloat() * 0.9f) + Random.nextFloat() * 0.4f // Normal vibration 1.8 - 3.6
    }

    val rpm = if (!isMotorRunning) {
      0
    } else if (anomalyMode) {
      1420 + (Random.nextInt(80) - 40)
    } else {
      1485 + (Random.nextInt(15) - 7)
    }

    val temp = if (!isMotorRunning) {
      (40.0f - (t % 15) * 0.2f).coerceAtLeast(24.0f)
    } else if (anomalyMode) {
      (68.5f + (t % 20) * 0.4f).coerceAtMost(88.0f)
    } else {
      49.2f + (sin(t * 0.05).toFloat() * 1.5f)
    }

    val accelX = if (isMotorRunning) (sin(t * 0.8).toFloat() * baseRms * 0.2f) else 0.02f
    val accelY = if (isMotorRunning) (sin(t * 0.5).toFloat() * baseRms * 0.3f) else 0.04f
    val accelZ = if (isMotorRunning) (9.81f + sin(t * 1.2).toFloat() * baseRms * 0.4f) else 9.81f

    val gyroX = if (isMotorRunning) (sin(t * 0.6).toFloat() * baseRms * 0.05f) else 0f
    val gyroY = if (isMotorRunning) (sin(t * 0.4).toFloat() * baseRms * 0.05f) else 0f
    val gyroZ = if (isMotorRunning) (sin(t * 0.9).toFloat() * baseRms * 0.08f) else 0f

    val simulatedJson = """
      {"device_id":"MPU6050_MOTOR_01","timestamp":${System.currentTimeMillis() / 1000},"accel":{"x":${"%.2f".format(accelX)},"y":${"%.2f".format(accelY)},"z":${"%.2f".format(accelZ)}},"gyro":{"x":${"%.2f".format(gyroX)},"y":${"%.2f".format(gyroY)},"z":${"%.2f".format(gyroZ)}},"vibration_rms":${"%.2f".format(baseRms)},"temperature_c":${"%.1f".format(temp)},"rpm":$rpm,"relay_state":"${currentRelayState.name}"}
    """.trimIndent().replace("\n", "")

    return TelemetryPacket(
      deviceId = "MPU6050_MOTOR_01",
      timestamp = System.currentTimeMillis() / 1000,
      accel = Vector3D(accelX, accelY, accelZ),
      gyro = Vector3D(gyroX, gyroY, gyroZ),
      vibrationRms = baseRms,
      temperatureC = temp,
      rpm = rpm,
      relayState = currentRelayState,
      rawJson = simulatedJson
    )
  }
}
