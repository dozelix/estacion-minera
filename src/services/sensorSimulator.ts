import {
  RelayCommand,
  RelayState,
  TelemetryPacket,
  VibrationSeverity,
  calculateSeverity
} from '../types/telemetry';

/**
 * Built-in industrial MPU6050 sensor & motor simulator for testing and evaluation.
 * Exact parity with SensorSimulator.kt and tools/sensor_node_simulator.py.
 */
export class SensorSimulatorService {
  private timer: number | null = null;
  private currentRelayState: RelayState = RelayState.CLOSED;
  private anomalyMode: boolean = false;
  private stepCount: number = 0;
  private listeners: ((packet: TelemetryPacket) => void)[] = [];

  constructor() {}

  start(intervalMs: number = 500): void {
    this.stop();
    this.timer = window.setInterval(() => {
      this.stepCount++;
      const packet = this.generatePacket(this.stepCount);
      this.notify(packet);
    }, intervalMs);
  }

  stop(): void {
    if (this.timer !== null) {
      clearInterval(this.timer);
      this.timer = null;
    }
  }

  isRunning(): boolean {
    return this.timer !== null;
  }

  subscribe(listener: (packet: TelemetryPacket) => void): () => void {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  }

  private notify(packet: TelemetryPacket): void {
    for (const listener of this.listeners) {
      try {
        listener(packet);
      } catch (e) {
        console.error('Error notifying telemetry listener:', e);
      }
    }
  }

  handleCommand(command: RelayCommand): void {
    this.currentRelayState = command.state;
  }

  setAnomalyMode(enabled: boolean): void {
    this.anomalyMode = enabled;
  }

  isAnomalyMode(): boolean {
    return this.anomalyMode;
  }

  getRelayState(): RelayState {
    return this.currentRelayState;
  }

  generatePacket(t: number): TelemetryPacket {
    const isMotorRunning = this.currentRelayState === RelayState.CLOSED;

    let vibrationRms: number;
    let rpm: number;
    let temp: number;
    let ax: number, ay: number, az: number;
    let gx: number, gy: number, gz: number;

    if (!isMotorRunning) {
      // Motor off / tripped
      vibrationRms = Number((0.12 + (Math.random() * 0.08)).toFixed(2));
      rpm = 0;
      temp = Number((Math.max(24.0, 48.0 - (t % 20) * 0.4)).toFixed(1));
      ax = 0.02;
      ay = 0.03;
      az = 9.81;
      gx = 0.0;
      gy = 0.0;
      gz = 0.0;
    } else if (this.anomalyMode) {
      // Critical vibration ISO 10816-3 Zona D (> 7.1 mm/s)
      vibrationRms = Number((7.8 + (Math.sin(t * 0.5) * 1.6) + (Math.random() * 0.7)).toFixed(2));
      rpm = Math.floor(1420 + ((Math.random() - 0.5) * 80));
      temp = Number((Math.min(88.0, 68.0 + (t % 15) * 0.5)).toFixed(1));
      ax = Number((Math.sin(t * 0.8) * vibrationRms * 0.3).toFixed(2));
      ay = Number((Math.cos(t * 0.5) * vibrationRms * 0.35).toFixed(2));
      az = Number((9.81 + Math.sin(t * 1.1) * vibrationRms * 0.4).toFixed(2));
      gx = Number((Math.sin(t * 0.6) * 0.3).toFixed(2));
      gy = Number((Math.cos(t * 0.4) * 0.3).toFixed(2));
      gz = Number((Math.sin(t * 0.9) * 0.4).toFixed(2));
    } else {
      // Normal faena operation ISO 10816-3 Zona A/B (< 4.5 mm/s)
      vibrationRms = Number((2.3 + (Math.sin(t * 0.2) * 0.8) + ((Math.random() - 0.5) * 0.4)).toFixed(2));
      rpm = Math.floor(1485 + ((Math.random() - 0.5) * 16));
      temp = Number((49.0 + (Math.sin(t * 0.05) * 1.5)).toFixed(1));
      ax = Number((Math.sin(t * 0.8) * 0.4).toFixed(2));
      ay = Number((Math.sin(t * 0.5) * 0.5).toFixed(2));
      az = Number((9.81 + Math.sin(t * 1.2) * 0.6).toFixed(2));
      gx = Number((Math.sin(t * 0.3) * 0.04).toFixed(2));
      gy = Number((Math.cos(t * 0.2) * 0.04).toFixed(2));
      gz = Number((Math.sin(t * 0.5) * 0.05).toFixed(2));
    }

    const timestamp = Math.floor(Date.now() / 1000);
    const rawJson = JSON.stringify({
      device_id: 'MPU6050_MOTOR_01',
      timestamp,
      accel: { x: ax, y: ay, z: az },
      gyro: { x: gx, y: gy, z: gz },
      vibration_rms: vibrationRms,
      temperature_c: temp,
      rpm,
      relay_state: this.currentRelayState
    });

    return {
      deviceId: 'MPU6050_MOTOR_01',
      timestamp,
      accel: { x: ax, y: ay, z: az },
      gyro: { x: gx, y: gy, z: gz },
      vibrationRms,
      temperatureC: temp,
      rpm,
      relayState: this.currentRelayState,
      rawJson,
      severity: calculateSeverity(vibrationRms)
    };
  }
}

export const sensorSimulator = new SensorSimulatorService();
