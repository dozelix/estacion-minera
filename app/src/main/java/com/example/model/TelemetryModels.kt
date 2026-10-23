package com.example.model

import org.json.JSONObject

enum class ConnectionState {
  DISCONNECTED,
  CONNECTING,
  CONNECTED,
  RECONNECTING,
  SIMULATING
}

enum class RelayState {
  CLOSED, // Normal operational state (circuit closed, motor powered)
  OPEN    // Tripped / de-energized (circuit open, motor stopped)
}

enum class VibrationSeverity {
  NORMAL,   // < 4.5 mm/s
  ALERT,    // 4.5 - 7.1 mm/s
  CRITICAL  // > 7.1 mm/s
}

data class Vector3D(
  val x: Float,
  val y: Float,
  val z: Float
)

data class TelemetryPacket(
  val deviceId: String,
  val timestamp: Long,
  val accel: Vector3D,
  val gyro: Vector3D,
  val vibrationRms: Float,
  val temperatureC: Float,
  val rpm: Int,
  val relayState: RelayState,
  val rawJson: String = ""
) {
  val severity: VibrationSeverity
    get() = when {
      vibrationRms >= 7.1f -> VibrationSeverity.CRITICAL
      vibrationRms >= 4.5f -> VibrationSeverity.ALERT
      else -> VibrationSeverity.NORMAL
    }

  companion object {
    fun fromJson(jsonStr: String): TelemetryPacket? {
      return try {
        val root = JSONObject(jsonStr)
        if (!root.has("device_id") && !root.has("vibration_rms")) return null

        val devId = root.optString("device_id", "DESCONOCIDO")
        val ts = root.optLong("timestamp", System.currentTimeMillis() / 1000)

        val accelObj = root.optJSONObject("accel")
        val accel = if (accelObj != null) {
          Vector3D(
            accelObj.optDouble("x", 0.0).toFloat(),
            accelObj.optDouble("y", 0.0).toFloat(),
            accelObj.optDouble("z", 0.0).toFloat()
          )
        } else {
          Vector3D(0f, 0f, 0f)
        }

        val gyroObj = root.optJSONObject("gyro")
        val gyro = if (gyroObj != null) {
          Vector3D(
            gyroObj.optDouble("x", 0.0).toFloat(),
            gyroObj.optDouble("y", 0.0).toFloat(),
            gyroObj.optDouble("z", 0.0).toFloat()
          )
        } else {
          Vector3D(0f, 0f, 0f)
        }

        val vibRms = root.optDouble("vibration_rms", 0.0).toFloat()
        val temp = root.optDouble("temperature_c", 0.0).toFloat()
        val rpmVal = root.optInt("rpm", 0)
        val relayStr = root.optString("relay_state", "CLOSED").uppercase()
        val relay = if (relayStr == "OPEN") RelayState.OPEN else RelayState.CLOSED

        TelemetryPacket(
          deviceId = devId,
          timestamp = ts,
          accel = accel,
          gyro = gyro,
          vibrationRms = vibRms,
          temperatureC = temp,
          rpm = rpmVal,
          relayState = relay,
          rawJson = jsonStr
        )
      } catch (_: Exception) {
        null
      }
    }
  }
}

data class RelayCommand(
  val command: String = "SET_RELAY",
  val state: RelayState,
  val reason: String = "MANUAL_COMMAND",
  val operator: String = "operador@faena.cl"
) {
  fun toJson(): String {
    val json = JSONObject()
    json.put("command", command)
    json.put("state", state.name)
    json.put("reason", reason)
    json.put("operator", operator)
    json.put("timestamp", System.currentTimeMillis() / 1000)
    return json.toString()
  }
}
