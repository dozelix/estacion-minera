/**
 * Design tokens and industrial theme colors.
 * Strict parity with Color.kt, Dimens.kt, and Theme.kt.
 */

export const IndustrialColors = {
  // Brand accents
  amber: '#FF8F00',        // Main operational amber
  amberLight: '#FFB74D',   // Amber for dark containers
  amberGlow: 'rgba(255, 143, 0, 0.25)',

  cyan: '#00B0FF',         // Terminal & telemetry accent
  green: '#00E676',        // Success accent
  red: '#FF3D00',          // Emergency & critical accent
  yellow: '#FFD600',       // Warning accent

  // Dark surfaces
  darkBg: '#0B1017',
  darkSurface: '#141D29',
  darkSurfaceVariant: '#1F2B3B',
  darkCardBorder: '#2A3B4F',

  // Light surfaces
  lightBg: '#F1F5F9',
  lightSurface: '#FFFFFF',
  lightSurfaceVariant: '#E2E8F0',
  lightCardBorder: '#CBD5E1',

  // Instruments (ALWAYS DARK HMI)
  scopePanel: '#0A0E14',
  scopeFrameworkText: '#64748B',
  scopeChip: '#141D29',
  scopeMarker: '#94A3B8',
  consoleBg: '#070B10'
};

export interface StatusColors {
  success: string;
  alert: string;
  info: string;
  danger: string;
  muted: string;
}

export function getStatusColors(isDark: boolean): StatusColors {
  if (isDark) {
    return {
      success: IndustrialColors.green,
      alert: IndustrialColors.yellow,
      info: IndustrialColors.cyan,
      danger: IndustrialColors.red,
      muted: '#94A3B8'
    };
  } else {
    return {
      success: '#1B5E20',
      alert: '#B26A00',
      info: '#0277BD',
      danger: '#C62828',
      muted: '#64748B'
    };
  }
}

export const IndustrialDimens = {
  touchMin: 48,
  spacing4: 4,
  spacing8: 8,
  spacing10: 10,
  spacing12: 12,
  spacing16: 16,
  spacing18: 18,
  spacing24: 24,
  spacing32: 32,
  chartHeight: 180,
  brandBadge: 72,
  iconHero: 36,
  iconMd: 20
};
