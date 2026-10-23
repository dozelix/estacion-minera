/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        industrial: {
          amber: '#FF8F00',
          amberLight: '#FFB74D',
          cyan: '#00B0FF',
          green: '#00E676',
          red: '#FF3D00',
          yellow: '#FFD600',
          darkBg: '#0B1017',
          darkSurface: '#141D29',
          darkSurfaceVariant: '#1F2B3B',
          scopePanel: '#0A0E14',
          consoleBg: '#070B10'
        }
      }
    },
  },
  plugins: [],
}
