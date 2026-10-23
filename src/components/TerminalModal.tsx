import React, { useState, useEffect, useRef } from 'react';
import { Terminal, X, Copy, Trash2, ArrowDownCircle, Check, Filter } from 'lucide-react';
import { TerminalLogEntry } from '../types/telemetry';
import { IndustrialColors } from '../theme/tokens';

interface TerminalModalProps {
  isOpen: boolean;
  onClose: () => void;
  logs: TerminalLogEntry[];
  onClearLogs: () => void;
}

export const TerminalModal: React.FC<TerminalModalProps> = ({
  isOpen,
  onClose,
  logs,
  onClearLogs
}) => {
  const [filter, setFilter] = useState<'ALL' | 'RX' | 'TX' | 'SYS'>('ALL');
  const [autoScroll, setAutoScroll] = useState(true);
  const [copied, setCopied] = useState(false);
  const scrollRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (autoScroll && scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  if (!isOpen) return null;

  const filteredLogs = logs.filter(log => {
    if (filter === 'ALL') return true;
    return log.direction === filter;
  });

  const handleCopy = () => {
    const text = filteredLogs
      .map(l => `[${l.timestamp}] [${l.direction}] ${l.payload}`)
      .join('\n');
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 bg-black/75 backdrop-blur-sm animate-fadeIn">
      <div className="flex flex-col w-full max-w-2xl h-[85vh] bg-[#0c131c] border border-[#1f2b3b] rounded-2xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-4 py-3 bg-[#141d29] border-b border-[#1f2b3b]">
          <div className="flex items-center gap-2">
            <div className="p-1.5 bg-[#FF8F00]/20 rounded-lg text-[#FF8F00]">
              <Terminal className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-sm font-bold tracking-wide font-mono-code text-white">
                CONSOLA DE TRAMAS &gt;_
              </h2>
              <p className="text-[11px] text-slate-400">
                Inspección bidireccional de JSON delimitado por \n (ISO 10816-3)
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

        {/* Toolbar */}
        <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2 bg-[#0E1520] border-b border-[#1f2b3b] text-xs">
          {/* Filters */}
          <div className="flex items-center gap-1">
            <span className="text-[11px] text-slate-400 mr-1 flex items-center gap-1">
              <Filter className="w-3 h-3" /> Filtro:
            </span>
            {(['ALL', 'RX', 'TX', 'SYS'] as const).map(f => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-2 py-0.5 rounded font-mono-code font-bold text-[11px] transition ${
                  filter === f
                    ? 'bg-[#FF8F00] text-black shadow-sm'
                    : 'text-slate-400 hover:text-white hover:bg-slate-800'
                }`}
              >
                {f}
              </button>
            ))}
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setAutoScroll(!autoScroll)}
              className={`flex items-center gap-1 px-2 py-1 rounded text-[11px] transition ${
                autoScroll ? 'bg-cyan-500/20 text-cyan-400 border border-cyan-500/30' : 'text-slate-400'
              }`}
            >
              <ArrowDownCircle className="w-3.5 h-3.5" />
              Auto-scroll
            </button>
            <button
              onClick={handleCopy}
              className="flex items-center gap-1 px-2 py-1 rounded text-[11px] bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
              {copied ? 'Copiado' : 'Copiar'}
            </button>
            <button
              onClick={onClearLogs}
              className="flex items-center gap-1 px-2 py-1 rounded text-[11px] bg-rose-500/10 text-rose-400 hover:bg-rose-500/20 transition"
            >
              <Trash2 className="w-3.5 h-3.5" />
              Limpiar
            </button>
          </div>
        </div>

        {/* Console Log Area */}
        <div
          ref={scrollRef}
          className="flex-1 p-3 overflow-y-auto font-mono-code text-[11.5px] leading-relaxed bg-[#070b10] text-slate-200 select-text"
        >
          {filteredLogs.length === 0 ? (
            <div className="h-full flex items-center justify-center text-slate-600">
              No hay tramas registradas bajo este filtro.
            </div>
          ) : (
            filteredLogs.map(log => {
              let tagColor = 'text-cyan-400 bg-cyan-950/60 border-cyan-800/50';
              if (log.direction === 'TX') {
                tagColor = 'text-amber-400 bg-amber-950/60 border-amber-800/50';
              } else if (log.direction === 'SYS') {
                tagColor = log.isError
                  ? 'text-rose-400 bg-rose-950/60 border-rose-800/50'
                  : 'text-purple-400 bg-purple-950/60 border-purple-800/50';
              }

              return (
                <div key={log.id} className="py-1 border-b border-slate-900/60 flex items-start gap-2 break-all">
                  <span className="text-slate-500 shrink-0 text-[10px] pt-0.5">[{log.timestamp}]</span>
                  <span className={`px-1.5 py-0.2 rounded text-[10px] font-bold border shrink-0 ${tagColor}`}>
                    {log.direction}
                  </span>
                  <span className={log.isError ? 'text-rose-300' : 'text-slate-300'}>
                    {log.payload}
                  </span>
                </div>
              );
            })
          )}
        </div>

        {/* Footer info */}
        <div className="px-4 py-2 bg-[#0c131c] border-t border-[#1f2b3b] text-[11px] text-slate-400 flex justify-between items-center">
          <span>Tramas: {filteredLogs.length} / {logs.length}</span>
          <span className="text-[#FF8F00] font-mono-code">PUERTO: 8080 TCP • RATE: 2 Hz</span>
        </div>
      </div>
    </div>
  );
};
