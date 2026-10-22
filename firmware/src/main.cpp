/*
 * =============================================================================
 * TI3042 — ES2 · NODO SENSOR MINERO "ESTACIÓN MINERA" (FIRMWARE MONOLÍTICO)
 * =============================================================================
 * Placa: ESP32 (DevKit V1 o compatible) · Librerías externas: NINGUNA
 *
 * Paridad total con el simulador de referencia  tools/sensor_node_simulator.py:
 *   - Servidor TCP :8080   → la app Android conecta como cliente (igual que al PC)
 *   - Telemetría JSON delimitada por '\n' a 2 Hz (500 ms) con los MISMOS campos
 *   - Comandos entrantes: {"command":"SET_RELAY","state":"OPEN"|"CLOSED",...}
 *     (acepta además el alias "RELAY_CONTROL", igual que el simulador Python)
 *   - Consola serial con las MISMAS teclas de prueba del simulador:
 *       [a] inyectar ANOMALÍA crítica (> 7.1 mm/s)  · [n] restablecer normal
 *       [r] alternar relé manualmente               · [s] estado + [h] ayuda
 *
 * Modos (MONOLITO único, mismo archivo):
 *   1) MODO SENSOR REAL  → si hay MPU6050 en I2C responde WHO_AM_I=0x68:
 *        lee acelerómetro/giroscopio con registros crudos (Wire) y calcula
 *        vibración RMS en mm/s sobre una ventana de 25 muestras (0.5 s @ 50 Hz).
 *   2) MODO SINTÉTICO (prueba) → si NO hay MPU6050 (o USE_MPU6050=0), genera los
 *        mismos patrones matemáticos del simulador Python: normal ~2.3±0.8,
 *        anomalía ~7.8+1.6·sin(t), motor desconectado ~0.12. La demo NUNCA
 *        muere por falta de hardware.
 *   El modo se autodetecta en el arranque (sensorReal) y se informa por Serial.
 *   Una anomalía forzada por la tecla [a] funciona en AMBOS modos.
 *
 * Cableado (ver README.md del firmware):
 *   MPU6050: VCC→3V3 · GND→GND · SDA→GPIO21 · SCL→GPIO22 · AD0→GND (0x68)
 *   Relé:    IN→GPIO26 · VCC→5V · GND→GND · COM/NO en serie al circuito del motor
 * =============================================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <WiFiServer.h>
#include <WiFiClient.h>
#include <Wire.h>
#include <ESPmDNS.h>
#include <time.h>

/* =============================================================================
 * 1. CONFIGURACIÓN — EDITAR SOLO ESTE BLOQUE
 * ========================================================================== */
// --- Red Wi-Fi de la casa / faena (la app Android debe estar en la misma red)
#define WIFI_SSID            "TU_RED_WIFI"      // <-- COMPLETAR ANTES DE FLASHEAR
#define WIFI_PASS            "TU_PASSWORD"      // <-- COMPLETAR ANTES DE FLASHEAR
#define INTENTOS_WIFI        5                  // reintentos STA antes del SoftAP

// --- Servidor TCP (paridad con el simulador y con la app)
#define TCP_PORT             8080
#define PERIODO_TELEMETRIA_MS 500              // 2 Hz, igual que el Python
#define PERIODO_MUESTREO_MS   20               // 50 Hz de muestreo acelerómetro (modo real)
#define HOSTNAME_MDNS        "nodo-minero"     // DNS local: nodo-minero.local

// --- Relé industrial (módulo azul típico con optoacoplador: ACTIVO LOW)
#define RELAY_PIN            26
#define RELAY_OPEN_LEVEL     LOW               // GPIO LOW  → relé ABIERTO (motor OFF)
#define RELAY_CLOSE_LEVEL    HIGH              // GPIO HIGH → relé CERRADO (motor ON)
#define NOMBRE_DISPOSITIVO   "MPU6050_MOTOR_01" // device_id del JSON (no cambiar)

// --- MPU6050 (sensor real opcional; si falta cae en modo sintético solos)
#define USE_MPU6050          1                 // 1 = detectar y usar si responde · 0 = siempre sintético
#define I2C_SDA              21
#define I2C_SCL              22
#define MPU_RANGO_G          16.0f             // escala configurada ±16 g
#define VIB_FACTOR           6.63f             // mm/s por (m/s²) a ~24 Hz (1450 RPM):
                                               // v = a/(2π·f)·1000 → 1000/(2π·24) ≈ 6.63

// --- Umbrales ISO 10816-3 (los mismos que evalúa la app; informativos acá)
#define UMBRAL_ALERTA        4.5f              // mm/s — zona C
#define UMBRAL_CRITICO       7.1f              // mm/s — zona D

/* =============================================================================
 * 2. REGISTROS MPU6050 (datasheet InvenSense MPU-6000/MPU-6050 Register Map)
 * ========================================================================== */
#define MPU_WHO_AM_I         0x75   // módulos GY-521 responden 0x68
#define MPU_PWR_MGMT_1       0x6B   // al escribir 0x00 despierta el sensor
#define MPU_ACCEL_XOUT_H     0x3B   // ráfaga de 14 bytes: ACCEL[6] TEMP[2] GYRO[6]

/* =============================================================================
 * 3. ESTADO GLOBAL DEL NODO
 * ========================================================================== */
WiFiServer servidor(TCP_PORT);
WiFiClient  cliente;          // una sola estación conectada a la vez (la nueva reemplaza)
char bufferComando[192];
uint8_t     lenComando = 0;

char estadoRele[8]        = "CLOSED";  // "CLOSED" = motor ON · "OPEN" = disparado
bool anomaliaActiva       = false;     // equivalente del self.anomaly_active del Python
bool sensorReal           = false;     // autodetectado: MPU6050 presente en I2C
uint8_t direccionMPU      = 0;         // dirección I2C efectiva del MPU

uint32_t millisTelemetria = 0;         // temporizador de emisión (500 ms)
uint32_t millisMuestra    = 0;         // temporizador de muestreo real (20 ms)

// Ventana circular de vibración RMS (25 muestras @ 50 Hz = 0,5 s)
#define TAM_VENTANA 25
float   ventanaVib[TAM_VENTANA];
uint8_t idxVentana = 0;
uint8_t nVentana   = 0;

int64_t pasoSintetico = 0;             // equivalente del self.step del Python

/* =============================================================================
 * 4. UTILIDADES DE TIEMPO (epoch real NTP con fallback, y hora de log)
 * ========================================================================== */
int64_t epochActual() {
  time_t ahora = time(nullptr);
  if (ahora > 1700000000) return (int64_t)ahora;              // sincronizado vía NTP
  return 1729512000LL + (int64_t)(millis() / 1000);           // fallback: emula una época
}

static void horaLog(void) {
  // Imprime "[HH:MM:SS]" al estilo del simulador Python
  time_t ahora = time(nullptr);
  char b[12];
  if (ahora > 1700000000) {
    struct tm ts;
    localtime_r(&ahora, &ts);
    strftime(b, sizeof(b), "%H:%M:%S", &ts);
  } else {
    uint32_t s = (uint32_t)((millis() / 1000) % 86400ULL);
    snprintf(b, sizeof(b), "%02u:%02u:%02u",
             (unsigned)(s / 3600), (unsigned)((s / 60) % 60), (unsigned)(s % 60));
  }
  Serial.printf("[%s]", b);
}

/* =============================================================================
 * 5. MPU6050 — DRIVER MINIMALISTA POR I2C (Wire, sin librerías externas)
 * ========================================================================== */
static bool i2cLeer(uint8_t dir, uint8_t reg, uint8_t *datos, uint8_t largo) {
  Wire.beginTransmission(dir);
  Wire.write(reg);
  if (Wire.endTransmission(false) != 0) return false;
  if (Wire.requestFrom((int)dir, (int)largo) != largo) return false;
  for (uint8_t i = 0; i < largo; i++) datos[i] = Wire.read();
  return true;
}

static bool i2cEscribir(uint8_t dir, uint8_t reg, uint8_t valor) {
  Wire.beginTransmission(dir);
  Wire.write(reg);
  Wire.write(valor);
  return Wire.endTransmission() == 0;
}

static bool mpuDetectar(void) {
  const uint8_t candidatas[2] = { 0x68, 0x69 };               // AD0=GND → 0x68 · AD0=HIGH → 0x69
  for (uint8_t i = 0; i < 2; i++) {
    uint8_t who = 0;
    if (i2cLeer(candidatas[i], MPU_WHO_AM_I, &who, 1) && (who == 0x68 || who == 0x69)) {
      direccionMPU = candidatas[i];
      i2cEscribir(direccionMPU, MPU_PWR_MGMT_1, 0x00);        // despertar
      delay(50);
      Serial.printf("[MPU] MPU6050 detectado en 0x%02X → MODO SENSOR REAL\n", direccionMPU);
      return true;
    }
  }
  return false;
}

/* Leer ráfaga de 14 bytes → m/s², °/s y temperatura del chip (°C). false si falla I2C. */
static bool mpuLeer(float *ax, float *ay, float *az,
                    float *gx, float *gy, float *gz, float *chipTemp) {
  uint8_t b[14];
  if (!i2cLeer(direccionMPU, MPU_ACCEL_XOUT_H, b, 14)) return false;

  const float escalaAcc = MPU_RANGO_G / 32768.0f * 9.81f;     // crudo → m/s²
  const float escalaGyr = 2000.0f / 32768.0f;                 // crudo → °/s (±2000 °/s)
  const float crudoT    = (float)(int16_t)(((uint16_t)b[6] << 8) | b[7]);
  *chipTemp = crudoT / 340.0f + 36.53f;                       // fórmula del datasheet

  *ax = (float)(int16_t)(((uint16_t)b[0]  << 8) | b[1])  * escalaAcc;
  *ay = (float)(int16_t)(((uint16_t)b[2]  << 8) | b[3])  * escalaAcc;
  *az = (float)(int16_t)(((uint16_t)b[4]  << 8) | b[5])  * escalaAcc;
  *gx = (float)(int16_t)(((uint16_t)b[8]  << 8) | b[9])  * escalaGyr;
  *gy = (float)(int16_t)(((uint16_t)b[10] << 8) | b[11]) * escalaGyr;
  *gz = (float)(int16_t)(((uint16_t)b[12] << 8) | b[13]) * escalaGyr;
  return true;
}

/* =============================================================================
 * 6. VENTANA DE VIBRACIÓN RMS
 *    Modo real: una muestra cada 20 ms; RMS sobre 0,5 s (paridad de latencia
 *    con el Python). Anomalía forzada: la trama sintética manda sobre el real.
 * ========================================================================== */
static void ventanaReiniciar(void) { idxVentana = 0; nVentana = 0; }

static void ventanaAgregar(float mm_s) {
  ventanaVib[idxVentana] = mm_s;
  idxVentana = (idxVentana + 1) % TAM_VENTANA;
  if (nVentana < TAM_VENTANA) nVentana++;
}

static float ventanaRMS(void) {
  if (nVentana == 0) return 0.0f;
  float suma = 0.0f;
  for (uint8_t i = 0; i < nVentana; i++) suma += ventanaVib[i] * ventanaVib[i];
  return sqrtf(suma / nVentana);
}

/* =============================================================================
 * 7. TELEMETRÍA — PATRONES IDÉNTICOS AL SIMULADOR Python
 *    Fórmulas copiadas de generate_telemetry_packet() del nodo de referencia:
 *      desconectado : 0.12 + U(0.01..0.08) · rpm 0 · temp decae a 24 °C
 *      anomalía     : 7.8 + 1.6·sin(t·0.5) + U(0.2..0.9) · 1420±40 · T asciende a 88 °C
 *      normal       : 2.3 + 0.8·sin(t·0.2) + U(−0.2..0.3) · 1485±8 · T ~49 °C
 * ========================================================================== */
static float uniforme(float minimo, float maximo) {          // U[min,max] con paso 0,01
  return minimo + (float)random(0, 101) / 100.0f * (maximo - minimo);
}

static int enteroConRuido(int base, int amplitud) {          // base ± entero[0..amp]
  return base + random(-amplitud, amplitud + 1);
}

static void generarPaquete(float *vib, float *ax, float *ay, float *az,
                           float *gx, float *gy, float *gz,
                           float *temp, int *rpm) {
  pasoSintetico++;
  const float t = (float)pasoSintetico;
  const bool cerrado = (strcmp(estadoRele, "CLOSED") == 0);
  const bool anomaliaForzada = cerrado && anomaliaActiva;    // anomalía solo en marcha

  if (!cerrado) {                                            // === motor DESconectado ===
    *vib  = 0.12f + uniforme(0.01f, 0.08f);
    *rpm  = 0;
    *temp = max(24.0f, 48.0f - fmodf(t, 20.0f) * 0.4f);
    *ax = 0.02f; *ay = 0.03f; *az = 9.81f;
    *gx = 0.0f;  *gy = 0.0f;  *gz = 0.0f;
  } else if (anomaliaForzada) {                              // === ISO 10816 zona D ===
    *vib  = 7.8f + sinf(t * 0.5f) * 1.6f + uniforme(0.2f, 0.9f);
    *rpm  = enteroConRuido(1420, 40);
    *temp = min(88.0f, 68.0f + fmodf(t, 15.0f) * 0.5f);
    *ax = sinf(t * 0.8f) * *vib * 0.30f;
    *ay = cosf(t * 0.5f) * *vib * 0.35f;
    *az = 9.81f + sinf(t * 1.1f) * *vib * 0.40f;
    *gx = sinf(t * 0.6f) * 0.3f;
    *gy = cosf(t * 0.4f) * 0.3f;
    *gz = sinf(t * 0.9f) * 0.4f;
  } else {                                                   // === normal zona A/B ===
    *vib  = 2.3f + sinf(t * 0.2f) * 0.8f + uniforme(-0.2f, 0.3f);
    *rpm  = enteroConRuido(1485, 8);
    *temp = 49.0f + sinf(t * 0.05f) * 1.5f;
    *ax = sinf(t * 0.8f) * 0.4f;
    *ay = sinf(t * 0.5f) * 0.5f;
    *az = 9.81f + sinf(t * 1.2f) * 0.6f;
    *gx = sinf(t * 0.3f) * 0.04f;
    *gy = cosf(t * 0.2f) * 0.04f;
    *gz = sinf(t * 0.5f) * 0.05f;
  }

  // En modo sensor real los ejes/temperatura/RMS vienen del chip; el RMS sintético
  // de la anomalía forzada [a] manda por sobre la lectura física (para probar el
  // Auto-Trip de la app incluso con motor detenido en el escritorio).
  if (sensorReal) {
    float rx, ry, rz, rgx, rgy, rgz, rtemp;
    if (mpuLeer(&rx, &ry, &rz, &rgx, &rgy, &rgz, &rtemp)) {
      *ax = rx; *ay = ry; *az = rz;
      *gx = rgx; *gy = rgy; *gz = rgz;
      *temp = rtemp;
      if (!anomaliaForzada) *vib = ventanaRMS();             // ventana RMS real
    }
  }
}

/* =============================================================================
 * 8. TRAMA JSON SALIENTE (orden de campos idéntico al del simulador Python)
 * ========================================================================== */
char tramaJson[384];

static void emitirTelemetria(void) {
  float vib, ax, ay, az, gx, gy, gz, temp;
  int rpm;
  generarPaquete(&vib, &ax, &ay, &az, &gx, &gy, &gz, &temp, &rpm);

  snprintf(tramaJson, sizeof(tramaJson),
    "{\"device_id\":\"%s\",\"timestamp\":%lld,"
    "\"accel\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f},"
    "\"gyro\":{\"x\":%.2f,\"y\":%.2f,\"z\":%.2f},"
    "\"vibration_rms\":%.2f,\"temperature_c\":%.1f,\"rpm\":%d,\"relay_state\":\"%s\"}\n",
    NOMBRE_DISPOSITIVO, (long long)epochActual(),
    ax, ay, az, gx, gy, gz, vib, temp, rpm, estadoRele);

  if (cliente && cliente.connected()) {
    cliente.print(tramaJson);                                // '\n' ya incluido
  }

  const char *sev = (vib >= UMBRAL_CRITICO) ? "CRÍTICA" : (vib >= UMBRAL_ALERTA) ? "ALERTA" : "NORMAL";
  horaLog();
  Serial.printf(" [RELÉ: %s] Vib: %.2f mm/s (%s) | Temp: %.1f°C | RPM: %d\n",
                estadoRele, vib, sev, temp, rpm);
}

/* =============================================================================
 * 9. COMANDOS ENTRANTE (TCP) Y RELÉ
 *    Parser JSON minimalista: busca la clave y extrae el valor entre comillas.
 *    Suficiente para las tramas de la app (org.json, sin espacios) y los alias
 *    del protocolo documentado. Los comandos se procesan fuera del hilo de
 *    transmisión (loop único no bloqueante, igual que el listener del Python).
 * ========================================================================== */
static bool extraerCampoJson(const char *json, const char *clave, char *salida, size_t maximo) {
  char patron[24];
  snprintf(patron, sizeof(patron), "\"%s\"", clave);
  const char *base = strstr(json, patron);
  if (!base) return false;
  const char *dosPuntos = strchr(base + strlen(patron), ':');
  if (!dosPuntos) return false;
  const char *comilla = strchr(dosPuntos + 1, '"');          // solo campos de tipo string
  if (!comilla) return false;
  const char *ultima = strchr(comilla + 1, '"');
  if (!ultima) return false;
  size_t largo = (size_t)(ultima - comilla - 1);
  if (largo >= maximo) largo = maximo - 1;
  memcpy(salida, comilla + 1, largo);
  salida[largo] = '\0';
  return true;
}

static void textoAMayusculas(char *s) {
  for (; *s; s++) *s = (char)toupper((unsigned char)*s);
}

static void aplicarRele(bool abrir) {
  strcpy(estadoRele, abrir ? "OPEN" : "CLOSED");
  digitalWrite(RELAY_PIN, abrir ? RELAY_OPEN_LEVEL : RELAY_CLOSE_LEVEL);
  Serial.printf("\n[*] Relé alternado/establecido a: %s\n", estadoRele);
}

static void procesarComando(const char *json) {
  char comando[24] = {0}, estado[16] = {0};
  char operador[64] = "Desconocido";
  char motivo[64]   = "Sin motivo";

  extraerCampoJson(json, "command",  comando,  sizeof(comando));
  extraerCampoJson(json, "state",    estado,   sizeof(estado));
  extraerCampoJson(json, "operator", operador, sizeof(operador));
  extraerCampoJson(json, "reason",   motivo,   sizeof(motivo));
  textoAMayusculas(comando);
  textoAMayusculas(estado);

  if ((strcmp(comando, "SET_RELAY") == 0 || strcmp(comando, "RELAY_CONTROL") == 0) &&
      (strcmp(estado, "OPEN") == 0 || strcmp(estado, "CLOSED") == 0)) {
    Serial.println("\n>>> [COMANDO RECIBIDO DE LA APP ANDROID] <<<");
    Serial.printf("    Operador: %s\n", operador);
    Serial.printf("    Acción:   Relé -> %s\n", estado);
    Serial.printf("    Motivo:   %s\n\n", motivo);
    aplicarRele(strcmp(estado, "OPEN") == 0);
  } else {
    Serial.printf("[-] Error procesando comando o no reconocido: %s\n", json);
  }
}

/* =============================================================================
 * 10. CONSOLA SERIAL — MISMAS TECLAS DE PRUEBA QUE EL SIMULADOR PYTHON
 *      [a] anomalía · [n] normal · [r] alternar relé · [s] estado · [h] ayuda
 * ========================================================================== */
static void bannerSerial(void) {
  Serial.println("\n===================================================================");
  Serial.println("  NODO SENSOR IoT (MPU6050 + RELÉ INDUSTRIAL) INICIADO");
  Serial.println("===================================================================");
  Serial.println(" Escuchando en:");
  Serial.printf("   • IP LAN Wi-Fi:     %s:%d\n", WiFi.localIP().toString().c_str(), TCP_PORT);
  Serial.printf("   • DNS local mDNS:   %s.local:%d\n", HOSTNAME_MDNS, TCP_PORT);
  Serial.printf("   • Modo sensor:      %s\n", sensorReal ? "REAL (MPU6050)" : "SINTÉTICO (prueba sin hardware)");
  Serial.printf("   • Relé GPIO %2d:     abierto cuando el pin va %s\n",
                RELAY_PIN, (RELAY_OPEN_LEVEL == LOW) ? "LOW (módulo activo-bajo)" : "HIGH (módulo activo-alto)");
  Serial.printf("   • Estado actual:    Relé=%s · Anomalía=%s\n",
                estadoRele, anomaliaActiva ? "ACTIVA" : "NO");
  Serial.println("-------------------------------------------------------------------");
  Serial.println(" COMANDOS INTERACTIVOS DE TECLADO (igual que el simulador Python):");
  Serial.println("   [a] Inyectar ANOMALÍA crítica de vibración (RMS > 7.1 mm/s)");
  Serial.println("   [n] Restablecer vibración a estado NORMAL");
  Serial.println("   [r] Alternar relé manualmente (OPEN / CLOSED)");
  Serial.println("   [s] Imprimir estado del nodo   ·   [h] Mostrar esta ayuda");
  Serial.println("===================================================================\n");
}

static void consolaSerial(void) {
  while (Serial.available() > 0) {
    switch ((char)Serial.read()) {
      case 'a':
        anomaliaActiva = true;
        Serial.println("\n[!] ANOMALÍA DE VIBRACIÓN ACTIVADA. Espere disparo de relé en la app.");
        break;
      case 'n':
        anomaliaActiva = false;
        Serial.println("\n[*] Vibración restablecida a rango seguro.");
        break;
      case 'r':
        aplicarRele(strcmp(estadoRele, "CLOSED") == 0);
        break;
      case 's':
        Serial.printf("\n[ESTADO] Relé=%s · Anomalía=%s · Sensor=%s · IP=%s:%d · Estación=%s\n",
                      estadoRele, anomaliaActiva ? "SI" : "NO",
                      sensorReal ? "MPU6050" : "SINTETICO",
                      WiFi.localIP().toString().c_str(), TCP_PORT,
                      (cliente && cliente.connected()) ? "conectada" : "ninguna");
        break;
      case 'h':
        bannerSerial();
        break;
      default: /* ignora \r \n y teclas sin uso */ break;
    }
  }
}

/* =============================================================================
 * 11. Wi-Fi — ESTACIÓN PRINCIPAL + SoftAP DE EMERGENCIA (demo siempre disponible)
 * ========================================================================== */
static bool conectarWifi(void) {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  Serial.printf("[Wi-Fi] Conectando a \"%s\"", WIFI_SSID);
  for (uint8_t intento = 0; intento < INTENTOS_WIFI && WiFi.status() != WL_CONNECTED; intento++) {
    uint32_t t0 = millis();
    while (millis() - t0 < 2000 && WiFi.status() != WL_CONNECTED) {
      delay(100);
      Serial.print(".");
    }
    Serial.printf(" intento %d/%d\n", intento + 1, INTENTOS_WIFI);
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[Wi-Fi] Conectado. IP: %s\n", WiFi.localIP().toString().c_str());
    return true;
  }
  // Red no disponible (aula sin credenciales, outage, etc.): hotspot propio para
  // que la demo en sala siga funcionando: el celular se conecta al SoftAP.
  Serial.println("[Wi-Fi] Sin AP con esas credenciales → SoftAP de emergencia");
  WiFi.mode(WIFI_AP);
  WiFi.softAP("ESTACION-MINERA-NODO", "minerasegura2026");
  Serial.printf("[Wi-Fi] Hotspot estación: ESTACION-MINERA-NODO · IP: %s:%d\n",
                WiFi.softAPIP().toString().c_str(), TCP_PORT);
  Serial.println("        (conecta el celular al hotspot y usa esta IP en la app)");
  return false;
}

/* =============================================================================
 * 12. setup() / loop() — SIN BLOQUEOS, TOLERANTE A DESCONECTAR Y RECONECTAR
 * ========================================================================== */
void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println("\n[FSM] Arrancando nodo sensor minero (firmware monolítico TI3042)");

  pinMode(RELAY_PIN, OUTPUT);
  digitalWrite(RELAY_PIN, RELAY_CLOSE_LEVEL);                 // arranque seguro: motor ON
  Serial.println("[RELÉ] Relé inicializado en CLOSED (motor ON)");

#if USE_MPU6050
  Wire.begin(I2C_SDA, I2C_SCL);
  Wire.setClock(400000);                                      // I2C fast-mode 400 kHz
  sensorReal = mpuDetectar();
  if (!sensorReal) {
    Serial.println("[MPU] Sin MPU6050 en I2C → MODO SINTÉTICO (patrones del simulador Python)");
    Serial.println("      (al conectar un MPU6050 en SDA21/SCL22 este mismo firmware lo adopta)");
  }
#else
  Serial.println("[MPU] USE_MPU6050=0 → MODO SINTÉTICO forzado");
#endif

  if (conectarWifi()) {
    configTime(-3 * 3600, 0, "pool.ntp.org", "time.nist.gov"); // fecha real (fallback interno si falla)
    if (MDNS.begin(HOSTNAME_MDNS)) MDNS.addService("iot-ti3042", "tcp", TCP_PORT);
  }

  servidor.begin(TCP_PORT);
  servidor.setNoDelay(true);
  Serial.printf("[TCP] Servidor listo en puerto %d\n", TCP_PORT);
  bannerSerial();
}

void loop() {
  // --- 1) NUEVA CONEXIÓN ENTRANTE (server.available() solo entrega sockets
  //        pendientes de aceptar: lo que llegue acá es una estación NUEVA; la
  //        estación anterior se cierra para que la demo nunca quede en cuerda
  //        suelta ante un microcorte del Wi-Fi del teléfono) -----------------
  WiFiClient nuevoCliente = servidor.available();
  if (nuevoCliente && nuevoCliente.connected()) {
    if (cliente && cliente.connected()) {
      Serial.println("[*] Cerrando estación anterior para dar paso a la nueva conexión");
      cliente.stop();
    }
    ventanaReiniciar();
    lenComando = 0;
    cliente = nuevoCliente;
    Serial.printf("[+] Nueva estación conectada desde: %s\n",
                  cliente.remoteIP().toString().c_str());
  }

  // --- 2) MUESTREO del acelerómetro real a 50 Hz (solo modo REAL) -----------
  if (sensorReal && millis() - millisMuestra >= PERIODO_MUESTREO_MS) {
    millisMuestra = millis();
    float rx, ry, rz, rgx, rgy, rgz, rtemp;
    if (mpuLeer(&rx, &ry, &rz, &rgx, &rgy, &rgz, &rtemp)) {
      float mag = sqrtf(rx * rx + ry * ry + rz * rz);
      ventanaAgregar(fabsf(mag - 9.81f) * VIB_FACTOR);        // desviación de |a| → mm/s
    }
  }

  // --- 3) TELEMETRÍA a 2 Hz al cliente conectado (temporizador no bloqueante)
  if (cliente && cliente.connected()) {
    if (millis() - millisTelemetria >= PERIODO_TELEMETRIA_MS) {
      millisTelemetria = millis();
      emitirTelemetria();
    }
  }

  // --- 4) COMANDOS ENTRANTE DE LA ESTACIÓN (delimitados por '\n') -----------
  while (cliente && cliente.available() > 0) {
    char c = (char)cliente.read();
    if (c == '\n') {
      bufferComando[lenComando] = '\0';
      procesarComando(bufferComando);
      lenComando = 0;
    } else if (c != '\r' && lenComando < sizeof(bufferComando) - 1) {
      bufferComando[lenComando++] = c;
    }
  }

  // --- 5) DETECCIÓN DE DESCONEXIÓN (corte de Wi-Fi o cierre de la app) ------
  if (cliente && !cliente.connected()) {
    Serial.println("[!] Estación de supervisión desconectada. Esperando reconexión...");
    cliente.stop();
    lenComando = 0;
  }

  // --- 6) CONSOLA SERIAL DE PRUEBA (paridad con el simulador Python) --------
  consolaSerial();
}
