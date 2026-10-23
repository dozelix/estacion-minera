import {
  ConnectionState,
  RelayCommand,
  RelayState,
  TelemetryPacket,
  TerminalLogEntry,
  calculateSeverity
} from '../types/telemetry';
import { sensorSimulator } from './sensorSimulator';

export interface NetworkConfig {
  host: string;
  port: number;
  autoReconnect: boolean;
  useSimulator: boolean;
  reconnectDelayMs: number;
}

export class NetworkManager {
  private config: NetworkConfig = {
    host: '10.0.2.2',
    port: 8080,
    autoReconnect: true,
    useSimulator: true,
    reconnectDelayMs: 3000
  };

  private connectionState: ConnectionState = ConnectionState.DISCONNECTED;
  private socket: WebSocket | null = null;
  private reconnectTimer: number | null = null;
  private stateListeners: ((state: ConnectionState) => void)[] = [];
  private packetListeners: ((packet: TelemetryPacket) => void)[] = [];
  private logListeners: ((entry: TerminalLogEntry) => void)[] = [];
  private unsubscribeSim: (() => void) | null = null;

  constructor() {
    // Load persisted network config if present
    const saved = localStorage.getItem('mining_network_config');
    if (saved) {
      try {
        this.config = { ...this.config, ...JSON.parse(saved) };
      } catch {}
    }
  }

  getConfig(): NetworkConfig {
    return { ...this.config };
  }

  saveConfig(newConfig: Partial<NetworkConfig>): void {
    this.config = { ...this.config, ...newConfig };
    localStorage.setItem('mining_network_config', JSON.stringify(this.config));
  }

  getConnectionState(): ConnectionState {
    return this.connectionState;
  }

  onStateChange(cb: (state: ConnectionState) => void): () => void {
    this.stateListeners.push(cb);
    cb(this.connectionState);
    return () => {
      this.stateListeners = this.stateListeners.filter(l => l !== cb);
    };
  }

  onPacket(cb: (packet: TelemetryPacket) => void): () => void {
    this.packetListeners.push(cb);
    return () => {
      this.packetListeners = this.packetListeners.filter(l => l !== cb);
    };
  }

  onLog(cb: (entry: TerminalLogEntry) => void): () => void {
    this.logListeners.push(cb);
    return () => {
      this.logListeners = this.logListeners.filter(l => l !== cb);
    };
  }

  private setState(newState: ConnectionState): void {
    this.connectionState = newState;
    this.stateListeners.forEach(cb => cb(newState));
  }

  private addLog(direction: 'RX' | 'TX' | 'SYS', payload: string, isError = false): void {
    const entry: TerminalLogEntry = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
      direction,
      timestamp: new Date().toLocaleTimeString(),
      payload,
      isError
    };
    this.logListeners.forEach(cb => cb(entry));
  }

  connect(): void {
    this.disconnect();

    if (this.config.useSimulator) {
      this.setState(ConnectionState.CONNECTING);
      this.addLog('SYS', `Iniciando simulador interno MPU6050 a 2 Hz...`);

      setTimeout(() => {
        this.setState(ConnectionState.SIMULATING);
        this.addLog('SYS', `Simulador activo (MPU6050_MOTOR_01 enlazado).`);

        sensorSimulator.start(500);
        this.unsubscribeSim = sensorSimulator.subscribe(packet => {
          this.packetListeners.forEach(cb => cb(packet));
          if (packet.rawJson) {
            this.addLog('RX', packet.rawJson);
          }
        });
      }, 500);
      return;
    }

    // Direct WebSocket/TCP bridge mode
    this.setState(ConnectionState.CONNECTING);
    this.addLog('SYS', `Conectando a socket ws://${this.config.host}:${this.config.port}...`);

    try {
      this.socket = new WebSocket(`ws://${this.config.host}:${this.config.port}`);

      this.socket.onopen = () => {
        this.setState(ConnectionState.CONNECTED);
        this.addLog('SYS', `Conexión TCP establecida con ${this.config.host}:${this.config.port}`);
      };

      this.socket.onmessage = (event) => {
        const raw = event.data;
        if (typeof raw === 'string') {
          this.addLog('RX', raw);
          try {
            const data = JSON.parse(raw);
            const packet: TelemetryPacket = {
              deviceId: data.device_id || 'UNKNOWN',
              timestamp: data.timestamp || Math.floor(Date.now() / 1000),
              accel: data.accel || { x: 0, y: 0, z: 9.81 },
              gyro: data.gyro || { x: 0, y: 0, z: 0 },
              vibrationRms: data.vibration_rms || 0,
              temperatureC: data.temperature_c || 0,
              rpm: data.rpm || 0,
              relayState: (data.relay_state === 'OPEN') ? RelayState.OPEN : RelayState.CLOSED,
              rawJson: raw,
              severity: calculateSeverity(data.vibration_rms || 0)
            };
            this.packetListeners.forEach(cb => cb(packet));
          } catch (e) {
            this.addLog('SYS', `Error parseando JSON: ${e}`, true);
          }
        }
      };

      this.socket.onerror = (err) => {
        this.addLog('SYS', `Fallo de conexión en socket: ${err}`, true);
      };

      this.socket.onclose = () => {
        this.addLog('SYS', `Socket cerrado.`, true);
        if (this.config.autoReconnect) {
          this.setState(ConnectionState.RECONNECTING);
          this.scheduleReconnect();
        } else {
          this.setState(ConnectionState.DISCONNECTED);
        }
      };
    } catch (e) {
      this.addLog('SYS', `Excepción abriendo socket: ${e}`, true);
      if (this.config.autoReconnect) {
        this.setState(ConnectionState.RECONNECTING);
        this.scheduleReconnect();
      } else {
        this.setState(ConnectionState.DISCONNECTED);
      }
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer !== null) clearTimeout(this.reconnectTimer);
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.connect();
    }, this.config.reconnectDelayMs);
  }

  disconnect(): void {
    if (this.reconnectTimer !== null) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.unsubscribeSim) {
      this.unsubscribeSim();
      this.unsubscribeSim = null;
    }
    sensorSimulator.stop();

    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }

    this.setState(ConnectionState.DISCONNECTED);
  }

  sendRelayCommand(state: RelayState, reason: string, operator: string): boolean {
    const cmd: RelayCommand = {
      command: 'SET_RELAY',
      state,
      reason,
      operator,
      timestamp: Math.floor(Date.now() / 1000)
    };

    const jsonStr = JSON.stringify(cmd);
    this.addLog('TX', jsonStr);

    if (this.config.useSimulator) {
      sensorSimulator.handleCommand(cmd);
      return true;
    }

    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(jsonStr + '\n');
      return true;
    }

    this.addLog('SYS', 'Error al enviar comando: socket no conectado', true);
    return false;
  }
}

export const networkManager = new NetworkManager();
