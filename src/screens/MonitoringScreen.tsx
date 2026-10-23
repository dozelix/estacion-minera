import React, { useState, useEffect, useRef } from 'react';
import {
  Shield,
  Power,
  PowerOff,
  Zap,
  Activity,
  Gauge,
  Thermometer,
  Wifi,
  WifiOff,
  Terminal,
  Settings,
  LogOut,
  Type,
  AlertOctagon,
  AlertTriangle,
  CheckCircle2,
  Bell,
  RefreshCw,
  Compass,
  RotateCcw
} from 'lucide-react';
import {
  ConnectionState,
  RelayState,
  TelemetryPacket,
  TerminalLogEntry,
  UserProfile,
  VibrationSeverity
} from '../types/telemetry';
import { networkManager, NetworkConfig } from '../services/tcpClient';
import { sensorSimulator } from '../services/sensorSimulator';
import { VibrationOscilloscope } from '../components/VibrationOscilloscope';
import { StatusBadge } from '../components/StatusBadge';
import { TerminalModal } from '../components/TerminalModal';
import { ConnectionModal } from '../components/ConnectionModal';
import { RelayConfirmationDialog } from '../components/RelayConfirmationDialog';
import { IndustrialColors } from '../theme/tokens';

interface MonitoringScreenProps {
  user: UserProfile;
  onLogout: () => void;
  isDark?: boolean;
}

export const MonitoringScreen: React.FC<MonitoringScreenProps> = ({
  user,
  onLogout,
  isDark = true
}) => {
  const [packet, setPacket] = useState<TelemetryPacket | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>(ConnectionState.DISCONNECTED);
  const [vibrationHistory, setVibrationHistory] = useState<number[]>([]);
  const [logs, setLogs] = useState<TerminalLogEntry[]>([]);
  const [isLargeFont, setIsLargeFont] = useState(false);
  const [isForegroundService, setIsForegroundService] = useState(true);
  const [isAnomalyActive, setIsAnomalyActive] = useState(false);
  const [autoTripEnabled, setAutoTripEnabled] = useState(true);

  // Dialogs
  const [isTerminalOpen, setIsTerminalOpen] = useState(false);
  const [isConnectionOpen, setIsConnectionOpen] = useState(false);
  const [confirmDialog, setConfirmDialog] = useState<{
    isOpen: boolean;
    targetState: RelayState;
  }>({
    isOpen: false,
    targetState: RelayState.OPEN
  });

  const consecutiveCriticalCount = useRef(0);

  // Initialize network and event listeners
  useEffect(() => {
    const unsubState = networkManager.onStateChange(setConnectionState);
    const unsubPacket = networkManager.onPacket(newPacket => {
      setPacket(newPacket);
      setVibrationHistory(prev => {
        const next = [...prev, newPacket.vibrationRms];
        return next.slice(-40); // Keep last 40 samples
      });

      // Automatic safety trip logic: ISO 10816-3 Zona D (> 7.1 mm/s for 2 samples)
      if (autoTripEnabled && newPacket.vibrationRms >= 7.1 && newPacket.relayState === RelayState.CLOSED) {
        consecutiveCriticalCount.current += 1;
        if (consecutiveCriticalCount.current >= 2) {
          consecutiveCriticalCount.current = 0;
          networkManager.sendRelayCommand(
            RelayState.OPEN,
            'DISPARO PREVENTIVO AUTOMÁTICO (Vibración >= 7.1 mm/s según ISO 10816-3)',
            'SISTEMA AUTOMÁTICO DE SEGURIDAD'
          );
        }
      } else {
        consecutiveCriticalCount.current = 0;
      }
    });

    const unsubLog = networkManager.onLog(entry => {
      setLogs(prev => [...prev.slice(-150), entry]);
    });

    // Start connection
    networkManager.connect();

    return () => {
      unsubState();
      unsubPacket();
      unsubLog();
      networkManager.disconnect();
    };
  }, [autoTripEnabled]);

  const handleRelayClick = (targetState: RelayState) => {
    setConfirmDialog({
      isOpen: true,
      targetState
    });
  };

  const handleConfirmRelay = (reason: string) => {
    networkManager.sendRelayCommand(confirmDialog.targetState, reason, user.name);
    setConfirmDialog({ isOpen: false, targetState: RelayState.OPEN });
  };

  const toggleAnomaly = () => {
    const next = !isAnomalyActive;
    setIsAnomalyActive(next);
    sensorSimulator.setAnomalyMode(next);
  };

  const isRelayOpen = packet?.relayState === RelayState.OPEN;
  const currentRms = packet ? packet.vibrationRms : null;

  return (
    <div className="min-h-full w-full flex flex-col bg-[#0B1017] text-slate-100 select-none pb-12">
      {/* Top App Bar */}
      <header className="sticky top-0 z-30 bg-[#141D29]/95 backdrop-blur border-b border-[#1F2B3B] px-4 py-3 shadow-md">
        <div className="max-w-5xl mx-auto flex items-center justify-between gap-3">
          {/* Title & Role Badge */}
          <div className="flex flex-col min-w-0">
            <div className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-lg bg-[#FF8F00]/15 border border-[#FF8F00]/30 w-fit">
              <span className="w-1.5 h-1.5 rounded-full bg-[#FF8F00] animate-ping" />
              <span className="font-mono-code font-bold text-[10px] sm:text-[11px] text-[#FF8F00] tracking-wider uppercase">
                ROL: ESTACIÓN DE SUPERVISIÓN
              </span>
            </div>
            <h1 className={`font-bold font-industrial tracking-tight text-white truncate ${isLargeFont ? 'text-lg sm:text-xl' : 'text-base sm:text-lg'}`}>
              Motor Minero #01 (Molienda SAG)
            </h1>
          </div>

          {/* Action icons (min 48dp touch target) */}
          <div className="flex items-center gap-1 shrink-0">
            <button
              onClick={() => setIsLargeFont(!isLargeFont)}
              title={isLargeFont ? 'Texto normal' : 'Texto grande (Modo Faena/Guantes)'}
              className={`p-2.5 rounded-xl transition min-w-[48px] min-h-[48px] flex items-center justify-center ${
                isLargeFont ? 'bg-[#FF8F00] text-black shadow-md' : 'text-slate-400 hover:text-white hover:bg-slate-800'
              }`}
            >
              <Type className="w-5 h-5" />
            </button>

            <button
              onClick={() => setIsTerminalOpen(true)}
              title="Consola de Tramas JSON"
              className="p-2.5 rounded-xl text-cyan-400 hover:bg-cyan-950/40 hover:text-cyan-300 transition min-w-[48px] min-h-[48px] flex items-center justify-center"
            >
              <Terminal className="w-5 h-5" />
            </button>

            <button
              onClick={() => setIsConnectionOpen(true)}
              title="Configuración de Red"
              className="p-2.5 rounded-xl text-slate-300 hover:bg-slate-800 hover:text-white transition min-w-[48px] min-h-[48px] flex items-center justify-center"
            >
              <Settings className="w-5 h-5" />
            </button>

            <button
              onClick={onLogout}
              title="Cerrar Sesión"
              className="p-2.5 rounded-xl text-rose-400 hover:bg-rose-950/40 hover:text-rose-300 transition min-w-[48px] min-h-[48px] flex items-center justify-center"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-5xl mx-auto w-full p-4 sm:p-6 space-y-4">
        {/* Banner: Operador activo & Estado de Enlace */}
        <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-[18px] shadow-lg space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            {/* Operator info */}
            <div className="min-w-0">
              <div className="text-sm sm:text-base font-extrabold text-white truncate">
                {user.name}
              </div>
              <div className="text-xs text-slate-400 flex items-center gap-1.5 truncate">
                <span className="w-1.5 h-1.5 rounded-full bg-slate-500 shrink-0" />
                <span className="truncate">{user.role}</span>
              </div>
            </div>

            {/* Connection Badge */}
            <div className="flex items-center gap-2 shrink-0">
              <div
                className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-xl border text-xs font-mono-code font-bold ${
                  connectionState === ConnectionState.CONNECTED
                    ? 'bg-emerald-950/50 border-emerald-500 text-emerald-400'
                    : connectionState === ConnectionState.SIMULATING
                    ? 'bg-purple-950/50 border-purple-500 text-purple-300'
                    : connectionState === ConnectionState.CONNECTING
                    ? 'bg-cyan-950/50 border-cyan-500 text-cyan-300'
                    : connectionState === ConnectionState.RECONNECTING
                    ? 'bg-amber-950/50 border-amber-500 text-amber-300'
                    : 'bg-rose-950/50 border-rose-500 text-rose-300'
                }`}
              >
                <span className="relative flex h-2 w-2 shrink-0">
                  <span
                    className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${
                      connectionState === ConnectionState.CONNECTED || connectionState === ConnectionState.SIMULATING
                        ? 'bg-emerald-400'
                        : 'bg-amber-400'
                    }`}
                  />
                  <span
                    className={`relative inline-flex rounded-full h-2 w-2 ${
                      connectionState === ConnectionState.CONNECTED || connectionState === ConnectionState.SIMULATING
                        ? 'bg-emerald-500'
                        : 'bg-rose-500'
                    }`}
                  />
                </span>
                <span>
                  {connectionState === ConnectionState.CONNECTED && 'ENLACE TCP ACTIVO (8080)'}
                  {connectionState === ConnectionState.SIMULATING && 'SIMULADOR MPU6050 (2 Hz)'}
                  {connectionState === ConnectionState.CONNECTING && 'CONECTANDO AL NODO...'}
                  {connectionState === ConnectionState.RECONNECTING && 'REINTENTANDO CONEXIÓN...'}
                  {connectionState === ConnectionState.DISCONNECTED && 'SIN ENLACE TCP'}
                </span>
              </div>
            </div>
          </div>

          {/* FIX 3: Servicio con Pantalla Apagada — Con min-width 0, sin desborde */}
          <div className="pt-3 border-t border-[#1F2B3B] flex items-center justify-between gap-3 w-full">
            <div className="flex-1 min-w-0 flex items-center gap-2 pr-2">
              <Bell className={`w-4 h-4 shrink-0 ${isForegroundService ? 'text-emerald-400' : 'text-slate-500'}`} />
              <div className="flex-1 min-w-0">
                <div className="text-xs font-semibold text-slate-200 line-clamp-2">
                  Servicio con Pantalla Apagada
                </div>
                <div className="text-[11px] text-slate-400 line-clamp-2">
                  Tolerancia Doze / Notificaciones Críticas en segundo plano
                </div>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer shrink-0 w-[52px] h-[32px]">
              <input
                type="checkbox"
                checked={isForegroundService}
                onChange={e => setIsForegroundService(e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-[52px] h-[30px] bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-[22px] peer-checked:after:border-white after:content-[''] after:absolute after:top-[4px] after:left-[4px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-[22px] after:w-[22px] after:transition-all peer-checked:bg-emerald-600"></div>
            </label>
          </div>
        </div>

        {/* RELAY CONTROL BOARD */}
        <div
          className={`rounded-2xl p-[18px] border-2 transition-all shadow-xl space-y-4 ${
            isRelayOpen
              ? 'bg-rose-950/20 border-rose-600 shadow-rose-950/30'
              : 'bg-[#141D29] border-emerald-600/60 shadow-emerald-950/20'
          }`}
        >
          {/* Header */}
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-2.5 min-w-0">
              <div
                className={`p-2 rounded-xl shrink-0 ${
                  isRelayOpen ? 'bg-rose-600/20 text-rose-400' : 'bg-emerald-600/20 text-emerald-400'
                }`}
              >
                <Zap className="w-5 h-5" />
              </div>
              <div className="min-w-0">
                <div className="font-mono-code font-bold text-xs uppercase tracking-wider text-slate-400 truncate">
                  RELÉ PRINCIPAL
                </div>
                <div
                  className={`text-base sm:text-lg font-extrabold tracking-tight truncate ${
                    isRelayOpen ? 'text-rose-400' : 'text-emerald-400'
                  }`}
                >
                  {isRelayOpen ? 'ABIERTO (DISPARADO)' : 'CERRADO (OPERATIVO)'}
                </div>
              </div>
            </div>

            <div
              className={`px-3 py-1 rounded-full text-xs font-mono-code font-bold border shrink-0 ${
                isRelayOpen
                  ? 'bg-rose-950/50 border-rose-500 text-rose-300'
                  : 'bg-emerald-950/50 border-emerald-500 text-emerald-300'
              }`}
            >
              {isRelayOpen ? 'CIRCUITO ABIERTO' : 'CIRCUITO CERRADO'}
            </div>
          </div>

          <p className="text-xs text-slate-400">
            {isRelayOpen
              ? 'Alimentación eléctrica cortada por seguridad. Motor desenergizado y detenido.'
              : 'Circuito de potencia energizado. Motor en operación nominal continua.'}
          </p>

          {/* FIX 1: Action Buttons: Disparo Preventivo / Rearmar Relé (Simetría e intrinsic-height) */}
          <div className="flex items-stretch gap-[12px] w-full min-h-[50px]">
            <button
              type="button"
              onClick={() => handleRelayClick(RelayState.OPEN)}
              disabled={isRelayOpen}
              className={`flex-1 w-0 h-full min-h-[50px] p-2.5 rounded-xl font-bold flex items-center justify-center transition active:scale-[0.98] shadow-md ${
                isRelayOpen
                  ? 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed opacity-60'
                  : 'bg-[#FF8F00] hover:bg-[#FFA000] text-black shadow-[#FF8F00]/20'
              }`}
            >
              <div className="flex items-center justify-center gap-[6px] w-full">
                <PowerOff className="w-5 h-5 shrink-0" />
                <span className="text-[13px] font-bold line-clamp-2 text-center leading-tight">
                  Disparo Preventivo
                </span>
              </div>
            </button>

            <button
              type="button"
              onClick={() => handleRelayClick(RelayState.CLOSED)}
              disabled={!isRelayOpen}
              className={`flex-1 w-0 h-full min-h-[50px] p-2.5 rounded-xl font-bold flex items-center justify-center transition active:scale-[0.98] shadow-md ${
                !isRelayOpen
                  ? 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed opacity-60'
                  : 'bg-emerald-600 hover:bg-emerald-500 text-white shadow-emerald-950/50'
              }`}
            >
              <div className="flex items-center justify-center gap-[6px] w-full">
                <RotateCcw className="w-5 h-5 shrink-0" />
                <span className="text-[13px] font-bold line-clamp-2 text-center leading-tight">
                  Rearmar Relé
                </span>
              </div>
            </button>
          </div>

          {/* Emergency Stop Button */}
          <button
            type="button"
            onClick={() => handleRelayClick(RelayState.OPEN)}
            disabled={isRelayOpen}
            className={`w-full min-h-[50px] px-4 py-3 rounded-xl font-black text-xs sm:text-[13px] tracking-wider uppercase flex items-center justify-center gap-2 shadow-lg transition active:scale-[0.98] ${
              isRelayOpen
                ? 'bg-slate-800 text-slate-500 border border-slate-700 cursor-not-allowed opacity-60'
                : 'bg-rose-600 hover:bg-rose-500 text-white shadow-rose-950/50'
            }`}
          >
            <AlertTriangle className="w-5 h-5 shrink-0 text-white" />
            <span className="truncate">PARADA DE EMERGENCIA GENERAL</span>
          </button>

          {/* FIX 2: Protección Auto-Trip Row — Con flex-1 min-w-0, sin desborde */}
          <div className="pt-3 border-t border-[#1F2B3B] flex items-center justify-between gap-3 w-full">
            <div className="flex-1 min-w-0 pr-2">
              <div className="text-xs font-semibold text-white line-clamp-2">
                Protección Auto-Trip (Desconexión Automática)
              </div>
              <div className="text-[11px] text-slate-400 line-clamp-2 mt-0.5">
                Apertura automática de relé si vibración crítica &gt;= 7.1 mm/s
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer shrink-0 w-[52px] h-[32px]">
              <input
                type="checkbox"
                checked={autoTripEnabled}
                onChange={e => setAutoTripEnabled(e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-[52px] h-[30px] bg-slate-700 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-[22px] peer-checked:after:border-white after:content-[''] after:absolute after:top-[4px] after:left-[4px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-[22px] after:w-[22px] after:transition-all peer-checked:bg-[#FF8F00]"></div>
            </label>
          </div>
        </div>

        {/* VIBRATION MONITOR CARD (ISO 10816-3) */}
        <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-[18px] shadow-xl space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <Activity className="w-5 h-5 text-[#FF8F00] shrink-0" />
                <h2 className={`font-bold font-industrial tracking-wide text-white truncate ${isLargeFont ? 'text-lg' : 'text-base'}`}>
                  Vibración Mecánica RMS (Norma ISO 10816-3)
                </h2>
              </div>
              <p className="text-xs text-slate-400 truncate">
                Monitoreo continuo de desbalanceo y falla de rodamiento en tiempo real
              </p>
            </div>

            <div className="flex items-center gap-2 shrink-0">
              <StatusBadge
                severity={packet ? packet.severity : VibrationSeverity.NORMAL}
                isLargeFont={isLargeFont}
              />
              <button
                type="button"
                onClick={toggleAnomaly}
                className={`min-h-[48px] px-3 py-1.5 rounded-xl text-xs font-bold font-mono-code border transition flex items-center gap-1.5 ${
                  isAnomalyActive
                    ? 'bg-rose-600/30 text-rose-300 border-rose-500 animate-pulse'
                    : 'bg-slate-800 text-slate-300 border-slate-700 hover:border-amber-500'
                }`}
              >
                <AlertTriangle className="w-3.5 h-3.5 text-[#FF8F00] shrink-0" />
                <span className="truncate">{isAnomalyActive ? 'NORMALIZAR' : 'FALLA CRÍTICA'}</span>
              </button>
            </div>
          </div>

          {/* Oscilloscope Waveform Canvas (FIX 4 aplicado) */}
          <VibrationOscilloscope
            vibrationHistory={vibrationHistory}
            currentRms={currentRms}
            height={200}
          />

          {/* ISO 10816-3 Zones Reference Legend */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
            <div className="p-2.5 rounded-xl bg-[#0B1017] border border-emerald-900/40 text-xs">
              <div className="flex items-center gap-1.5 text-emerald-400 font-bold font-mono-code truncate">
                <CheckCircle2 className="w-3.5 h-3.5 shrink-0" />
                <span>ZONA A/B: NORMAL (&lt; 4.5 mm/s)</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5 truncate">Operación sin restricciones</p>
            </div>

            <div className="p-2.5 rounded-xl bg-[#0B1017] border border-amber-900/40 text-xs">
              <div className="flex items-center gap-1.5 text-amber-400 font-bold font-mono-code truncate">
                <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                <span>ZONA C: ALERTA (4.5 - 7.1 mm/s)</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5 truncate">Operación temporal admisible</p>
            </div>

            <div className="p-2.5 rounded-xl bg-[#0B1017] border border-red-900/40 text-xs">
              <div className="flex items-center gap-1.5 text-red-400 font-bold font-mono-code truncate">
                <AlertOctagon className="w-3.5 h-3.5 shrink-0" />
                <span>ZONA D: CRÍTICA (&gt; 7.1 mm/s)</span>
              </div>
              <p className="text-[11px] text-slate-400 mt-0.5 truncate">Disparo preventivo obligatorio</p>
            </div>
          </div>
        </div>

        {/* MOTOR TELEMETRY METRICS GRID */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {/* Temperature Card */}
          <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5 truncate">
                <Thermometer className="w-4 h-4 text-cyan-400 shrink-0" /> Temperatura Devanado
              </span>
              <span className="text-[11px] font-mono-code px-2 py-0.5 rounded-lg bg-slate-800 text-slate-300 shrink-0">
                LÍMITE 85°C
              </span>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold font-mono-code text-white">
                {packet ? packet.temperatureC.toFixed(1) : '--.-'}
              </span>
              <span className="text-sm font-bold text-slate-400">°C</span>
            </div>
            <div className="w-full bg-[#0B1017] rounded-full h-2 overflow-hidden">
              <div
                className={`h-full transition-all duration-300 ${
                  (packet?.temperatureC || 0) > 75
                    ? 'bg-rose-500'
                    : (packet?.temperatureC || 0) > 60
                    ? 'bg-amber-400'
                    : 'bg-emerald-400'
                }`}
                style={{ width: `${Math.min(100, ((packet?.temperatureC || 0) / 90) * 100)}%` }}
              />
            </div>
          </div>

          {/* RPM Card */}
          <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-4 space-y-2">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5 truncate">
                <Gauge className="w-4 h-4 text-amber-400 shrink-0" /> Velocidad Rotacional
              </span>
              <span className="text-[11px] font-mono-code px-2 py-0.5 rounded-lg bg-slate-800 text-slate-300 shrink-0">
                NOMINAL 1485
              </span>
            </div>
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-extrabold font-mono-code text-white">
                {packet ? packet.rpm : 0}
              </span>
              <span className="text-sm font-bold text-slate-400">RPM</span>
            </div>
            <div className="w-full bg-[#0B1017] rounded-full h-2 overflow-hidden">
              <div
                className="h-full bg-cyan-400 transition-all duration-300"
                style={{ width: `${Math.min(100, ((packet?.rpm || 0) / 1600) * 100)}%` }}
              />
            </div>
          </div>

          {/* MPU6050 3-Axis Telemetry Card */}
          <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-4 space-y-2 sm:col-span-2 lg:col-span-1">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider flex items-center gap-1.5 truncate">
                <Compass className="w-4 h-4 text-purple-400 shrink-0" /> MPU-6050 Acelerometría
              </span>
              <span className="text-[10px] font-mono-code text-purple-300 bg-purple-950/60 px-2 py-0.5 rounded-lg border border-purple-800/40 shrink-0">
                I2C 0x68
              </span>
            </div>

            <div className="grid grid-cols-3 gap-1.5 text-center font-mono-code pt-1">
              <div className="p-1.5 bg-[#0B1017] rounded-lg border border-slate-800">
                <div className="text-[10px] text-slate-500 font-bold">X (g)</div>
                <div className="text-xs font-extrabold text-cyan-300">
                  {packet ? packet.accel.x.toFixed(2) : '0.00'}
                </div>
              </div>
              <div className="p-1.5 bg-[#0B1017] rounded-lg border border-slate-800">
                <div className="text-[10px] text-slate-500 font-bold">Y (g)</div>
                <div className="text-xs font-extrabold text-cyan-300">
                  {packet ? packet.accel.y.toFixed(2) : '0.00'}
                </div>
              </div>
              <div className="p-1.5 bg-[#0B1017] rounded-lg border border-slate-800">
                <div className="text-[10px] text-slate-500 font-bold">Z (g)</div>
                <div className="text-xs font-extrabold text-cyan-300">
                  {packet ? packet.accel.z.toFixed(2) : '9.81'}
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer info bar */}
        <div className="p-3 bg-[#0c131c] rounded-xl border border-[#1f2b3b] flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400 font-mono-code">
          <div className="flex items-center gap-2">
            <span className="text-[#FF8F00] font-bold">DISPOSITIVO:</span>
            <span>{packet ? packet.deviceId : 'MPU6050_MOTOR_01'}</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-slate-500">ÚLTIMO PAQUETE:</span>
            <span>{packet ? new Date(packet.timestamp * 1000).toLocaleTimeString() : '--:--:--'}</span>
          </div>
        </div>
      </main>

      {/* Terminal Modal */}
      <TerminalModal
        isOpen={isTerminalOpen}
        onClose={() => setIsTerminalOpen(false)}
        logs={logs}
        onClearLogs={() => setLogs([])}
      />

      {/* Connection Modal */}
      <ConnectionModal
        isOpen={isConnectionOpen}
        onClose={() => setIsConnectionOpen(false)}
        config={networkManager.getConfig()}
        onSave={cfg => networkManager.saveConfig(cfg)}
        onReconnect={() => networkManager.connect()}
      />

      {/* Relay Safety Confirmation Dialog */}
      <RelayConfirmationDialog
        isOpen={confirmDialog.isOpen}
        targetState={confirmDialog.targetState}
        operatorName={user.name}
        onConfirm={handleConfirmRelay}
        onCancel={() => setConfirmDialog({ isOpen: false, targetState: RelayState.OPEN })}
      />
    </div>
  );
};
