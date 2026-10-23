import { UserProfile } from '../types/telemetry';

/**
 * ISO 27400 compliant authentication store emulation.
 * Mimics EncryptedAuthStore.kt with local encryption and pre-seeded demo operator.
 */

const STORAGE_PREFIX = 'mining_auth_secure_prefs_';
const KEY_HAS_USERS = `${STORAGE_PREFIX}has_users_registered`;
const KEY_ACTIVE_USER = `${STORAGE_PREFIX}active_user`;

export const DEFAULT_OPERATOR_EMAIL = 'operador@faena.cl';
export const DEFAULT_OPERATOR_PASSWORD = 'MineraSegura2026!';
export const DEFAULT_OPERATOR_NAME = 'Ing. Carlos Mendoza';
export const DEFAULT_OPERATOR_ROLE = 'Supervisor de Mantenimiento Predictivo';

// Simple XOR + Base64 obfuscation layer simulating hardware Keystore backing in browser
function encryptValue(text: string): string {
  try {
    const key = 0xAA;
    const chars = text.split('').map(c => String.fromCharCode(c.charCodeAt(0) ^ key)).join('');
    return btoa(unescape(encodeURIComponent(chars)));
  } catch {
    return btoa(text);
  }
}

function decryptValue(cipher: string): string {
  try {
    const text = decodeURIComponent(escape(atob(cipher)));
    const key = 0xAA;
    return text.split('').map(c => String.fromCharCode(c.charCodeAt(0) ^ key)).join('');
  } catch {
    return atob(cipher);
  }
}

export class EncryptedAuthStoreService {
  constructor() {
    this.seedDefaultOperatorIfEmpty();
  }

  private seedDefaultOperatorIfEmpty(): void {
    const hasUsers = localStorage.getItem(KEY_HAS_USERS);
    if (!hasUsers) {
      this.saveUser(
        DEFAULT_OPERATOR_EMAIL,
        DEFAULT_OPERATOR_PASSWORD,
        DEFAULT_OPERATOR_NAME,
        DEFAULT_OPERATOR_ROLE
      );
      localStorage.setItem(KEY_HAS_USERS, 'true');
    }
  }

  saveUser(email: string, password: string, name: string, role: string): boolean {
    const normalizedEmail = email.trim().toLowerCase();
    const userKey = `${STORAGE_PREFIX}user_${normalizedEmail}`;
    const rawData = `${password}|||${name}|||${role}`;
    localStorage.setItem(userKey, encryptValue(rawData));
    localStorage.setItem(KEY_HAS_USERS, 'true');
    return true;
  }

  verifyCredentials(email: string, password: string): UserProfile | null {
    const normalizedEmail = email.trim().toLowerCase();
    const userKey = `${STORAGE_PREFIX}user_${normalizedEmail}`;
    const cipher = localStorage.getItem(userKey);
    if (!cipher) return null;

    try {
      const rawData = decryptValue(cipher);
      const parts = rawData.split('|||');
      if (parts.length >= 3) {
        const storedPassword = parts[0];
        const storedName = parts[1];
        const storedRole = parts[2];

        if (storedPassword === password) {
          return {
            email: normalizedEmail,
            name: storedName,
            role: storedRole
          };
        }
      }
    } catch (e) {
      console.error('Error verifying credentials:', e);
    }
    return null;
  }

  setActiveSession(profile: UserProfile | null): void {
    if (profile) {
      localStorage.setItem(KEY_ACTIVE_USER, encryptValue(JSON.stringify(profile)));
    } else {
      localStorage.removeItem(KEY_ACTIVE_USER);
    }
  }

  getActiveSession(): UserProfile | null {
    const cipher = localStorage.getItem(KEY_ACTIVE_USER);
    if (!cipher) return null;
    try {
      const jsonStr = decryptValue(cipher);
      return JSON.parse(jsonStr) as UserProfile;
    } catch {
      return null;
    }
  }

  clearActiveSession(): void {
    this.setActiveSession(null);
  }
}

export const authStore = new EncryptedAuthStoreService();
