package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ConnectionState
import com.example.model.RelayState
import com.example.model.TelemetryPacket
import com.example.model.VibrationSeverity
import com.example.repository.TelemetryRepository
import com.example.service.MonitoringForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonitoringUiState(
  val hostInput: String = "10.0.2.2",
  val portInput: String = "8080",
  val isSimulationMode: Boolean = false,
  val isForegroundServiceActive: Boolean = false,
  val autoTripEnabled: Boolean = true,
  val autoTripThreshold: Float = 7.1f,
  val vibrationHistory: List<Float> = emptyList(),
  val showConnectionDialog: Boolean = false,
  val showTerminalDialog: Boolean = false,
  val isLargeFontMode: Boolean = false, // Accessibility high legibility mode (18-65 years)
  val showEmergencyConfirmDialog: Boolean = false,
  val showTripConfirmDialog: Boolean = false,
  val lastCommandFeedback: String? = null
)

class MonitoringViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = TelemetryRepository.getInstance()

  val currentPacket: StateFlow<TelemetryPacket?> = repository.currentPacket
  val connectionState: StateFlow<ConnectionState> = repository.connectionState
  val terminalLogs: StateFlow<List<String>> = repository.terminalLogs

  private val _uiState = MutableStateFlow(
    MonitoringUiState(
      hostInput = repository.targetHost,
      portInput = repository.targetPort.toString(),
      isSimulationMode = repository.isSimulationActive()
    )
  )
  val uiState: StateFlow<MonitoringUiState> = _uiState.asStateFlow()

  // Track if anomaly generator is toggled
  private val _isAnomalyActive = MutableStateFlow(false)
  val isAnomalyActive: StateFlow<Boolean> = _isAnomalyActive.asStateFlow()

  init {
    // Observe incoming packets to build vibration waveform history and trigger auto-trip if enabled
    viewModelScope.launch {
      repository.currentPacket.collect { packet ->
        if (packet != null) {
          updateVibrationHistory(packet.vibrationRms)

          val state = _uiState.value
          if (state.autoTripEnabled &&
            packet.vibrationRms >= state.autoTripThreshold &&
            packet.relayState == RelayState.CLOSED
          ) {
            triggerAutoTrip(packet.vibrationRms)
          }
        } else {
          _uiState.value = _uiState.value.copy(vibrationHistory = emptyList())
        }
      }
    }
  }

  private fun updateVibrationHistory(sample: Float) {
    val current = _uiState.value.vibrationHistory.toMutableList()
    if (current.size >= 40) {
      current.removeAt(0)
    }
    current.add(sample)
    _uiState.value = _uiState.value.copy(vibrationHistory = current)
  }

  private fun triggerAutoTrip(rms: Float) {
    viewModelScope.launch {
      repository.sendRelayCommand(
        state = RelayState.OPEN,
        reason = "AUTO_TRIP_VIBRACION_CRITICA: RMS excesivo (%.2f mm/s >= %.2f mm/s)".format(
          rms,
          _uiState.value.autoTripThreshold
        ),
        operator = "SISTEMA_AUTO_TRIP_PROTECCION"
      )
      _uiState.value = _uiState.value.copy(
        lastCommandFeedback = "¡DISPARO AUTOMÁTICO DE SEGURIDAD! Vibración crítica detectada."
      )
    }
  }

  fun onHostChange(value: String) {
    _uiState.value = _uiState.value.copy(hostInput = value)
  }

  fun onPortChange(value: String) {
    _uiState.value = _uiState.value.copy(portInput = value)
  }

  fun toggleSimulationMode(isSimulation: Boolean) {
    _uiState.value = _uiState.value.copy(isSimulationMode = isSimulation)
    connectToTarget()
  }

  fun toggleAutoTrip(enabled: Boolean) {
    _uiState.value = _uiState.value.copy(autoTripEnabled = enabled)
  }

  fun toggleConnectionDialog(show: Boolean) {
    _uiState.value = _uiState.value.copy(showConnectionDialog = show)
  }

  fun toggleTerminalDialog(show: Boolean) {
    _uiState.value = _uiState.value.copy(showTerminalDialog = show)
  }

  fun toggleLargeFontMode() {
    _uiState.value = _uiState.value.copy(isLargeFontMode = !_uiState.value.isLargeFontMode)
  }

  fun setEmergencyConfirmDialog(show: Boolean) {
    _uiState.value = _uiState.value.copy(showEmergencyConfirmDialog = show)
  }

  fun setTripConfirmDialog(show: Boolean) {
    _uiState.value = _uiState.value.copy(showTripConfirmDialog = show)
  }

  fun connectToTarget() {
    val state = _uiState.value
    val port = state.portInput.toIntOrNull() ?: 8080
    repository.connect(state.hostInput, port, state.isSimulationMode)
  }

  fun disconnect() {
    repository.disconnect()
  }

  fun tripRelay(operatorEmail: String = "operador@faena.cl") {
    viewModelScope.launch {
      val success = repository.sendRelayCommand(
        state = RelayState.OPEN,
        reason = "DISPARO_PREVENTIVO_OPERADOR",
        operator = operatorEmail
      )
      _uiState.value = _uiState.value.copy(
        lastCommandFeedback = if (success) "Orden de apertura de relé enviada" else "Fallo al enviar comando a relé"
      )
    }
  }

  fun resetRelay(operatorEmail: String = "operador@faena.cl") {
    viewModelScope.launch {
      val success = repository.sendRelayCommand(
        state = RelayState.CLOSED,
        reason = "RESTABLECIMIENTO_OPERATIVO",
        operator = operatorEmail
      )
      _uiState.value = _uiState.value.copy(
        lastCommandFeedback = if (success) "Relé rearmado y motor operativo" else "Fallo al rearmar relé"
      )
    }
  }

  fun emergencyShutdown(operatorEmail: String = "operador@faena.cl") {
    viewModelScope.launch {
      val success = repository.sendRelayCommand(
        state = RelayState.OPEN,
        reason = "PARADA_DE_EMERGENCIA_FAENA",
        operator = operatorEmail
      )
      _uiState.value = _uiState.value.copy(
        lastCommandFeedback = if (success) "¡PARADA DE EMERGENCIA EJECUTADA!" else "Fallo en parada de emergencia"
      )
    }
  }

  fun toggleAnomaly() {
    val newState = !_isAnomalyActive.value
    _isAnomalyActive.value = newState
    repository.toggleSimulatorAnomaly(newState)
  }

  fun toggleForegroundService(context: Context) {
    val willBeActive = !_uiState.value.isForegroundServiceActive
    _uiState.value = _uiState.value.copy(isForegroundServiceActive = willBeActive)
    if (willBeActive) {
      MonitoringForegroundService.start(context)
    } else {
      MonitoringForegroundService.stop(context)
    }
  }

  fun clearLogs() {
    repository.clearLogs()
  }
}
