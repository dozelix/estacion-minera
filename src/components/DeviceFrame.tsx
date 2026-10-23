import React, { useState } from 'react';
import { Smartphone, Monitor, Wifi, Battery, Signal, Moon, Sun } from 'lucide-react';

interface DeviceFrameProps {
  children: React.ReactNode;
  title?: string;
}

export const DeviceFrame: React.FC<DeviceFrameProps> = ({ children, title = 'Estación Minera TI3042' }) => {
  const [isMobileMode, setIsMobileMode] = useState(true);
  const [currentTime, setCurrentTime] = useState(() => 
    new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  );

  useEffect(() => {
    const timer = setInterval(() => {
      setCurrentTime(new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }));
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  return (
    <div className="min-h-screen w-full bg-[#070b10] flex flex-col items-center justify-start text-slate-100 selection:bg-[#FF8F00] selection:text-black">
      {/* Top Simulator Control Bar */}
      <div className="w-full bg-[#0e1622] border-b border-[#1f2b3b] px-4 py-2.5 flex items-center justify-between text-xs font-mono-code z-40 sticky top-0 shadow-md">
        <div className="flex items-center gap-2.5">
          <span className="flex h-2.5 w-2.5 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
          </span>
          <span className="font-bold text-[#FF8F00] tracking-wide">{title}</span>
          <span className="text-slate-500 hidden sm:inline">| Emulador Android TI3042 ES2</span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsMobileMode(!isMobileMode)}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border text-xs font-bold transition shadow-sm ${
              isMobileMode
                ? 'bg-[#FF8F00] text-black border-[#FF8F00]'
                : 'bg-[#141D29] text-slate-300 border-[#1F2B3B] hover:text-white hover:border-slate-600'
            }`}
          >
            {isMobileMode ? <Smartphone className="w-3.5 h-3.5" /> : <Monitor className="w-3.5 h-3.5" />}
            <span>{isMobileMode ? 'Vista Móvil Android (Activa)' : 'Vista Estación HMI'}</span>
          </button>
        </div>
      </div>

      {/* Frame Container */}
      <div className={`w-full flex-1 flex items-center justify-center ${isMobileMode ? 'p-2 sm:p-5' : ''}`}>
        {isMobileMode ? (
          /* Realistic Android Phone Device Shell */
          <div className="relative my-auto">
            {/* Outer Hardware Buttons */}
            <div className="absolute -left-2 top-28 w-1 h-12 bg-slate-700 rounded-l-sm" title="Vol +" />
            <div className="absolute -left-2 top-44 w-1 h-12 bg-slate-700 rounded-l-sm" title="Vol -" />
            <div className="absolute -right-2 top-32 w-1 h-14 bg-slate-700 rounded-r-sm" title="Power" />

            {/* Smartphone Chassis */}
            <div className="w-[390px] max-w-[94vw] h-[820px] max-h-[calc(100vh-65px)] bg-black rounded-[48px] p-3 shadow-2xl border-[5px] border-[#222c38] relative flex flex-col overflow-hidden ring-1 ring-white/15">
              {/* Phone Camera Hole & Ear Speaker */}
              <div className="absolute top-3.5 left-1/2 -translate-x-1/2 z-50 flex items-center gap-2 pointer-events-none">
                <div className="w-3.5 h-3.5 rounded-full bg-[#05070a] border border-slate-800 shadow-inner" />
              </div>

              {/* Android Status Bar */}
              <div className="w-full h-8 px-6 pt-1 flex items-center justify-between text-[11px] font-mono-code text-slate-300 select-none z-40 bg-[#0B1017] shrink-0">
                <span className="font-semibold tracking-tight">{currentTime}</span>
                <div className="flex items-center gap-2">
                  <Signal className="w-3.5 h-3.5 text-slate-300" />
                  <span className="text-[10px] font-bold text-slate-400">5G</span>
                  <Wifi className="w-3.5 h-3.5 text-slate-300" />
                  <div className="flex items-center gap-0.5">
                    <span className="text-[10px] font-bold text-emerald-400">98%</span>
                    <Battery className="w-3.5 h-3.5 text-emerald-400 fill-emerald-400" />
                  </div>
                </div>
              </div>

              {/* Screen Inner Viewport with Smooth Scroll */}
              <div className="w-full flex-1 rounded-[32px] overflow-y-auto overflow-x-hidden bg-[#0B1017] flex flex-col relative overscroll-contain">
                {children}
              </div>

              {/* Android Gesture Navigation Pill Bar */}
              <div className="w-full h-5 flex items-center justify-center bg-[#0B1017] shrink-0 pt-0.5">
                <div className="w-32 h-1 bg-slate-500/50 rounded-full" />
              </div>
            </div>
          </div>
        ) : (
          /* Full Viewport HMI Station */
          <div className="w-full h-full flex-1 flex flex-col">
            {children}
          </div>
        )}
      </div>
    </div>
  );
};
