#!/usr/bin/env python3
"""
=============================================================================
SISTEMA IoT MINERO - NODO SENSOR Y RELÉ INDUSTRIAL EMULADO (TI3042 - ES2)
=============================================================================
Script servidor TCP multi-hilo para pruebas de integración y demostración en red.
Emula un nodo sensor basado en ESP32 / Raspberry Pi conectado a un sensor MPU6050
y a un módulo de relé electromecánico de corte de alimentación de motor minero.

Cumple con la Pauta de Evaluación Sumativa 2 (TI3042):
- Envío continuo de telemetría JSON delimitada por newline (\n).
- Recepción de comandos de apertura y rearme de relé desde la app Android.
- Inyección manual de anomalías de vibración para verificar disparo preventivo.

Uso:
    python3 sensor_node_simulator.py [--host 0.0.0.0] [--port 8080]
=============================================================================
"""

import socket
import threading
import json
import time
import math
import random
import sys
import argparse

# Colores ANSI para terminal
CLR_CYAN = "\033[96m"
CLR_GREEN = "\033[92m"
CLR_YELLOW = "\033[93m"
CLR_RED = "\033[91m"
CLR_BOLD = "\033[1m"
CLR_RESET = "\033[0m"

class IndustrialSensorNode:
    def __init__(self, host="0.0.0.0", port=8080):
        self.host = host
        self.port = port
        self.server_socket = None
        self.running = True
        
        # Estado del motor y relé
        self.relay_state = "CLOSED"  # CLOSED = Motor encendido, OPEN = Desconectado/Disparado
        self.anomaly_active = False  # Modo falla de rodamiento / desbalance crítico
        self.step = 0
        self.lock = threading.Lock()

    def get_local_ip(self):
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"

    def generate_telemetry_packet(self):
        with self.lock:
            self.step += 1
            t = self.step
            is_closed = (self.relay_state == "CLOSED")

            if not is_closed:
                # Motor desconectado tras disparo de relé
                vibration_rms = round(0.12 + random.uniform(0.01, 0.08), 2)
                rpm = 0
                temp_c = round(max(24.0, 48.0 - (t % 20) * 0.4), 1)
                ax = 0.02
                ay = 0.03
                az = 9.81
                gx = 0.0
                gy = 0.0
                gz = 0.0
            elif self.anomaly_active:
                # Condición crítica de vibración ISO 10816 Zona D (> 7.1 mm/s)
                vibration_rms = round(7.8 + (math.sin(t * 0.5) * 1.6) + random.uniform(0.2, 0.9), 2)
                rpm = 1420 + random.randint(-40, 40)
                temp_c = round(min(88.0, 68.0 + (t % 15) * 0.5), 1)
                ax = round(math.sin(t * 0.8) * vibration_rms * 0.3, 2)
                ay = round(math.cos(t * 0.5) * vibration_rms * 0.35, 2)
                az = round(9.81 + math.sin(t * 1.1) * vibration_rms * 0.4, 2)
                gx = round(math.sin(t * 0.6) * 0.3, 2)
                gy = round(math.cos(t * 0.4) * 0.3, 2)
                gz = round(math.sin(t * 0.9) * 0.4, 2)
            else:
                # Operación normal de faena (ISO 10816 Zona A/B < 4.5 mm/s)
                vibration_rms = round(2.3 + (math.sin(t * 0.2) * 0.8) + random.uniform(-0.2, 0.3), 2)
                rpm = 1485 + random.randint(-8, 8)
                temp_c = round(49.0 + (math.sin(t * 0.05) * 1.5), 1)
                ax = round(math.sin(t * 0.8) * 0.4, 2)
                ay = round(math.sin(t * 0.5) * 0.5, 2)
                az = round(9.81 + math.sin(t * 1.2) * 0.6, 2)
                gx = round(math.sin(t * 0.3) * 0.04, 2)
                gy = round(math.cos(t * 0.2) * 0.04, 2)
                gz = round(math.sin(t * 0.5) * 0.05, 2)

            packet = {
                "device_id": "MPU6050_MOTOR_01",
                "timestamp": int(time.time()),
                "accel": {"x": ax, "y": ay, "z": az},
                "gyro": {"x": gx, "y": gy, "z": gz},
                "vibration_rms": vibration_rms,
                "temperature_c": temp_c,
                "rpm": rpm,
                "relay_state": self.relay_state
            }
            return packet

    def handle_client(self, client_socket, client_address):
        print(f"{CLR_GREEN}[+] Nueva estación conectada desde: {client_address}{CLR_RESET}")
        client_socket.settimeout(0.5)

        # Hilo de lectura de comandos entrantes desde la estación
        def command_listener():
            buffer = ""
            while self.running:
                try:
                    data = client_socket.recv(1024).decode('utf-8')
                    if not data:
                        break
                    buffer += data
                    while "\n" in buffer:
                        line, buffer = buffer.split("\n", 1)
                        line = line.strip()
                        if line:
                            self.process_command(line)
                except socket.timeout:
                    continue
                except Exception:
                    break

        cmd_thread = threading.Thread(target=command_listener, daemon=True)
        cmd_thread.start()

        # Bucle de emisión de telemetría a 2 Hz (cada 500 ms)
        try:
            while self.running:
                packet = self.generate_telemetry_packet()
                json_data = json.dumps(packet) + "\n"
                client_socket.sendall(json_data.encode('utf-8'))

                status_color = CLR_GREEN if packet["relay_state"] == "CLOSED" else CLR_RED
                sev_tag = "CRÍTICA" if packet["vibration_rms"] >= 7.1 else ("ALERTA" if packet["vibration_rms"] >= 4.5 else "NORMAL")
                
                print(f"[{time.strftime('%H:%M:%S')}] {status_color}[RELÉ: {packet['relay_state']}]{CLR_RESET} "
                      f"Vib: {packet['vibration_rms']} mm/s ({sev_tag}) | "
                      f"Temp: {packet['temperature_c']}°C | "
                      f"RPM: {packet['rpm']}")

                time.sleep(0.5)
        except (BrokenPipeError, ConnectionResetError):
            print(f"{CLR_YELLOW}[!] Estación de supervisión desconectada ({client_address}).{CLR_RESET}")
        except Exception as e:
            print(f"{CLR_RED}[-] Error en transmisión: {e}{CLR_RESET}")
        finally:
            try:
                client_socket.close()
            except Exception:
                pass
            print(f"{CLR_YELLOW}[*] Socket cerrado para {client_address}. Esperando reconexión...{CLR_RESET}")

    def process_command(self, raw_json):
        try:
            cmd = json.loads(raw_json)
            command_type = cmd.get("command")
            target_state = cmd.get("state", "").upper()
            reason = cmd.get("reason", "Sin motivo")
            operator = cmd.get("operator", "Desconocido")

            with self.lock:
                if command_type in ["SET_RELAY", "RELAY_CONTROL"]:
                    if target_state in ["OPEN", "CLOSED"]:
                        self.relay_state = target_state
                        color = CLR_RED if target_state == "OPEN" else CLR_GREEN
                        print(f"\n{CLR_BOLD}{color}>>> [COMANDO RECIBIDO DE LA APP ANDROID] <<<{CLR_RESET}")
                        print(f"    Operador: {operator}")
                        print(f"    Acción: Relé -> {target_state}")
                        print(f"    Motivo: {reason}\n")
        except Exception as e:
            print(f"{CLR_RED}[-] Error procesando comando: {e} ({raw_json}){CLR_RESET}")

    def console_input_loop(self):
        print(f"\n{CLR_BOLD}--- COMANDOS INTERACTIVOS DE TECLADO ---{CLR_RESET}")
        print(" [a] Inyectar ANOMALÍA crítica de vibración (RMS > 7.1 mm/s)")
        print(" [n] Restablecer vibración a estado NORMAL")
        print(" [r] Alternar relé manualmente (OPEN / CLOSED)")
        print(" [q] Salir del simulador\n")

        while self.running:
            try:
                choice = sys.stdin.readline().strip().lower()
                if choice == 'a':
                    with self.lock:
                        self.anomaly_active = True
                    print(f"{CLR_RED}[!] ANOMALÍA DE VIBRACIÓN ACTIVADA. Espere disparo de relé en la app.{CLR_RESET}")
                elif choice == 'n':
                    with self.lock:
                        self.anomaly_active = False
                    print(f"{CLR_GREEN}[*] Vibración restablecida a rango seguro.{CLR_RESET}")
                elif choice == 'r':
                    with self.lock:
                        self.relay_state = "OPEN" if self.relay_state == "CLOSED" else "CLOSED"
                    print(f"{CLR_YELLOW}[*] Relé alternado manualmente a: {self.relay_state}{CLR_RESET}")
                elif choice == 'q':
                    print("Cerrando simulador de nodo sensor...")
                    self.running = False
                    break
            except (KeyboardInterrupt, EOFError):
                self.running = False
                break

    def start(self):
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.bind((self.host, self.port))
        self.server_socket.listen(5)

        local_ip = self.get_local_ip()
        print(f"\n{CLR_BOLD}{CLR_CYAN}==================================================================={CLR_RESET}")
        print(f"{CLR_BOLD}  NODO SENSOR IoT (MPU6050 + RELÉ INDUSTRIAL) INICIADO  {CLR_RESET}")
        print(f"{CLR_BOLD}{CLR_CYAN}==================================================================={CLR_RESET}")
        print(f" Escuchando en:")
        print(f"   • Host local:       {self.host}:{self.port}")
        print(f"   • IP de Red LAN:    {local_ip}:{self.port}")
        print(f"   • Emulador Android: 10.0.2.2:{self.port} (dirección por defecto)")
        print(f"{CLR_CYAN}-------------------------------------------------------------------{CLR_RESET}")

        # Hilo de entrada de consola
        input_thread = threading.Thread(target=self.console_input_loop, daemon=True)
        input_thread.start()

        # Bucle de aceptación de clientes
        try:
            while self.running:
                self.server_socket.settimeout(1.0)
                try:
                    client_sock, client_addr = self.server_socket.accept()
                    client_thread = threading.Thread(
                        target=self.handle_client,
                        args=(client_sock, client_addr),
                        daemon=True
                    )
                    client_thread.start()
                except socket.timeout:
                    continue
        except KeyboardInterrupt:
            print("\nInterrupción detectada. Cerrando servidor...")
        finally:
            self.running = False
            if self.server_socket:
                self.server_socket.close()
            print(f"{CLR_GREEN}Servidor TCP detenido.{CLR_RESET}")

def main():
    parser = argparse.ArgumentParser(description="Simulador de Nodo Sensor MPU6050 y Relé para TI3042")
    parser.add_argument("--host", default="0.0.0.0", help="Dirección host (default 0.0.0.0)")
    parser.add_argument("--port", type=int, default=8080, help="Puerto TCP (default 8080)")
    args = parser.parse_args()

    node = IndustrialSensorNode(host=args.host, port=args.port)
    node.start()

if __name__ == "__main__":
    main()
