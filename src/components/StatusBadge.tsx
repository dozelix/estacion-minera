import React from 'react';
import { CheckCircle2, AlertTriangle, AlertOctagon, HelpCircle, Activity } from 'lucide-react';
import { VibrationSeverity } from '../types/telemetry';
import { IndustrialColors } from '../theme/tokens';

interface StatusBadgeProps {
  severity: VibrationSeverity;
  label?: string;
  isLargeFont?: boolean;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({
  severity,
  label,
  isLargeFont = false
}) => {
  let bg = 'rgba(0, 230, 118, 0.15)';
  let border = 'rgba(0, 230, 118, 0.4)';
  let color = IndustrialColors.green;
  let text = label || 'NORMAL (ZONA A/B)';
  let Icon = CheckCircle2;

  if (severity === VibrationSeverity.ALERT) {
    bg = 'rgba(255, 214, 0, 0.15)';
    border = 'rgba(255, 214, 0, 0.5)';
    color = IndustrialColors.yellow;
    text = label || 'ALERTA (ZONA C)';
    Icon = AlertTriangle;
  } else if (severity === VibrationSeverity.CRITICAL) {
    bg = 'rgba(255, 61, 0, 0.2)';
    border = 'rgba(255, 61, 0, 0.6)';
    color = IndustrialColors.red;
    text = label || 'CRÍTICO (ZONA D)';
    Icon = AlertOctagon;
  }

  return (
    <div
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full font-bold uppercase tracking-wider border font-mono-code ${
        isLargeFont ? 'text-xs' : 'text-[11px]'
      }`}
      style={{
        backgroundColor: bg,
        borderColor: border,
        color
      }}
    >
      <Icon className={isLargeFont ? 'w-4 h-4' : 'w-3.5 h-3.5'} />
      <span>{text}</span>
    </div>
  );
};
