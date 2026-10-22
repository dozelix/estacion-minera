# Sistema IoT Minero: Estación de Supervisión y Mantenimiento Predictivo (TI3042)

Sistema integral de monitoreo en tiempo real para motores de chancado/flotación en operaciones mineras, orientado a la Evaluación Sumativa 2 (ES2) de la asignatura TI3042. Integra autenticación segura bajo lineamientos ISO 27400 (`EncryptedSharedPreferences`), gestión de permisos de red en tiempo real, arquitectura de alta disponibilidad con **`Foreground Service`** para tolerancia al apagado de pantalla (Doze Mode), reconexión automática ante **microcortes de Wi-Fi**, cliente TCP Socket persistente en `Dispatchers.IO` desacoplado en `ViewModel` (resiliente a rotación), visualización de telemetría de acelerómetro/giróscopo (MPU6050), control y disparo de relé de protección, y el script de emulación en Python indicado por el docente para las pruebas de integración y demostración en red.

---

### Decisiones Críticas Confirmadas

- **Conectividad TCP y Simulación**: Modo híbrido con IP y puerto configurables en la UI para enlace en vivo a scripts Python de sensores, complementado con un simulador de telemetría industrial integrado en la propia app para pruebas autónomas.
- **Resiliencia Industrial y Tolerancia a Fallos**:
  - *Apagado de Pantalla*: `Foreground Service` con notificación persistente para blindar la conexión TCP frente a políticas de ahorro energético del SO (*Doze Mode* / *App Standby*).
  - *Microcortes de Wi-Fi*: Captura defensiva de `IOException` / `SocketException` en `Dispatchers.IO` con bucle de reconexión automática y retroceso exponencial (*exponential backoff*), protegiendo la UI contra crashes.
  - *Rotación de Pantalla (Criterio A5)*: Retención del estado y cliente en `ViewModel` con `StateFlow`, evitando cortes o reinicio del socket al girar el terminal.
- **Telemetría y Control**: Panel gráfico en tiempo real de vibración RMS y aceleración triaxial (MPU6050), detección de umbrales de advertencia/peligro (ISO 10816), control de disparo de relé y botón de parada de emergencia.
- **Ciberseguridad ISO 27400**: Credenciales de operador minero demo preconfiguradas (`operador@faena.cl` / `MineraSegura2026!`) con opción de registrar nuevos usuarios cifrados usando `EncryptedSharedPreferences`.
- **Rol Activo en Interfaz**: Identificador visual prominente de estación ("Estación de Supervisión - Motor Minero #01") conforme a los requerimientos de la Pauta ES2 (Criterio A4 / Estación 3).
- **Validación Práctica con Python**: Integración de un script servidor TCP en Python (`tools/sensor_node_simulator.py`) que emula en tiempo real el nodo MPU6050 enviando tramas JSON y reaccionando a comandos de relé, satisfaciendo la estación de evaluación presencial.

---

## 1. Visión General y Concepto Central

- **Propósito**: Prevenir fallas catastróficas en motores de gran envergadura mediante el análisis continuo de vibraciones (MPU6050) y temperatura, permitiendo desconexión segura mediante un relé industrial con disponibilidad ininterrumpida de grado industrial (24/7).
- **Cumplimiento de Pauta ES2**:
  1. *Login Seguro (ISO 27400)*: Credenciales almacenadas de manera cifrada con `EncryptedSharedPreferences` y Android Keystore.
  2. *Permisos en Runtime*: Verificación activa de conectividad de red y permisos (`POST_NOTIFICATIONS`, `INTERNET`), sin cierres forzados ante denegación.
  3. *Cliente Sockets TCP con Corrutinas*: Flujo continuo de tramas JSON en hilo de fondo (`Dispatchers.IO`), con reconexión automática ante microcortes de red.
  4. *Rol Visible en UI*: Insignia de estado que diferencia el nodo supervisor del nodo sensor.
  5. *Tolerancia a Rotación y Pantalla Bloqueada (Criterios A4 y A5)*: Arquitectura desacoplada en `ViewModel` y respaldo mediante `Foreground Service` que mantiene el socket activo aunque la pantalla se apague o gire.
  6. *Entregable Git y `README.md`*: Especificación formal de arquitectura, protocolo de tramas JSON, instrucciones de compilación y el script de Python integrado en el repositorio.

---

## 2. Experiencia de Usuario y Diseño Visual

### Flujo de Navegación

1. **Acceso Seguro (Login)**:
   - Pantalla de credenciales con validación industrial y almacenamiento cifrado.
   - Acceso rápido con credencial de demostración para evaluación docente.
   - Pestaña de registro de nuevo operador con cifrado inmediato.

2. **Dashboard de Supervisión Minera (Estación Activa)**:
   - **Barra Superior**: Rol activo destacado (*"ROL: ESTACIÓN DE SUPERVISIÓN - MOTOR MINERO"*), estado del enlace (Conectado / Desconectado / Reintentando / Simulador), switch de servicio en segundo plano y botón de cierre de sesión.
   - **Panel de Conexión**: Configuración dinámica de IP y puerto TCP, conmutador de modo Simulación / Socket Real.
   - **Telemetría MPU6050 en Vivo**:
     - Vibración RMS (mm/s) con indicador de estado (Normal, Alerta, Crítico).
     - Valores de aceleración triaxial $(A_x, A_y, A_z)$ y temperatura del devanado (°C).
     - Gráfico dinámico de tendencia de vibración en tiempo real.
   - **Mando de Seguridad del Relé**:
     - Indicador visual del estado del relé (Cerrado vs Abierto).
     - Botones de acción: *Disparo Preventivo de Relé*, *Restablecer Relé (Reset)* y *Parada de Emergencia*.
   - **Consola de Diagnóstico de Tramas**:
     - Visor en vivo de tramas JSON enviadas y recibidas por socket para verificación docente.

---

## 3. Decisiones Técnicas Clave y Justificación

| Escenario Crítico | Enfoque Seleccionado | Justificación Técnica |
| :--- | :--- | :--- |
| **Apagado de Pantalla (Doze Mode)** | `Foreground Service` con Notificación Persistente | Evita que el sistema operativo mate el proceso o pause las corrutinas de red al bloquear la pantalla o suspender el dispositivo. |
| **Microcortes de Wi-Fi** | Bucle de reconexión con Backoff en `Dispatchers.IO` | Captura excepciones de socket (`IOException`, `SocketTimeoutException`) sin propagarlas a la UI, reanudando la sesión automáticamente al volver la red. |
| **Rotación de Pantalla (Criterio A5)** | `ViewModel` + Kotlin `StateFlow` | La recreación de la `Activity` no interrumpe el socket ni resetea el histórico de telemetría. |
| **Cifrado Local (ISO 27400)** | `EncryptedSharedPreferences` (AndroidX Security) | Cumplimiento estricto; usa Android Keystore y AES-256 GCM, garantizando cero texto plano en memoria flash. |
| **Protocolo de Datos** | Tramas JSON delimitadas por salto de línea (`\n`) | Estándar en microcontroladores y emuladores IoT; compatible con el script de Python de pruebas. |

---

## 4. Arquitectura y Protocolo de Comunicación

```
┌─────────────────────────────────────────────────────────────┐
│                 NODO SENSOR (EXTERNO O LOCAL)               │
│   • Script Python (sensor_node_simulator.py)                │
│   • Emulación MPU6050 (ax, ay, az, gx, gy, gz, temp, rpm)   │
│   • Receptor de Comandos (RELAY_TRIP, RELAY_RESET)          │
└──────────────┬──────────────────────────────▲───────────────┘
               │ JSON Telemetría (\n)         │ JSON Comando (\n)
               │ Puerto TCP 8080              │
┌──────────────▼──────────────────────────────┴───────────────┐
│           ESTACIÓN DE MONITOREO ANDROID (APP KOTLIN)        │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ MonitoringForegroundService (Persistencia Doze)     │   │
│   │  • Notificación en curso con estado del motor       │   │
│   │  • Mantiene el proceso activo con pantalla apagada  │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │                              │
│   ┌──────────────────────────▼──────────────────────────┐   │
│   │ Capa de Red y Repositorio (Dispatchers.IO)          │   │
│   │  • TcpSocketClient: Socket TCP persistente          │   │
│   │  • Auto-reconexión defensiva ante microcortes       │   │
│   │  • TelemetryRepository: Parser JSON, buffer & Flow  │   │
│   │  • EncryptedAuthStore: EncryptedSharedPreferences   │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │ StateFlow<TelemetryState>    │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ MonitoringViewModel (Sobrevive Rotación Criterio A5)│   │
│   │  • Buffer de muestras históricas para gráficos      │   │
│   │  • Evaluación de umbrales ISO 10816                 │   │
│   │  • Disparo automático / manual de relé              │   │
│   └──────────────────────────┬──────────────────────────┘   │
│                              │ UI State                     │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐   │
│   │ Interfaz de Usuario Jetpack Compose (M3)            │   │
│   │  • [Tag Visible] "Estación de Supervisión Minera"   │   │
│   │  • Gráfico dinámico de vibración RMS                │   │
│   │  • Panel de telemetría MPU6050 & Botón de Relé      │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Especificación del Protocolo de Tramas JSON

**1. Telemetría del Sensor hacia la Estación (Downlink)**:
```json
{
  "device_id": "MPU6050_MOTOR_01",
  "timestamp": 1729512345,
  "accel": { "x": 0.12, "y": 0.45, "z": 9.81 },
  "vibration_rms": 2.45,
  "temperature_c": 54.2,
  "relay_state": "CLOSED"
}
```

**2. Comandos de la Estación hacia el Sensor/Relé (Uplink)**:
```json
{
  "command": "SET_RELAY",
  "state": "OPEN",
  "reason": "VIBRATION_THRESHOLD_EXCEEDED"
}
```

---

## 5. Plan de Ejecución y Módulos a Construir

1. **Configuración Inicial y Permisos**:
   - `build.gradle.kts`: `androidx.security:security-crypto`.
   - `AndroidManifest.xml`: Permisos `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` / `SPECIAL_USE`, `POST_NOTIFICATIONS`.
   - Sincronización de nombre en `metadata.json` y `strings.xml`.
2. **Capa de Seguridad (ISO 27400)**:
   - `EncryptedAuthStore.kt`: Almacenamiento cifrado en hardware/software con `MasterKeys`.
   - Pantalla de inicio de sesión y registro seguro.
3. **Capa de Servicio en Primer Plano y Red**:
   - `MonitoringForegroundService.kt`: Servicio persistente con notificación que protege las corrutinas y el socket contra Doze Mode.
   - `TcpSocketClient.kt`: Bucle asíncrono con captura de excepciones para reconexión automática tras microcortes de Wi-Fi.
   - `TelemetryRepository.kt`: Gestor de telemetría con modo simulación integrado de respaldo.
4. **Capa de UI y ViewModel (Tolerancia a Rotación)**:
   - `MonitoringViewModel.kt`: Sobrevive al ciclo de vida de la Activity y retiene la serie temporal de vibración.
   - `MonitoringScreen.kt`: Rol visible permanente ("ESTACIÓN DE SUPERVISIÓN - MOTOR MINERO"), gráficos Canvas en tiempo real, mandos de relé y consola de tramas.
   - Manejo amigable de permisos en runtime (notificaciones y red).
5. **Validación en Red con Script de Python y Documentación**:
   - Script servidor TCP en Python (`tools/sensor_node_simulator.py`) que emula en tiempo real el nodo MPU6050 para la demostración presencial.
   - `README.md` técnico profesional con problemática minera, justificación de arquitectura (Doze, microcortes, rotación), especificación de protocolo e instrucciones paso a paso.
