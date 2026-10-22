# AGENTS.md — IOT_E2 (TI3042 · ES2 · Estación Minera)

Proyecto **público en GitHub**: cero secretos, cero tokens, cero keystores en el árbol.

## Identidad

- **Asignatura**: TI3042 — Aplicaciones Móviles para IoT (DUUC · UA2 Aplicaciones Android)
- **Producto**: Estación Móvil de Supervisión de Motores Mineros (chancado/molienda)
  con disparo de relé por vibración ISO 10816-3 (superposición Android + ESP32).
- **Idioma**: todo en español (código, comentarios, commits, docs).

## Estructura

```
IOT_E2/
├── estacion-minera/      ← App Android (Kotlin + Jetpack Compose; origen AI Studio)
│   ├── app/src/main/java/com/example/
│   │   ├── ui/            (LoginScreen, MonitoringScreen, components/, theme/)
│   │   ├── network/       (TcpSocketClient — cliente TCP)
│   │   ├── model/         (TelemetryModels — contratos JSON)
│   │   ├── security/      (EncryptedAuthStore)
│   │   └── service/       (MonitoringForegroundService — Doze)
│   └── tools/sensor_node_simulator.py   ← FUENTE DE VERDAD del protocolo
├── firmware/              ← ESP32 PlatformIO (src/main.cpp monolito)
└── TI3042_U2_ES_GUÍA.docx (material del curso — NO se sube al repo)
```

## Normas firmware (decididas, no negociar sin discutir)

- **PlatformIO**, nunca Arduino IDE (`platformio.ini`, `pio run -t upload`).
- **Monolito**: un solo `src/main.cpp`, **sin librerías externas** (solo core:
  WiFi/Wire/ESPmDNS/time — MPU6050 por registros crudos, driver propio).
- **Paridad de protocolo con `sensor_node_simulator.py`**: el JSON de telemetría
  (`device_id`, `timestamp`, `accel`, `gyro`, `vibration_rms`, `temperature_c`,
  `rpm`, `relay_state` @ 2 Hz delimitado por `\n`) y el comando de control
  (`SET_RELAY` + alias `RELAY_CONTROL`) deben mantenerse idénticos al Python
  y a `model/TelemetryModels.kt`. Cualquier cambio del contrato toca los TRES
  lados + ambos README.
- Hardware configurable solo en el bloque "1. CONFIGURACIÓN": relé GPIO 26
  **ACTIVO LOW** (módulo azul optoacoplado), MPU6050 I2C 21/22.
- Si el MPU6050 no responde `WHO_AM_I` → modo sintético automático con las
  MISMAS fórmulas del Python (la demo nunca se muere sin hardware).
- La app Android es **cliente TCP**; el nodo (ESP32 o Python) es servidor :8080.
  Nueva conexión reemplaza la anterior (robustez ante microcortes).

## Normas UI Android

- **Tokens, no literales**: espaciados/alturas en `theme/Dimens.kt`, formas en
  `IndustrialShapes`, colores de estado vía `industrialStatusColors()` (tema
  claro/oscuro). Nada de `Color(0xFF…)` disperso en screens.
- **dynamicColor = false**: la identidad industrial ámbar/slate no la pisa
  Material You. El tema sigue al sistema (claro + oscuro ambos pulidos).
- **Instrumentos SIEMPRE oscuros**: consola de tramas (`ConsoleBg`) y panel del
  osciloscopio (`Scope*`) son HMI oscuros en ambos temas por decisión de diseño.
- **Doble codificación accesible**: todo estado = color + icono + texto.
- Toques ≥ 48 dp (botones 52/56, diálogos 50/56). `Dimens.TouchMin` es el mínimo.
- Insets: MainActivity NO aplica systemBars padding (los screens manejan lo
  propio: Login `windowInsetsPadding(systemBars)`, Monitoring `Scaffold`).
- Los `testTag` existentes necesarios para QA no se renombran.

## Compilación (gotchas)

- Esta máquina (Celeron/4 GB) solo EDITA: **no hay Android Studio instalado**
  — los builds de la app se hacen en **Google AI Studio** (flujo actual del
  usuario); ESP32 con `pio run` si hay toolchain, si no en otra máquina.
- **`debug.keystore` no existe** (el `build.gradle.kts` de AI Studio lo
  referencia y NO hay que tocar los signingConfigs). Para compilar debug
  local generar antes:
  ```bash
  keytool -genkeypair -v -keystore debug.keystore -storepass android \
    -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  ```
- `.env` solo placeholder comentado (`GEMINI_API_KEY` sin usar) — no descomentar.

## Repo público (postura de seguridad)

- `estacion-minera/.gitignore` ya cubre `.env` y `debug.keystore`; completar
  `*.jks` si apareciere una keystore de release. NUNCA commit `.env` real.
- Las credenciales demo (`operador@faena.cl`) son by-design de la demo del
  curso (se rellenan desde el botón de UI y viven cifradas en
  EncryptedSharedPreferences); NO son secretos de producción.
- Firmware solo con placeholders (`TU_RED_WIFI`); hotspot de emergencia
  `ESTACION-MINERA-NODO` es intencional para la demo en sala.
- Cualquier valor sensible nuevo va a variables de entorno o `.env` (ignorado),
  jamás al código.

## QA (estaciones de revisión — qué demostrar)

1. Login cifrado (ISO 27400 / EncryptedSharedPreferences)
2. Permisos runtime sin crash ante denegación (notificaciones Android 13+)
3. Comunicación en vivo dos dispositivos + rol visible ("ROL: ESTACIÓN…")
4. Robustez: desconexión forzada + rotación: el socket vive en el ViewModel
   (nada de bloquear el hilo principal; red en Dispatchers.IO)
5. Consola de tramas `>_` para inspección de JSON bidireccional
