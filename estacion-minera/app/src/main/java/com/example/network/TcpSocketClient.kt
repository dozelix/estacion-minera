package com.example.network

import com.example.model.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.io.IOException

/**
 * Industrial TCP Socket client with coroutine-based asynchronous I/O (Dispatchers.IO).
 * Resilient against Wi-Fi micro-cuts through automatic retry loops with exponential backoff.
 */
class TcpSocketClient(
  private val externalScope: CoroutineScope
) {

  private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
  val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val _incomingMessages = MutableSharedFlow<String>(replay = 1)
  val incomingMessages: SharedFlow<String> = _incomingMessages.asSharedFlow()

  private val _logEvents = MutableSharedFlow<String>(replay = 5)
  val logEvents: SharedFlow<String> = _logEvents.asSharedFlow()

  private var socketJob: Job? = null
  private var activeSocket: Socket? = null
  private var socketWriter: BufferedWriter? = null

  private var currentHost: String = "10.0.2.2"
  private var currentPort: Int = 8080
  private var autoReconnect: Boolean = true

  fun connect(host: String, port: Int) {
    disconnect()
    currentHost = host.trim()
    currentPort = port
    autoReconnect = true

    socketJob = externalScope.launch(Dispatchers.IO) {
      var attempt = 0
      val baseDelayMs = 1000L
      val maxDelayMs = 15000L

      while (isActive && autoReconnect) {
        attempt++
        try {
          _connectionState.value = if (attempt == 1) ConnectionState.CONNECTING else ConnectionState.RECONNECTING
          log("Intentando conectar a $currentHost:$currentPort (intento $attempt)...")

          val socket = Socket()
          // 4-second connection timeout to detect dead links quickly
          socket.connect(InetSocketAddress(currentHost, currentPort), 4000)
          socket.soTimeout = 0 // Keep-alive stream
          activeSocket = socket

          val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
          val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
          socketWriter = writer

          _connectionState.value = ConnectionState.CONNECTED
          attempt = 0 // Reset attempt counter upon successful connection
          log("Conectado exitosamente con Nodo Sensor ($currentHost:$currentPort)")

          // Reading loop
          while (isActive && !socket.isClosed) {
            val line = reader.readLine()
            if (line == null) {
              log("El nodo remoto cerro el canal TCP.")
              break
            }
            if (line.isNotBlank()) {
              _incomingMessages.emit(line)
            }
          }
        } catch (e: SocketTimeoutException) {
          log("Timeout de socket en $currentHost:$currentPort: ${e.message}")
        } catch (e: SocketException) {
          log("Error de enlace TCP / microcorte Wi-Fi: ${e.message}")
        } catch (e: IOException) {
          log("Excepcion de I/O en socket: ${e.message}")
        } catch (e: Exception) {
          log("Fallo no fatal en socket: ${e.message}")
        } finally {
          cleanupSocket()
        }

        if (isActive && autoReconnect) {
          val backoff = (baseDelayMs * (1L shl (attempt.coerceAtMost(4)))).coerceAtMost(maxDelayMs)
          log("Reconectando en ${backoff / 1000}s tras microcorte...")
          delay(backoff)
        }
      }

      _connectionState.value = ConnectionState.DISCONNECTED
    }
  }

  suspend fun send(message: String): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
      val writer = socketWriter
      if (writer != null && activeSocket?.isConnected == true && activeSocket?.isClosed == false) {
        writer.write(message)
        if (!message.endsWith("\n")) {
          writer.write("\n")
        }
        writer.flush()
        log("Enviado -> $message")
        true
      } else {
        log("No se pudo enviar comando: Socket no conectado")
        false
      }
    } catch (e: Exception) {
      log("Error enviando comando: ${e.message}")
      false
    }
  }

  fun disconnect() {
    autoReconnect = false
    socketJob?.cancel()
    socketJob = null
    cleanupSocket()
    _connectionState.value = ConnectionState.DISCONNECTED
    log("Desconexion TCP solicitada")
  }

  private fun cleanupSocket() {
    try {
      socketWriter?.close()
    } catch (_: Exception) {}
    socketWriter = null

    try {
      activeSocket?.close()
    } catch (_: Exception) {}
    activeSocket = null
  }

  private fun log(message: String) {
    externalScope.launch {
      _logEvents.emit(message)
    }
  }
}
