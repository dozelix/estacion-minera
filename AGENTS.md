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
├── app/ + gradle/ + build.gradle.kts + settings.gradle.kts
│   ← App Android (Kotlin + Jetpack Compose) consolidada EN LA RAÍZ (única copia)
│   ├── app/src/main/java/com/example/
│   │   ├── ui/            (LoginScreen, MonitoringScreen, components/, theme/)
│   │   ├── network/       (TcpSocketClient — cliente TCP)
│   │   ├── model/         (TelemetryModels — contratos JSON)
│   │   ├── security/      (EncryptedAuthStore)
│   │   └── service/       (MonitoringForegroundService — Doze)
├── tools/sensor_node_simulator.py   ← FUENTE DE VERDAD del protocolo
├── firmware/              ← ESP32 PlatformIO (src/main.cpp monolito)
├── docs/img/              ← Capturas del APK (incluidas en README)
├── docs/README-ANDROID.md ← Documento de diseño del APK (problemática/arquitectura)
└── TI3042_U2_ES_GUÍA.docx (material del curso — NO se sube al repo)
```

**Única copia (22-oct-2026)**: la carpeta histórica `estacion-minera/` y el
applet React de AI Studio fueron eliminados del repo (referencias
recuperables del git history). El código Android editable vive SOLO en la
raíz (`app/src/main/java/com/example/`).

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

- **Compilación**: esta máquina (Celeron/4 GB) **EDITA e INSTALA**: no hay
  Android Studio ni SDK. El APK se compila en **Google AI Studio** (proyecto
  nativo importado del repo) y se descarga; instalación local con
  `adb install -r <apk>` (adb presente). Alternativa: Android Studio en otra
  máquina. El repo no lleva `gradlew`/wrapper jar: build CLI local requiere
  Gradle instalado; ESP32 con `pio run` si hay toolchain.
- **`debug.keystore` no existe** (el `build.gradle.kts` de AI Studio lo
  referencia y NO hay que tocar los signingConfigs). Para compilar debug
  local generar antes:
  ```bash
  keytool -genkeypair -v -keystore debug.keystore -storepass android \
    -keypass android -alias androiddebugkey -keyalg RSA -keysize 2048 \
    -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  ```
- Sin `.env` en el árbol (eliminado al consolidar): repo público, sin
  secretos; `GEMINI_API_KEY` nunca se usa.

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
