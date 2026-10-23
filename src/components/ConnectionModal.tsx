import React, { useState } from 'react';
import { Settings, X, Wifi, Cpu, RefreshCw, Server, AlertCircle } from 'lucide-react';
import { NetworkConfig } from '../services/tcpClient';

interface ConnectionModalProps {
  isOpen: boolean;
  onClose: () => void;
  config: NetworkConfig;
  onSave: (newConfig: NetworkConfig) => void;
  onReconnect: () => void;
}

export const ConnectionModal: React.FC<ConnectionModalProps> = ({
  isOpen,
  onClose,
  config,
  onSave,
  onReconnect
}) => {
  const [host, setHost] = useState(config.host);
  const [port, setPort] = useState(config.port.toString());
  const [autoReconnect, setAutoReconnect] = useState(config.autoReconnect);
  const [useSimulator, setUseSimulator] = useState(config.useSimulator);
  const [reconnectDelay, setReconnectDelay] = useState((config.reconnectDelayMs / 1000).toString());

  if (!isOpen) return null;

  const handleApply = () => {
    const newConfig: NetworkConfig = {
      host: host.trim(),
      port: parseInt(port, 10) || 8080,
      autoReconnect,
      useSimulator,
      reconnectDelayMs: (parseFloat(reconnectDelay) || 3) * 1000
    };
    onSave(newConfig);
    onReconnect();
    onClose();
  };

  const applyPreset = (presetHost: string, presetPort: number, sim: boolean) => {
    setHost(presetHost);
    setPort(presetPort.toString());
    setUseSimulator(sim);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div className="flex flex-col w-full max-w-lg bg-[#0c131c] border border-[#1f2b3b] rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-5 py-4 bg-[#141d29] border-b border-[#1f2b3b]">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-[#FF8F00]/20 rounded-lg text-[#FF8F00]">
              <Settings className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white tracking-wide">
                Configuración de Conexión IoT
              </h2>
              <p className="text-xs text-slate-400">
                Parámetros de enlace TCP / Simulador de nodo sensor
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-5 space-y-4 max-h-[75vh] overflow-y-auto">
          {/* Mode Selector */}
          <div>
            <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">
              Origen de Datos de Telemetría
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setUseSimulator(true)}
                className={`flex items-center gap-2.5 p-3 rounded-xl border text-left transition ${
                  useSimulator
                    ? 'bg-[#FF8F00]/15 border-[#FF8F00] text-white shadow-sm'
                    : 'bg-[#141d29] border-[#1f2b3b] text-slate-400 hover:border-slate-700'
                }`}
              >
                <Cpu className={`w-5 h-5 ${useSimulator ? 'text-[#FF8F00]' : 'text-slate-500'}`} />
                <div>
                  <div className="text-xs font-bold">Simulador Interno</div>
                  <div className="text-[10px] text-slate-400">Eval. autónoma (2 Hz)</div>
                </div>
              </button>

              <button
                type="button"
                onClick={() => setUseSimulator(false)}
                className={`flex items-center gap-2.5 p-3 rounded-xl border text-left transition ${
                  !useSimulator
                    ? 'bg-cyan-500/15 border-cyan-500 text-white shadow-sm'
                    : 'bg-[#141d29] border-[#1f2b3b] text-slate-400 hover:border-slate-700'
                }`}
              >
                <Wifi className={`w-5 h-5 ${!useSimulator ? 'text-cyan-400' : 'text-slate-500'}`} />
                <div>
                  <div className="text-xs font-bold">Socket TCP en Vivo</div>
                  <div className="text-[10px] text-slate-400">ESP32 / Python :8080</div>
                </div>
              </button>
            </div>
          </div>

          {/* Preset Profiles */}
          <div>
            <label className="block text-[11px] font-semibold text-slate-400 mb-1.5">
              Perfiles Rápidos de Faena:
            </label>
            <div className="flex flex-wrap gap-1.5">
              <button
                type="button"
                onClick={() => applyPreset('127.0.0.1', 8080, true)}
                className="px-2.5 py-1 text-[11px] rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 font-mono-code"
              >
                Simulador MPU6050
              </button>
              <button
                type="button"
                onClick={() => applyPreset('10.0.2.2', 8080, false)}
                className="px-2.5 py-1 text-[11px] rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 font-mono-code"
              >
                Emulador Android (10.0.2.2:8080)
              </button>
              <button
                type="button"
                onClick={() => applyPreset('192.168.4.1', 8080, false)}
                className="px-2.5 py-1 text-[11px] rounded bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700 font-mono-code"
              >
                ESP32 AP (192.168.4.1:8080)
              </button>
            </div>
          </div>

          {/* Host & Port inputs */}
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2">
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Dirección Host / IP
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={host}
                  onChange={e => setHost(e.target.value)}
                  placeholder="ej. 10.0.2.2 o 192.168.1.50"
                  disabled={useSimulator}
                  className="w-full px-3 py-2 bg-[#080d14] border border-[#1f2b3b] rounded-lg text-xs font-mono-code text-white focus:outline-none focus:border-[#FF8F00] disabled:opacity-50"
                />
              </div>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Puerto TCP
              </label>
              <input
                type="number"
                value={port}
                onChange={e => setPort(e.target.value)}
                placeholder="8080"
                disabled={useSimulator}
                className="w-full px-3 py-2 bg-[#080d14] border border-[#1f2b3b] rounded-lg text-xs font-mono-code text-white focus:outline-none focus:border-[#FF8F00] disabled:opacity-50"
              />
            </div>
          </div>

          {/* Reconnect Settings */}
          <div className="p-3 bg-[#141d29] rounded-xl border border-[#1f2b3b] space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <div className="text-xs font-bold text-white">Reconexión Automática</div>
                <div className="text-[11px] text-slate-400">Reintentar ante microcortes de red</div>
              </div>
              <input
                type="checkbox"
                checked={autoReconnect}
                onChange={e => setAutoReconnect(e.target.checked)}
                className="w-4 h-4 accent-[#FF8F00] cursor-pointer"
              />
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-[#1f2b3b]">
              <div className="text-xs text-slate-300">Intervalo de Reintento</div>
              <div className="flex items-center gap-1.5">
                <input
                  type="number"
                  step="0.5"
                  min="1"
                  max="30"
                  value={reconnectDelay}
                  onChange={e => setReconnectDelay(e.target.value)}
                  className="w-16 px-2 py-1 bg-[#080d14] border border-[#1f2b3b] rounded text-xs font-mono-code text-right text-white"
                />
                <span className="text-xs text-slate-400">seg</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 p-2.5 bg-blue-950/30 border border-blue-900/50 rounded-xl text-[11px] text-blue-300">
            <AlertCircle className="w-4 h-4 shrink-0 text-blue-400" />
            <span>
              La estación Android actúa como <strong>Cliente TCP</strong>; el ESP32 o el script Python actúa como <strong>Servidor :8080</strong>.
            </span>
          </div>
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 px-5 py-3 bg-[#141d29] border-t border-[#1f2b3b]">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg text-xs font-semibold text-slate-300 hover:bg-slate-800 transition"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleApply}
            className="flex items-center gap-1.5 px-5 py-2 rounded-lg text-xs font-bold bg-[#FF8F00] text-black hover:bg-[#ffa000] shadow-md transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Aplicar y Reconectar
          </button>
        </div>
      </div>
    </div>
  );
};
