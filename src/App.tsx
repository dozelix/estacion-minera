import React, { useState, useEffect } from 'react';
import { LoginScreen } from './screens/LoginScreen';
import { MonitoringScreen } from './screens/MonitoringScreen';
import { DeviceFrame } from './components/DeviceFrame';
import { authStore } from './services/authStore';
import { UserProfile } from './types/telemetry';

export const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(() => authStore.getActiveSession());

  useEffect(() => {
    const active = authStore.getActiveSession();
    if (active) {
      setCurrentUser(active);
    }
  }, []);

  const handleLoginSuccess = (user: UserProfile) => {
    setCurrentUser(user);
  };

  const handleLogout = () => {
    authStore.clearActiveSession();
    setCurrentUser(null);
  };

  return (
    <DeviceFrame title="Estación Minera TI3042">
      {currentUser ? (
        <MonitoringScreen
          user={currentUser}
          onLogout={handleLogout}
        />
      ) : (
        <LoginScreen
          onLoginSuccess={handleLoginSuccess}
        />
      )}
    </DeviceFrame>
  );
};

export default App;
