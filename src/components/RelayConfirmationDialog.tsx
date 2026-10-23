import React, { useState } from 'react';
import { AlertTriangle, PowerOff, ShieldCheck, X } from 'lucide-react';
import { RelayState } from '../types/telemetry';

interface RelayConfirmationDialogProps {
  isOpen: boolean;
  targetState: RelayState;
  operatorName: string;
  onConfirm: (reason: string) => void;
  onCancel: () => void;
}

export const RelayConfirmationDialog: React.FC<RelayConfirmationDialogProps> = ({
  isOpen,
  targetState,
  operatorName,
  onConfirm,
  onCancel
}) => {
  const [reason, setReason] = useState(
    targetState === RelayState.OPEN
      ? 'PARADA DE EMERGENCIA - RIESGO DE SOBREVIBRACIÓN ISO 10816'
      : 'REARME MANUAL TRAS VERIFICACIÓN MECÁNICA'
  );

  if (!isOpen) return null;

  const isTrip = targetState === RelayState.OPEN;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-fadeIn">
      <div className={`flex flex-col w-full max-w-md bg-[#0e1622] rounded-2xl border shadow-2xl overflow-hidden ${
        isTrip ? 'border-red-600/60 shadow-red-950/40' : 'border-emerald-600/60 shadow-emerald-950/40'
      }`}>
        {/* Banner */}
        <div className={`flex items-center gap-3 px-5 py-4 ${
          isTrip ? 'bg-red-950/60 text-red-400 border-b border-red-900/60' : 'bg-emerald-950/60 text-emerald-400 border-b border-emerald-900/60'
        }`}>
          <div className={`p-2 rounded-xl ${isTrip ? 'bg-red-500/20' : 'bg-emerald-500/20'}`}>
            {isTrip ? <PowerOff className="w-6 h-6 text-red-400" /> : <ShieldCheck className="w-6 h-6 text-emerald-400" />}
          </div>
          <div>
            <h3 className="text-base font-bold tracking-tight text-white">
              {isTrip ? 'Confirmar Parada de Emergencia' : 'Confirmar Rearme de Circuito'}
            </h3>
            <p className="text-xs text-slate-300">
              {isTrip ? 'Apertura inmediata del relé de corte (GPIO 26)' : 'Cierre de relé y encendido de motor minero'}
            </p>
          </div>
        </div>

        {/* Content */}
        <div className="p-5 space-y-4">
          <div className="p-3 bg-[#141e2b] rounded-xl border border-slate-800 text-xs text-slate-300 space-y-1">
            <div className="flex justify-between">
              <span className="text-slate-400">Operador a cargo:</span>
              <strong className="text-white font-mono-code">{operatorName}</strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Comando TCP:</span>
              <strong className={`font-mono-code ${isTrip ? 'text-red-400' : 'text-emerald-400'}`}>
                SET_RELAY -&gt; {targetState}
              </strong>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-400">Activo físico:</span>
              <span className="text-white">Motor Molienda #01 (Chancado)</span>
            </div>
          </div>

          <div>
            <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider mb-1.5">
              Motivo Operacional / Registro de Auditoría
            </label>
            <textarea
              rows={2}
              value={reason}
              onChange={e => setReason(e.target.value)}
              className="w-full px-3 py-2 bg-[#080d14] border border-[#1f2b3b] rounded-lg text-xs text-white focus:outline-none focus:border-[#FF8F00] font-sans"
              placeholder="Ingrese justificación de maniobra..."
            />
          </div>

          {isTrip && (
            <div className="flex items-center gap-2 p-2.5 bg-red-950/30 border border-red-900/40 rounded-xl text-xs text-red-300">
              <AlertTriangle className="w-4 h-4 shrink-0 text-red-400" />
              <span>Esta acción cortará la alimentación eléctrica del motor de inmediato.</span>
            </div>
          )}
        </div>

        {/* Buttons */}
        <div className="flex items-center justify-end gap-2 px-5 py-3 bg-[#141d29] border-t border-[#1f2b3b]">
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 rounded-xl text-xs font-bold text-slate-300 hover:bg-slate-800 transition"
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={() => onConfirm(reason)}
            className={`flex items-center gap-1.5 px-5 py-2.5 rounded-xl text-xs font-bold text-white shadow-lg transition ${
              isTrip
                ? 'bg-red-600 hover:bg-red-500 shadow-red-950/50'
                : 'bg-emerald-600 hover:bg-emerald-500 shadow-emerald-950/50'
            }`}
          >
            {isTrip ? <PowerOff className="w-4 h-4" /> : <ShieldCheck className="w-4 h-4" />}
            {isTrip ? 'DISPARAR RELÉ' : 'REARMAR CIRCUITO'}
          </button>
        </div>
      </div>
    </div>
  );
};
