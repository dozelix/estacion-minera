import React, { useState } from 'react';
import {
  Shield,
  User,
  Lock,
  Eye,
  EyeOff,
  CheckCircle2,
  AlertTriangle,
  BadgeCheck,
  Briefcase,
  KeyRound,
  Sparkles
} from 'lucide-react';
import { authStore, DEFAULT_OPERATOR_EMAIL, DEFAULT_OPERATOR_PASSWORD } from '../services/authStore';
import { UserProfile } from '../types/telemetry';
import { IndustrialColors } from '../theme/tokens';

interface LoginScreenProps {
  onLoginSuccess: (user: UserProfile) => void;
  isDark?: boolean;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onLoginSuccess, isDark = true }) => {
  const [activeTab, setActiveTab] = useState<'LOGIN' | 'REGISTER'>('LOGIN');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState('Operador de Faena');
  const [showPassword, setShowPassword] = useState(false);
  const [statusMessage, setStatusMessage] = useState<{ text: string; isError: boolean } | null>(null);

  const handleDemoFill = () => {
    setEmail(DEFAULT_OPERATOR_EMAIL);
    setPassword(DEFAULT_OPERATOR_PASSWORD);
    setStatusMessage({
      text: 'Credenciales demo cargadas (ISO 27400). Presione "Ingresar".',
      isError: false
    });
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setStatusMessage(null);

    if (!email.trim() || !password.trim()) {
      setStatusMessage({ text: 'Por favor complete todos los campos requeridos.', isError: true });
      return;
    }

    if (activeTab === 'LOGIN') {
      const user = authStore.verifyCredentials(email, password);
      if (user) {
        authStore.setActiveSession(user);
        onLoginSuccess(user);
      } else {
        setStatusMessage({
          text: 'Credenciales inválidas. Compruebe usuario o use el botón de demo.',
          isError: true
        });
      }
    } else {
      // Register
      if (!name.trim()) {
        setStatusMessage({ text: 'Por favor ingrese el nombre del operador.', isError: true });
        return;
      }
      authStore.saveUser(email, password, name, role);
      const user: UserProfile = { email: email.trim().toLowerCase(), name, role };
      authStore.setActiveSession(user);
      onLoginSuccess(user);
    }
  };

  return (
    <div className="min-h-full w-full flex flex-col items-center justify-center p-4 sm:p-6 bg-[#0B1017]">
      <div className="w-full max-w-md space-y-6">
        {/* Brand Icon Badge & System Title */}
        <div className="flex flex-col items-center text-center space-y-2">
          <div className="w-16 h-16 rounded-2xl bg-[#FF8F00]/15 flex items-center justify-center border border-[#FF8F00]/30 shadow-lg shadow-[#FF8F00]/10">
            <Shield className="w-9 h-9 text-[#FF8F00]" />
          </div>

          <div className="font-mono-code font-bold text-xs text-[#FF8F00] tracking-[0.2em] uppercase mt-2">
            SISTEMA IoT MINERO TI3042
          </div>

          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight font-industrial">
            Estación de Supervisión
          </h1>

          <p className="text-xs sm:text-sm text-slate-400 max-w-xs">
            Monitoreo Predictivo de Motor de Molienda con Disparo de Relé ISO 10816-3
          </p>
        </div>

        {/* Card Container */}
        <div className="bg-[#141D29] border border-[#1F2B3B] rounded-2xl p-6 shadow-xl space-y-5">
          {/* Tabs */}
          <div className="grid grid-cols-2 p-1 bg-[#0B1017] rounded-xl border border-[#1F2B3B]">
            <button
              type="button"
              onClick={() => { setActiveTab('LOGIN'); setStatusMessage(null); }}
              className={`py-2 text-xs font-bold rounded-lg transition ${
                activeTab === 'LOGIN'
                  ? 'bg-[#FF8F00] text-black shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Iniciar Sesión
            </button>
            <button
              type="button"
              onClick={() => { setActiveTab('REGISTER'); setStatusMessage(null); }}
              className={`py-2 text-xs font-bold rounded-lg transition ${
                activeTab === 'REGISTER'
                  ? 'bg-[#FF8F00] text-black shadow'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Nuevo Operador
            </button>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="space-y-4">
            {activeTab === 'REGISTER' && (
              <>
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Nombre Completo del Operador
                  </label>
                  <div className="relative">
                    <User className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                    <input
                      type="text"
                      value={name}
                      onChange={e => setName(e.target.value)}
                      placeholder="ej. Ing. Andrea Valdés"
                      className="w-full pl-9 pr-3 py-2.5 bg-[#0B1017] border border-[#1F2B3B] rounded-xl text-xs text-white placeholder-slate-600 focus:outline-none focus:border-[#FF8F00]"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Rol Operacional
                  </label>
                  <div className="relative">
                    <Briefcase className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                    <select
                      value={role}
                      onChange={e => setRole(e.target.value)}
                      className="w-full pl-9 pr-3 py-2.5 bg-[#0B1017] border border-[#1F2B3B] rounded-xl text-xs text-white focus:outline-none focus:border-[#FF8F00]"
                    >
                      <option value="Supervisor de Mantenimiento Predictivo">Supervisor de Mantenimiento</option>
                      <option value="Operador de Faena Chancado">Operador de Faena Chancado</option>
                      <option value="Ingeniero de Automatización e IoT">Ingeniero de Automatización</option>
                      <option value="Técnico Eléctrico de Guardia">Técnico Eléctrico</option>
                    </select>
                  </div>
                </div>
              </>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Correo Electrónico Faena
              </label>
              <div className="relative">
                <User className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                <input
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="operador@faena.cl"
                  className="w-full pl-9 pr-3 py-2.5 bg-[#0B1017] border border-[#1F2B3B] rounded-xl text-xs text-white placeholder-slate-600 focus:outline-none focus:border-[#FF8F00]"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Contraseña de Seguridad
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-2.5 w-4 h-4 text-slate-500" />
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  className="w-full pl-9 pr-10 py-2.5 bg-[#0B1017] border border-[#1F2B3B] rounded-xl text-xs text-white placeholder-slate-600 focus:outline-none focus:border-[#FF8F00]"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-2.5 text-slate-500 hover:text-slate-300"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {/* Status Message */}
            {statusMessage && (
              <div
                className={`flex items-start gap-2 p-3 rounded-xl text-xs border ${
                  statusMessage.isError
                    ? 'bg-red-950/40 border-red-800/60 text-red-300'
                    : 'bg-emerald-950/40 border-emerald-800/60 text-emerald-300'
                }`}
              >
                {statusMessage.isError ? (
                  <AlertTriangle className="w-4 h-4 shrink-0 text-red-400 mt-0.5" />
                ) : (
                  <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-400 mt-0.5" />
                )}
                <span>{statusMessage.text}</span>
              </div>
            )}

            {/* Submit button (≥48dp min height = 52px) */}
            <button
              type="submit"
              className="w-full h-13 min-h-[50px] bg-[#FF8F00] hover:bg-[#FFA000] text-black font-extrabold text-xs tracking-wider uppercase rounded-xl shadow-lg shadow-[#FF8F00]/20 flex items-center justify-center gap-2 transition active:scale-[0.99]"
            >
              <KeyRound className="w-4 h-4" />
              {activeTab === 'LOGIN' ? 'INGRESAR A LA ESTACIÓN' : 'REGISTRAR Y ACCEDER'}
            </button>

            {/* Quick Demo button */}
            <button
              type="button"
              onClick={handleDemoFill}
              className="w-full py-2.5 bg-[#1F2B3B] hover:bg-[#2A3B4F] text-[#FFB74D] font-bold text-xs rounded-xl border border-[#FF8F00]/30 flex items-center justify-center gap-1.5 transition"
            >
              <Sparkles className="w-3.5 h-3.5 text-[#FF8F00]" />
              Usar Credenciales Demo (Pauta Evaluación)
            </button>
          </form>

          {/* Security footnote */}
          <div className="pt-3 border-t border-[#1F2B3B] flex items-center justify-center gap-2 text-[10px] text-slate-500 font-mono-code text-center">
            <BadgeCheck className="w-3.5 h-3.5 text-emerald-500 shrink-0" />
            <span>Almacenamiento Cifrado ISO 27400 • AES-256 GCM</span>
          </div>
        </div>
      </div>
    </div>
  );
};
