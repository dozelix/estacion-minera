# Estación Minera TI3042 — Supervisión de Motores (IoT)

**Evaluación Sumativa 2 (ES2) — Asignatura TI3042: Aplicaciones Móviles para IoT / Sistemas y Redes Industriales**  
**Proyecto:** Estación Móvil de Supervisión y Control para Motores Mineros (Chancado y Molienda) con Disparo de Relé por Vibración ISO 10816-3.

---

## 1. Descripción del Sistema

La **Estación Minera TI3042** es una estación de supervisión de alta fiabilidad para activos mecánicos críticos en faena minera (molinos SAG, chancadores y motores de pulpa). Monitorea telemetría física procedente de nodos sensores basados en el sensor inercial **MPU6050** y comanda el relé electromecánico de desenergización (GPIO 26) ante eventos de sobrevibración destructiva según la norma internacional **ISO 10816-3**.

---

## 2. Características Principales Portadas

1. **Autenticación Cifrada Segura (ISO 27400):**
   - Emulación de almacenamiento criptográfico con AES-256 GCM (`EncryptedAuthStore`).
   - Botón de carga rápida de credenciales de evaluación (`operador@faena.cl` / `MineraSegura2026!`).
   - Gestión de perfiles y roles de supervisión (Supervisor de Mantenimiento, Operador de Faena).

2. **Panel de Mando y Supervisión en Tiempo Real:**
   - **Identificación Inequívoca:** Rol visible permanente en barra superior (`[ ROL: ESTACIÓN DE SUPERVISIÓN ]`).
   - **Placa de Control de Relé:** Indicador de estado del relé (CERRADO / OPERATIVO vs. ABIERTO / DISPARADO), botón de **Parada de Emergencia** con diálogo de confirmación de seguridad y auditoría de operador, y botón de **Rearme de Circuito**.
   - **Disparo Preventivo Automático (Auto-Trip):** Apertura inmediata del relé ante detección sostenida de vibración en Zona D (&gt; 7.1 mm/s).

3. **Monitoreo de Vibración RMS (Norma ISO 10816-3):**
   - **Osciloscopio en Vivo en Canvas:** Trazo de forma de onda, sombreado de gradiente, líneas guía de umbrales a 4.5 mm/s (Zona C: Alerta) y 7.1 mm/s (Zona D: Crítica) con etiquetas milimétricamente alineadas sobre panel HMI oscuro (`ScopePanel`).
   - **Inyector Interactivo de Falla:** Alterna entre operación nominal de faena y modo de anomalía crítica de vibración.
   - **Doble Codificación Accesible:** Todos los estados presentan color + icono + texto.

4. **Telemetría Física Completa del Motor:**
   - Temperatura de devanado en °C con barra de nivel.
   - Tacómetro de velocidad rotacional en RPM (1485 RPM nominal, 0 RPM tras parada).
   - Acelerometría triaxial MPU-6050 ($A_x, A_y, A_z$ en $g$).

5. **Consola de Tramas `>_` y Configuración de Red:**
   - Visualizador de tramas JSON delimitadas por `\n` recibidas (RX), comandos enviados (TX) y eventos de sistema (SYS).
   - Modos de enlace configurables: Simulador Interno MPU6050 a 2 Hz o Socket TCP en vivo (ESP32 / Python `tools/sensor_node_simulator.py` en puerto 8080).
   - Resiliencia ante microcortes con reconexión automática configurable.
   - Selector de vista: **Vista Móvil Android** (con marco de dispositivo móvil, barra de estado y navegación) o **Vista Estación HMI** (pantalla completa).

---

## 3. Estructura del Proyecto

```
IOT_E2/
├── src/                          ← Implementación Web/React en AI Studio
│   ├── components/               (Osciloscopio Canvas, StatusBadge, TerminalModal, ConnectionModal, DeviceFrame)
│   ├── screens/                  (LoginScreen, MonitoringScreen)
│   ├── services/                 (authStore ISO 27400, sensorSimulator, tcpClient)
│   ├── theme/                    (tokens industriales, colores de estado semánticos)
│   └── types/                    (contratos de telemetría y tramas JSON)
├── estacion-minera/              ← Código fuente Android nativo (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/com/example/
│   │   ├── ui/                   (LoginScreen.kt, MonitoringScreen.kt)
│   │   ├── network/              (TcpSocketClient.kt, SensorSimulator.kt)
│   │   ├── model/                (TelemetryModels.kt)
│   │   └── security/             (EncryptedAuthStore.kt)
│   └── tools/sensor_node_simulator.py  ← Simulador TCP de nodo sensor en Python
└── firmware/                     ← Firmware ESP32 PlatformIO (src/main.cpp monolito)
```

---

## 4. Ejecución

- **Servidor de desarrollo:** `npm run dev` (escucha en `0.0.0.0:3000`)
- **Compilación de producción:** `npm run build`
