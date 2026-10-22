package com.example.repository

import com.example.model.ConnectionState
import com.example.model.RelayCommand
import com.example.model.RelayState
import com.example.model.TelemetryPacket
import com.example.network.SensorSimulator
import com.example.network.TcpSocketClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Central repository for telemetry stream, TCP socket lifecycle and relay commanding.
 * Preserves the most recent genuine TCP telemetry record, while ensuring test data
 * is never persisted as real historical telemetry.
 */
class TelemetryRepository private constructor() {

  private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  val tcpClient = TcpSocketClient(scope)
  val simulator = SensorSimulator(scope)

  // Current active packet displayed in UI (null if no data has been received yet)
  private val _currentPacket = MutableStateFlow<TelemetryPacket?>(null)
  val currentPacket: StateFlow<TelemetryPacket?> = _currentPacket.asStateFlow()

  // Retains the most recent genuine TCP packet (never populated by tests/simulations)
  private var lastGenuineTcpPacket: TelemetryPacket? = null

  private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
  val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

  private var isSimulating = false
  var targetHost: String = "10.0.2.2"
    private set
  var targetPort: Int = 8080
    private set

  init {
    // Listen to real TCP incoming frames strictly from network socket
    scope.launch {
      tcpClient.incomingMessages.collect { rawLine ->
        addLog("RX: $rawLine")
        val packet = TelemetryPacket.fromJson(rawLine)
        if (packet != null) {
          lastGenuineTcpPacket = packet
          _currentPacket.value = packet
        }
      }
    }

    // Forward TCP connection state when not in simulator/test mode
    scope.launch {
      tcpClient.connectionState.collect { state ->
        if (!isSimulating) {
          _connectionState.value = state
          if (state == ConnectionState.DISCONNECTED) {
            // When disconnected, show the most recent genuine TCP record if available, or null (blank)
            _currentPacket.value = lastGenuineTcpPacket
          }
        }
      }
    }

    // Forward TCP client internal log events
    scope.launch {
      tcpClient.logEvents.collect { logMsg ->
        addLog("[SYS] $logMsg")
      }
    }

    // Listen to test/simulator telemetry (ONLY active when test mode is enabled)
    scope.launch {
      simulator.telemetryFlow.collect { packet ->
        if (isSimulating) {
          _currentPacket.value = packet
          addLog("TEST-RX: ${packet.rawJson}")
        }
      }
    }
  }

  fun connect(host: String, port: Int, simulation: Boolean) {
    targetHost = host.trim()
    targetPort = port
    isSimulating = simulation

    if (simulation) {
      tcpClient.disconnect()
      simulator.start()
      _connectionState.value = ConnectionState.SIMULATING
      addLog("[MODO TEST] Modo prueba activo. Los datos no se almacenarán como lecturas reales.")
    } else {
      simulator.stop()
      // If we previously had test data displayed, clear it back to the genuine TCP record or null
      _currentPacket.value = lastGenuineTcpPacket
      tcpClient.connect(targetHost, targetPort)
      addLog("[ENLACE TCP] Conectando a $targetHost:$targetPort...")
    }
  }

  fun disconnect() {
    isSimulating = false
    simulator.stop()
    tcpClient.disconnect()
    _connectionState.value = ConnectionState.DISCONNECTED
    // Revert to the last genuine TCP packet if one exists, otherwise null (blank)
    _currentPacket.value = lastGenuineTcpPacket
    addLog("[ENLACE] Desconectado.")
  }

  suspend fun sendRelayCommand(
    state: RelayState,
    reason: String = "OPERATOR_COMMAND",
    operator: String = "operador@faena.cl"
  ): Boolean {
    val cmd = RelayCommand(
      command = "SET_RELAY",
      state = state,
      reason = reason,
      operator = operator
    )
    val json = cmd.toJson()
    addLog("TX CMD: $json")

    return if (isSimulating) {
      simulator.handleCommand(cmd)
      addLog("[TEST] Comando de relé procesado en prueba: $state")
      true
    } else {
      tcpClient.send(json)
    }
  }

  fun toggleSimulatorAnomaly(enabled: Boolean) {
    simulator.setAnomalyMode(enabled)
    addLog("[TEST] Inyección de anomalía en prueba: $enabled")
  }

  fun isSimulatorAnomaly(): Boolean = simulator.isAnomalyMode()

  fun isSimulationActive(): Boolean = isSimulating

  fun hasGenuineData(): Boolean = lastGenuineTcpPacket != null

  private fun addLog(entry: String) {
    val current = _terminalLogs.value.toMutableList()
    if (current.size >= 100) {
      current.removeAt(0)
    }
    current.add(entry)
    _terminalLogs.value = current
  }

  fun clearLogs() {
    _terminalLogs.value = emptyList()
  }

  companion object {
    @Volatile
    private var instance: TelemetryRepository? = null

    fun getInstance(): TelemetryRepository {
      return instance ?: synchronized(this) {
        instance ?: TelemetryRepository().also { instance = it }
      }
    }
  }
}
