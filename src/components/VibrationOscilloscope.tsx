import React, { useEffect, useRef } from 'react';
import { IndustrialColors } from '../theme/tokens';

interface VibrationOscilloscopeProps {
  vibrationHistory: number[];
  currentRms: number | null;
  height?: number;
}

const MAX_ESCALA = 10;
const UMBRAL_ALERTA = 4.5;
const UMBRAL_CRITICO = 7.1;

export const VibrationOscilloscope: React.FC<VibrationOscilloscopeProps> = ({
  vibrationHistory,
  currentRms,
  height = 180
}) => {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const hasData = vibrationHistory.length > 0 && currentRms !== null;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Handle high DPI
    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    const width = rect.width;
    const h = rect.height;

    // Clear background (Panel SIEMPRE oscuro HMI #0A0E14)
    ctx.fillStyle = IndustrialColors.scopePanel;
    ctx.fillRect(0, 0, width, h);

    const getY = (val: number) => {
      const clamped = Math.max(0, Math.min(MAX_ESCALA, val));
      return h - (clamped / MAX_ESCALA) * h;
    };

    const alertY = getY(UMBRAL_ALERTA);
    const criticalY = getY(UMBRAL_CRITICO);

    // Grid lines
    ctx.strokeStyle = '#16202C';
    ctx.lineWidth = 1;
    for (let i = 1; i < MAX_ESCALA; i += 2) {
      const y = getY(i);
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // Threshold line: Alerta (4.5 mm/s)
    ctx.strokeStyle = 'rgba(255, 214, 0, 0.45)';
    ctx.setLineDash([4, 4]);
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, alertY);
    ctx.lineTo(width, alertY);
    ctx.stroke();

    // Threshold line: Crítica (7.1 mm/s)
    ctx.strokeStyle = 'rgba(255, 61, 0, 0.55)';
    ctx.setLineDash([4, 4]);
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, criticalY);
    ctx.lineTo(width, criticalY);
    ctx.stroke();
    ctx.setLineDash([]); // Reset dash

    // Draw waveform only if we have at least 2 samples
    if (vibrationHistory.length >= 2 && currentRms !== null) {
      const stepX = width / Math.max(1, vibrationHistory.length - 1);

      let lineColor = IndustrialColors.green;
      if (currentRms >= UMBRAL_CRITICO) {
        lineColor = IndustrialColors.red;
      } else if (currentRms >= UMBRAL_ALERTA) {
        lineColor = IndustrialColors.yellow;
      }

      // Fill path gradient under curve
      const gradient = ctx.createLinearGradient(0, 0, 0, h);
      gradient.addColorStop(0, `${lineColor}55`);
      gradient.addColorStop(1, 'rgba(0,0,0,0)');

      ctx.beginPath();
      ctx.moveTo(0, h);

      vibrationHistory.forEach((val, idx) => {
        const x = idx * stepX;
        const y = getY(val);
        ctx.lineTo(x, y);
      });

      ctx.lineTo((vibrationHistory.length - 1) * stepX, h);
      ctx.closePath();
      ctx.fillStyle = gradient;
      ctx.fill();

      // Stroke line
      ctx.beginPath();
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      ctx.lineJoin = 'round';
      ctx.strokeStyle = lineColor;

      vibrationHistory.forEach((val, idx) => {
        const x = idx * stepX;
        const y = getY(val);
        if (idx === 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      });
      ctx.stroke();

      // Last sample indicator
      const lastX = (vibrationHistory.length - 1) * stepX;
      const lastY = getY(vibrationHistory[vibrationHistory.length - 1]);

      ctx.beginPath();
      ctx.arc(lastX, lastY, 5, 0, Math.PI * 2);
      ctx.fillStyle = '#FFFFFF';
      ctx.fill();

      ctx.beginPath();
      ctx.arc(lastX, lastY, 3, 0, Math.PI * 2);
      ctx.fillStyle = lineColor;
      ctx.fill();
    }
  }, [vibrationHistory, currentRms, height]);

  // Aligned legends formula matching Kotlin VibrationWaveform:
  // top = 10px + (1 - valor/10) * plotHeight - 6px
  const plotHeight = Math.max(20, height - 20);
  const criticalTopPx = 10 + (1 - UMBRAL_CRITICO / MAX_ESCALA) * plotHeight - 6;
  const alertTopPx = 10 + (1 - UMBRAL_ALERTA / MAX_ESCALA) * plotHeight - 6;

  let rmsColor = IndustrialColors.scopeMarker;
  if (currentRms !== null) {
    if (currentRms >= UMBRAL_CRITICO) rmsColor = IndustrialColors.red;
    else if (currentRms >= UMBRAL_ALERTA) rmsColor = IndustrialColors.yellow;
    else rmsColor = IndustrialColors.green;
  }

  return (
    <div
      className="relative w-full rounded-xl overflow-hidden border border-[#1f2b3b]"
      style={{ height: `${height}px`, backgroundColor: IndustrialColors.scopePanel }}
    >
      <canvas
        ref={canvasRef}
        className="w-full h-full block"
      />

      {/* Threshold Labels — ONLY SHOWN WHEN DATA IS PRESENT (FIX 4) */}
      {hasData ? (
        <>
          <div
            className="absolute left-2.5 pointer-events-none font-mono-code text-[10px] font-bold tracking-wider"
            style={{ top: `${criticalTopPx}px`, color: IndustrialColors.red }}
          >
            CRÍTICO (7.1 mm/s)
          </div>

          <div
            className="absolute left-2.5 pointer-events-none font-mono-code text-[10px] font-bold tracking-wider"
            style={{ top: `${alertTopPx}px`, color: IndustrialColors.yellow }}
          >
            ALERTA (4.5 mm/s)
          </div>

          <div
            className="absolute left-2.5 bottom-2 pointer-events-none font-mono-code text-[10px] font-bold tracking-wider"
            style={{ color: IndustrialColors.green }}
          >
            RANGO NORMAL (&lt;4.5)
          </div>
        </>
      ) : (
        /* Empty State: Only the centered muted message with horizontal padding */
        <div className="absolute inset-0 flex items-center justify-center px-4 pointer-events-none text-center">
          <span className="font-mono-code text-xs text-[#64748B] tracking-wider animate-pulse max-w-xs line-clamp-2 px-2">
            ESPERANDO RECEPCIÓN DE TELEMETRÍA...
          </span>
        </div>
      )}

      {/* RMS Value Chip — Always Top Right in both states */}
      <div
        className="absolute top-2 right-2 px-2.5 py-0.5 rounded font-mono-code font-bold text-sm tracking-tight border border-[#233245]"
        style={{
          backgroundColor: IndustrialColors.scopeChip,
          color: rmsColor
        }}
      >
        {currentRms !== null ? `${currentRms.toFixed(2)} mm/s` : '-- mm/s'}
      </div>
    </div>
  );
};
