/**
 * Telemetry and network contracts for Estación Minera (TI3042 ES2).
 * Direct parity with TelemetryModels.kt and sensor_node_simulator.py.
 */

export enum ConnectionState {
  DISCONNECTED = 'DISCONNECTED',
  CONNECTING = 'CONNECTING',
  CONNECTED = 'CONNECTED',
  RECONNECTING = 'RECONNECTING',
  SIMULATING = 'SIMULATING'
}

export enum RelayState {
  CLOSED = 'CLOSED', // Normal operational state (circuit closed, motor powered)
  OPEN = 'OPEN'      // Tripped / de-energized (circuit open, motor stopped)
}

export enum VibrationSeverity {
  NORMAL = 'NORMAL',     // < 4.5 mm/s (Zona A/B ISO 10816-3)
  ALERT = 'ALERT',       // 4.5 - 7.1 mm/s (Zona C ISO 10816-3)
  CRITICAL = 'CRITICAL'  // > 7.1 mm/s (Zona D ISO 10816-3)
}

export interface Vector3D {
  x: number;
  y: number;
  z: number;
}

export interface TelemetryPacket {
  deviceId: string;
  timestamp: number;
  accel: Vector3D;
  gyro: Vector3D;
  vibrationRms: number;
  temperatureC: number;
  rpm: number;
  relayState: RelayState;
  rawJson?: string;
  severity: VibrationSeverity;
}

export interface RelayCommand {
  command: 'SET_RELAY' | 'RELAY_CONTROL';
  state: RelayState;
  reason: string;
  operator: string;
  timestamp: number;
}

export interface UserProfile {
  email: string;
  name: string;
  role: string;
}

export interface TerminalLogEntry {
  id: string;
  direction: 'RX' | 'TX' | 'SYS';
  timestamp: string;
  payload: string;
  isError?: boolean;
}

export function calculateSeverity(vibrationRms: number): VibrationSeverity {
  if (vibrationRms >= 7.1) return VibrationSeverity.CRITICAL;
  if (vibrationRms >= 4.5) return VibrationSeverity.ALERT;
  return VibrationSeverity.NORMAL;
}
