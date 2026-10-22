# Sistema IoT Minero: Estación de Supervisión y Mantenimiento Predictivo (TI3042)

**Evaluación Sumativa 2 (ES2) — Asignatura TI3042: Sistemas IoT y Redes Industriales**  
**Proyecto:** Estación Móvil de Supervisión y Control para Motores Mineros de Chancado y Molienda.

---

## 1. Problemática Minera Resuelta

En las operaciones de la gran y mediana minería, los **motores eléctricos de gran potencia** que impulsan chancadores primarios, molinos SAG y bombas de pulpa/relaves operan bajo condiciones extremas de carga cíclica, polvo abrasivo y temperatura severa. 

El fallo catastrófico de un motor de este tipo produce:
- **Detención no programada de la faena**, con pérdidas estimadas entre US\$ 50.000 y US\$ 200.000 por hora de paro.
- **Riesgo crítico de seguridad física** para los operadores ante eventuales roturas mecánicas por desbalance severo o trabamiento de rodamientos.

### Solución Implementada:
Esta solución implementa una **Estación Móvil de Supervisión Industrial** conectada a un nodo sensor remoto (MPU6050 + Módulo Relé) que realiza:
1. **Medición continua de vibración RMS (mm/s)** y aceleración triaxial $(A_x, A_y, A_z)$ a través de un sensor inercial MPU6050.
2. **Evaluación de severidad según la norma internacional ISO 10816-3** para maquinaria rotatoria industrial:
   - **Zona A/B (Normal):** $< 4.5\text{ mm/s}$ (Operación segura continua).
   - **Zona C (Alerta):** $4.5 - 7.1\text{ mm/s}$ (Operación restringida, inspección requerida).
   - **Zona D (Peligro/Crítico):** $> 7.1\text{ mm/s}$ (Riesgo inminente de daño destructivo).
3. **Mando de Seguridad y Disparo de Relé de Desenergización:** Apertura remota manual o disparo automático (*Auto-Trip*) del relé electromecánico para aislar eléctricamente el motor antes de que ocurra una falla estructural.

---

## 2. Justificación de la Arquitectura Técnica y Resiliencia

El sistema ha sido diseñado bajo estándares industriales y las especificaciones estrictas de la **Pauta ES2**:

```
┌─────────────────────────────────────────────────────────────┐
│                 NODO SENSOR (EXTERNO O LOCAL)               │
│   • Script Python (tools/sensor_node_simulator.py)          │
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
│   │  • Notificación persistente con estado del motor    │   │
│   │  • Mantiene socket activo con pantalla bloqueada    │   │
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
│   │  • [Tag Visible] "ROL: ESTACIÓN DE SUPERVISIÓN"     │   │
│   │  • Gráfico dinámico de vibración RMS en Canvas      │   │
│   │  • Panel de telemetría MPU6050 & Botón de Relé      │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 1. Login Seguro Cifrado (Estándar ISO 27400)
- **Implementación:** `EncryptedAuthStore.kt` haciendo uso de `androidx.security.crypto.EncryptedSharedPreferences` y `MasterKey`.
- **Mecanismo:** Clave maestra protegida por hardware en **Android Keystore** utilizando:
  - `AES256_SIV` para el cifrado determinístico de las claves de acceso.
  - `AES256_GCM` para el cifrado autenticado de los datos de perfil y credenciales.
- **Cumplimiento:** Cero almacenamiento en texto plano en disco, garantizando confidencialidad e integridad conforme a normas de ciberseguridad en redes IoT industriales (ISO/IEC 27400).

### 2. Rol Visible en la Interfaz (Criterio A4 / Estación 3)
- La pantalla principal muestra un encabezado permanente e inalterable:  
  `[ ROL: ESTACIÓN DE SUPERVISIÓN - MOTOR MINERO #01 ]`
- Diferencia de forma inequívoca el rol de supervisión y mando móvil frente al rol de nodo emisor/sensor en terreno.

### 3. Tolerancia Real a la Rotación de Pantalla (Criterio A5 / Estación 4)
- **Implementación:** Arquitectura MVVM desacoplada con `MonitoringViewModel` y `StateFlow`.
- **Comportamiento:** Ante la destrucción y recreación de la `MainActivity` por cambio de configuración (giro vertical a horizontal o viceversa), el `ViewModel` mantiene intacta la conexión del socket TCP y el historial temporal de vibraciones sin desconexiones, reinicios ni parpadeos.

### 4. Resiliencia ante el Apagado de Pantalla (Doze Mode)
- **Implementación:** `MonitoringForegroundService.kt` con notificación continua en el sistema.
- **Justificación:** Las políticas agresivas de ahorro energético de Android (*Doze Mode* y *App Standby*) pausan los hilos de fondo y cierran los sockets TCP cuando la pantalla se bloquea. El Foreground Service garantiza ejecución continua 24/7 y supervisión ininterrumpida de alarmas.

### 5. Resiliencia ante Microcortes de Wi-Fi
- **Implementación:** `TcpSocketClient.kt` ejecutado estrictamente en `Dispatchers.IO`.
- **Mecanismo:** Aislamiento defensivo de `IOException`, `SocketException` y `SocketTimeoutException` con bucle de reconexión automática mediante retroceso exponencial (*exponential backoff* de 1s a 15s), evitando cualquier bloqueo o caída repentina (*crash*) en la interfaz gráfica.

---

## 3. Especificación del Protocolo de Comunicación Inalámbrica

- **Protocolo de Transporte:** TCP sobre IP (puerto predeterminado `8080`, configurable dinámicamente en la UI).
- **Delimitador de Trama:** Salto de línea (`\n`), estándar en transmisiones de microcontroladores y sockets industriales.

### Trama Downlink: Nodo Sensor hacia Estación Android (Telemetría a 2 Hz)
```json
{
  "device_id": "MPU6050_MOTOR_01",
  "timestamp": 1729512345,
  "accel": { "x": 0.12, "y": 0.45, "z": 9.81 },
  "gyro": { "x": 0.01, "y": 0.02, "z": 0.05 },
  "vibration_rms": 2.45,
  "temperature_c": 54.2,
  "rpm": 1480,
  "relay_state": "CLOSED"
}
```

| Campo | Tipo | Unidad / Rango | Descripción |
| :--- | :--- | :--- | :--- |
| `device_id` | String | Texto alfanumérico | Identificador único del nodo sensor |
| `timestamp` | Long | Segundos UNIX | Marca temporal del muestreo |
| `accel.x/y/z` | Float | $m/s^2$ ($\pm 16g$) | Aceleración triaxial MPU6050 |
| `gyro.x/y/z` | Float | $°/s$ ($\pm 2000°/s$) | Velocidad angular triaxial MPU6050 |
| `vibration_rms`| Float | $mm/s$ ($0 - 15$) | Valor eficaz de vibración (ISO 10816) |
| `temperature_c`| Float | °C ($0 - 120$) | Temperatura del devanado del estator |
| `rpm` | Int | RPM ($0 - 3600$) | Velocidad de rotación del eje |
| `relay_state` | String | `"CLOSED"` / `"OPEN"` | Estado físico del circuito de relé |

### Trama Uplink: Estación Android hacia Nodo Sensor (Comando de Control)
```json
{
  "command": "SET_RELAY",
  "state": "OPEN",
  "reason": "VIBRATION_THRESHOLD_EXCEEDED",
  "operator": "operador@faena.cl",
  "timestamp": 1729512348
}
```

| Campo | Tipo | Valores Permitidos | Descripción |
| :--- | :--- | :--- | :--- |
| `command` | String | `"SET_RELAY"` | Tipo de instrucción enviada |
| `state` | String | `"OPEN"` / `"CLOSED"` | Acción: Abrir (desconectar) o Cerrar (rearmar) |
| `reason` | String | Texto descriptivo | Motivo del disparo (ej: Alarma ISO 10816, Parada Emergencia) |
| `operator` | String | Correo operador | Identificación del usuario autenticado |

---

## 4. Instrucciones de Compilación y Ejecución

### Requisitos Previos:
- **Android:** Android Studio Jellyfish / Koala (o Gradle 8.5+ con JDK 17/21).
- **Python:** Python 3.8 o superior para el nodo sensor emulado.
- **Red:** Dispositivo Android y PC en la misma red Wi-Fi, o emulador Android estándar.

### Paso 1: Ejecutar el Nodo Sensor en Python
Abre una terminal en la raíz del repositorio y ejecuta:
```bash
python3 tools/sensor_node_simulator.py --port 8080
```
El script mostrará la dirección IP local del PC (ej: `192.168.1.105:8080`) y quedará a la espera de conexiones.

### Paso 2: Compilar y Ejecutar la App Android
1. Abre el proyecto en Android Studio o compila mediante terminal:
   ```bash
   gradle assembleDebug
   ```
2. Instala el APK generado en tu dispositivo o emulador:
   ```bash
   gradle installDebug
   ```

### Paso 3: Acceso Seguro y Enlace
1. En la pantalla de **Login**, presiona el botón **"Cargar Credenciales de Evaluación Demo"** (rellena automáticamente `operador@faena.cl` / `MineraSegura2026!`).
2. Presiona **"INGRESAR A SUPERVISIÓN"** (las credenciales se validan contra `EncryptedSharedPreferences`).
3. En la pantalla de **Monitoreo**, presiona el botón de **Configuración** (icono de engranaje en la barra superior):
   - Si usas el **Emulador Android**, deja la IP por defecto: `10.0.2.2` y puerto `8080`.
   - Si usas un **Teléfono Físico**, ingresa la IP local de tu PC (ej: `192.168.1.105`) y puerto `8080`.
   - Presiona **"Guardar y Conectar"**.
4. ¡Listo! Observarás la telemetría en tiempo real y el flujo bidireccional en la consola del script de Python y en la app.

---

## 5. Guía de Demostración para la Estación de Evaluación (Docente)

Para validar cada estación de la pauta ante el profesor:

| Criterio Evaluado | Acción de Demostración | Resultado Esperado |
| :--- | :--- | :--- |
| **Login Seguro (ISO 27400)** | Iniciar sesión y registrar un nuevo operador. Inspeccionar almacenamiento. | Cero contraseñas en texto plano; almacenamiento cifrado con AES-256 en Android Keystore. |
| **Rol Visible (Criterio A4)** | Mostrar la barra superior de la app. | Etiqueta destacada permanente: `"ROL: ESTACIÓN DE SUPERVISIÓN"`. |
| **Telemetría y Control de Relé** | En la terminal de Python presionar `a` (inyectar anomalía). | La app detecta vibración $> 7.1\text{ mm/s}$, pasa a color ROJO y ejecuta el *Auto-Trip* abriendo el relé. |
| **Rearme de Relé** | En la app presionar *"Rearmar Relé"*. | La app envía comando `SET_RELAY: CLOSED`. La terminal de Python recibe la orden y el motor vuelve a girar. |
| **Rotación de Pantalla (Criterio A5)** | Girar el teléfono de vertical a horizontal repetidas veces. | Cero cortes de socket, cero errores, la gráfica y la telemetría continúan fluyendo gracias al `ViewModel`. |
| **Apagado de Pantalla (Doze)** | Activar el switch *"Servicio Pantalla Apagada"* y bloquear la pantalla. | El socket se mantiene vivo en segundo plano con notificación permanente. |
| **Microcortes de Wi-Fi** | Apagar brevemente el Wi-Fi o pausar el script de Python y reactivarlo. | La app indica *"Reconectando..."* sin cerrarse inesperadamente, y reanuda el flujo automáticamente. |
| **Inspección de Tramas** | Presionar el botón de terminal (icono `>_`) en la barra superior. | Muestra la consola en vivo de tramas JSON enviadas y recibidas. |

---

## 6. Firmware del Nodo Físico (ESP32)

El nodo sensor **ya no requiere PC**: existe firmware nativo para ESP32 en
`../firmware/` (mismo protocolo TCP :8080, telemetría JSON a 2 Hz y comandos
`SET_RELAY` idénticos a `tools/sensor_node_simulator.py`).

- Proyecto PlatformIO (`src/main.cpp`, monolito, sin librerías externas)
- Modo sensor real MPU6050 (I2C 21/22) con fallback automático a modo sintético
- Relé activo-bajo en GPIO 26 · consola serial `a/n/r/s/h`
- Ver **`../firmware/README.md`** para cableado, flasheo y `probar_nodo.py`
