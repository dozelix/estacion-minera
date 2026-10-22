# Firmware ESP32 — Nodo Sensor Minero (TI3042 · ES2)

Nodo sensor con **servidor TCP** que reemplaza 1:1 al simulador
`estacion-minera/tools/sensor_node_simulator.py`: la app Android
("Estación Minera") se conecta **como cliente TCP a la IP del ESP32:8080**,
recibe telemetría JSON a 2 Hz y envía comandos `SET_RELAY`.

Un solo archivo: `src/main.cpp` (**librerías externas: ninguna** — solo el core
ESP32: `WiFi`, `Wire`, `ESPmDNS`, `time`). Proyecto **PlatformIO**.

---

## 1. Cableado

### Relé (módulo azul con optoacoplador — ACTIVO BAJA)

| Pin módulo | Conexión en ESP32            | Nota |
| :---       | :---                         | :--- |
| IN         | **GPIO 26**                  | LOW → relé ABIERTO (motor desconectado) |
| VCC        | 5V (VIN)                     | Alimentación del módulo; las variantes con JD-VCC piden 5V, no 3.3V |
| GND        | GND                          | Comun con la placa |

Circuito de potencia: **COM + NO** en serie con la alimentación del motor/LED
carga demostrativa. ⚠ Para la demo en sala se recomienda una lámpara/LED de 12V
o un ventilador USB de 5V (sin 220V en el escritorio).

### MPU6050 (opcional — si falta, el modo sintético cubre la demo)

| Pin MPU6050 | GPIO ESP32 | Nota |
| :---        | :---       | :--- |
| VCC         | 3V3        | |
| GND         | GND        | |
| SDA         | **GPIO 21**| Bus I2C @ 400 kHz |
| SCL         | **GPIO 22**| |
| AD0         | GND        | Dirección I2C `0x68` (HIGH = `0x69`, autodetectado) |

---

## 2. Modos de operación (mismo monolito)

| Modo | Cuándo | Comportamiento |
| :--- | :--- | :--- |
| **Sensor real** | MPU6050 responde `WHO_AM_I=0x68` en el arranque | Lee acelerómetro/giroscopio (40 med/s), calcula RMS de vibración en mm/s (ventana 0,5 s @ 50 Hz) y temperatura del chip |
| **Sintético (prueba)** | No hay MPU6050 o `USE_MPU6050=0` | Reproduce los **mismos patrones del simulador Python**: normal ~2,3 mm/s, anomalía ~7,8–9,5 mm/s, motor detenido ~0,12 mm/s |
| **Anomalía forzada [a]** | Tecla serial, en ambos modos | Inyecta vibración zona D (>7,1 mm/s) para probar el Auto-Trip de la app aunque el MPU esté quieto en el escritorio |

**RPM y `vibration_rms` en modo sensor real**: no hay encoder físico → el RPM
sigue el patrón del Python (1485±8 en marcha). La conversión a mm/s usa
`v = a/(2π·f)·1000` con f ≈ 24 Hz (1450 RPM). Constante ajustable:
`#define VIB_FACTOR 6.63f`.

---

## 3. Compilación y flasheo (PlatformIO / VS Code)

1. **Proyecto**: PlatformIO detecta `platformio.ini` al abrir la carpeta `firmware/`
   en VS Code (extensión PlatformIO IDE instalada). No agregue NINGUNA librería.
2. Abrir `src/main.cpp`.
3. Editar el bloque de configuración (al inicio del archivo):
   ```cpp
   #define WIFI_SSID "TU_RED_WIFI"   // la red de la casa / del aula
   #define WIFI_PASS "TU_PASSWORD"
   ```
4. Entorno por defecto: `[env:esp32dev]` (placa ESP32 DevKit V1). Flashear con
   el botón **Upload** de PlatformIO o terminal:
   ```bash
   cd firmware
   pio run -t upload          # compila y flashea (puerto autodetectado)
   pio device monitor -b 115200   # consola serial: banner + teclas a/n/r/s/h
   ```
5. Al arrancar, el banner serial imprime la **IP LAN del nodo**:
   ```
   Escuchando en:
     • IP LAN Wi-Fi:     192.168.1.37:8080
     • DNS local mDNS:   nodo-minero.local:8080
     • Modo sensor:      SINTÉTICO (prueba sin hardware)
   ```
6. La app Android: Configuración (engranaje) → IP del nodo → `192.168.1.37`,
   puerto `8080` → **Guardar y Conectar**.

> **Red de la clase sin credenciales**: el nodo hace fallback a hotspot propio
> `ESTACION-MINERA-NODO / minerasegura2026`; conecta el teléfono a ese hotspot
> y usa la IP `192.168.4.1:8080`.

---

## 4. Teclas de consola serial (paridad con el simulador Python)

| Tecla | Acción | Equivalente en Python |
| :--- | :--- | :--- |
| `a` | Inyectar anomalía crítica (> 7,1 mm/s) | `a` |
| `n` | Restablecer vibración normal | `n` |
| `r` | Alternar relé a mano | `r` |
| `s` | Imprimir estado del nodo | — (extra) |
| `h` | Ayuda / banner | — (extra) |

---

## 5. Cliente de prueba desde PC (sin app Android)

`probar_nodo.py` conecta al nodo como haría la app y verifica el ciclo bidireccional:

```bash
python3 probar_nodo.py --host 192.168.1.37 --port 8080
```

- Telemetría impresa en vivo con severidad ISO 10816
- Teclas: `o` abre relé (`SET_RELAY OPEN`), `c` rearmar (`CLOSED`), `q` salir

---

## 6. Tramas (paridad con el README principal de la app)

**Telemetría (nodo → app, cada 500 ms, JSON + `\n`)** — idéntica al simulador:

```json
{"device_id":"MPU6050_MOTOR_01","timestamp":1761148800,"accel":{"x":0.12,"y":0.45,"z":9.81},"gyro":{"x":0.01,"y":0.02,"z":0.05},"vibration_rms":2.45,"temperature_c":54.2,"rpm":1480,"relay_state":"CLOSED"}
```

**Comando (app → nodo, JSON + `\n`)**:

```json
{"command":"SET_RELAY","state":"OPEN","reason":"MANUAL_COMMAND","operator":"operador@faena.cl","timestamp":1761148805}
```

- Acepta `"command": "SET_RELAY"` y su alias `"RELAY_CONTROL"`
- `state`: `"OPEN"` (desenergizar) / `"CLOSED"` (rearmar)

---

## 7. Robustez incorporada (criterio A5)

- **Loop sin bloqueos**: `millis()` para muestreo (20 ms) y telemetría (500 ms);
  `delay()` solo en el arranque.
- **Reemplazo de estación**: si el teléfono reconecta (microcorte), la conexión
  vieja se cierra y la nueva toma el control — el nodo nunca queda muerto.
- **TCP sin nodos distantes**: `setNoDelay(true)`, buffer de comandos de 192 B
  con delimitador `\n` (`\r` descartado).
- **Autodetección del MPU6050**: cada arranque intenta I2C; falla → modo
  sintético con los patrones del simulador de referencia.
- **Cierre seguro del relé**: en `setup()` se remune CLOSED (motor ON) — estado
  conocido al reflashear.
