package com.example

import com.example.model.RelayCommand
import com.example.model.RelayState
import com.example.model.TelemetryPacket
import com.example.model.Vector3D
import com.example.model.VibrationSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM Unit tests verifying industrial telemetry parsing, severity thresholds,
 * and relay command formatting for the Mining IoT supervision station.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleUnitTest {

  private fun createSamplePacket(rms: Float): TelemetryPacket {
    return TelemetryPacket(
      deviceId = "MPU6050_TEST",
      timestamp = 1729512345L,
      accel = Vector3D(0f, 0f, 9.8f),
      gyro = Vector3D(0f, 0f, 0f),
      vibrationRms = rms,
      temperatureC = 45f,
      rpm = 1450,
      relayState = RelayState.CLOSED,
      rawJson = ""
    )
  }

  @Test
  fun telemetryPacket_parsesValidJsonCorrectly() {
    val jsonSample = """
      {
        "device_id": "MPU6050_MOTOR_01",
        "timestamp": 1729512345,
        "accel": { "x": 0.12, "y": 0.45, "z": 9.81 },
        "gyro": { "x": 0.01, "y": 0.02, "z": 0.05 },
        "vibration_rms": 2.45,
        "temperature_c": 54.2,
        "rpm": 1480,
        "relay_state": "CLOSED"
      }
    """.trimIndent()

    val packet = TelemetryPacket.fromJson(jsonSample)

    assertNotNull("El paquete de telemetría no debe ser nulo", packet)
    assertEquals("MPU6050_MOTOR_01", packet!!.deviceId)
    assertEquals(1729512345L, packet.timestamp)
    assertEquals(0.12f, packet.accel.x, 0.001f)
    assertEquals(0.45f, packet.accel.y, 0.001f)
    assertEquals(9.81f, packet.accel.z, 0.001f)
    assertEquals(2.45f, packet.vibrationRms, 0.001f)
    assertEquals(54.2f, packet.temperatureC, 0.001f)
    assertEquals(1480, packet.rpm)
    assertEquals(RelayState.CLOSED, packet.relayState)
    assertEquals(VibrationSeverity.NORMAL, packet.severity)
  }

  @Test
  fun vibrationSeverity_evaluatesThresholdsCorrectly() {
    val normalPacket = createSamplePacket(2.1f)
    assertEquals(VibrationSeverity.NORMAL, normalPacket.severity)

    val alertPacket = createSamplePacket(5.2f)
    assertEquals(VibrationSeverity.ALERT, alertPacket.severity)

    val criticalPacket = createSamplePacket(7.8f)
    assertEquals(VibrationSeverity.CRITICAL, criticalPacket.severity)
  }

  @Test
  fun relayCommand_serializesToJsonCorrectly() {
    val command = RelayCommand(
      command = "SET_RELAY",
      state = RelayState.OPEN,
      reason = "PARADA_DE_EMERGENCIA",
      operator = "operador@faena.cl"
    )

    val jsonString = command.toJson()

    assertTrue(jsonString.contains("\"command\":\"SET_RELAY\""))
    assertTrue(jsonString.contains("\"state\":\"OPEN\""))
    assertTrue(jsonString.contains("\"reason\":\"PARADA_DE_EMERGENCIA\""))
    assertTrue(jsonString.contains("\"operator\":\"operador@faena.cl\""))
  }

  @Test
  fun telemetryPacket_handlesEmptyOrCorruptedJsonGracefully() {
    val corruptJson = "{ malformed json .. "
    val packet = TelemetryPacket.fromJson(corruptJson)
    assertNull("El parseo de JSON corrupto debe retornar null sin arrojar excepción", packet)
  }
}
