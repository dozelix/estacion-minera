#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
prueba_nodo.py — Cliente de prueba para el firmware ESP32 del nodo sensor minero
(TI3042 · ES2). Conecta al servidor TCP del ESP32 igual que lo hace la app
Android "Estación Minera" y verifica el ciclo bidireccional sin necesidad de
la app:

    telemetría JSON del nodo → impresión en vivo con severidad ISO 10816-3
    comandos SET_RELAY       → teclas [o] abrir relé · [c] rearmar · [q] salir

Uso:
    python3 probar_nodo.py --host 192.168.1.37 --port 8080
"""

import argparse
import json
import socket
import sys
import threading
import time

CLR_R = "\033[91m"; CLR_G = "\033[92m"; CLR_Y = "\033[93m"
CLR_C = "\033[96m"; CLR_B = "\033[1m"; CLR_0 = "\033[0m"

def clasificar(rms: float) -> str:
    if rms >= 7.1:
        return f"{CLR_R}CRÍTICA{CLR_0}"
    if rms >= 4.5:
        return f"{CLR_Y}ALERTA{CLR_0}"
    return f"{CLR_G}NORMAL{CLR_0}"

def oyente(sock: socket.socket) -> None:
    buffer = ""
    while True:
        try:
            dato = sock.recv(4096).decode("utf-8")
        except OSError:
            print(f"\n{CLR_Y}[!] El nodo cerró la conexión.{CLR_0}")
            return
        if not dato:
            print(f"\n{CLR_Y}[!] El nodo cerró la conexión.{CLR_0}")
            return
        buffer += dato
        while "\n" in buffer:
            linea, buffer = buffer.split("\n", 1)
            if not linea.strip():
                continue
            try:
                p = json.loads(linea)
            except json.JSONDecodeError:
                print(f"\n[-] Trama no-JSON: {linea[:120]}")
                continue
            color = CLR_G if p.get("relay_state") == "CLOSED" else CLR_R
            print(f"{CLR_C}{time.strftime('%H:%M:%S')}{CLR_0} {color}[RELÉ: {p.get('relay_state')}]{CLR_0} "
                  f"Vib: {p.get('vibration_rms')} mm/s ({clasificar(p.get('vibration_rms', 0))}) | "
                  f"Temp: {p.get('temperature_c')}°C | RPM: {p.get('rpm')} | "
                  f"a=({p['accel']['x']},{p['accel']['y']},{p['accel']['z']})")

def main() -> None:
    parser = argparse.ArgumentParser(description="Cliente de prueba del nodo ESP32 (TI3042)")
    parser.add_argument("--host", required=True, help="IP del ESP32 (ver banner serial del nodo)")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()

    sock = socket.create_connection((args.host, args.port), timeout=5)
    sock.settimeout(None)
    print(f"{CLR_B}[+] Conectado al nodo sensor en {args.host}:{args.port}{CLR_0}")
    print("    Teclas: [o] abrir relé (SET_RELAY OPEN) · [c] rearmar (CLOSED) · [q] salir\n")

    threading.Thread(target=oyente, args=(sock,), daemon=True).start()

    while True:
        tecla = sys.stdin.readline().strip().lower()
        if tecla == "o":
            cmd = {"command": "SET_RELAY", "state": "OPEN",
                   "reason": "PRUEBA_CLIENTE_PC", "operator": "prueba@pc.cl",
                   "timestamp": int(time.time())}
        elif tecla == "c":
            cmd = {"command": "SET_RELAY", "state": "CLOSED",
                   "reason": "PRUEBA_CLIENTE_PC", "operator": "prueba@pc.cl",
                   "timestamp": int(time.time())}
        elif tecla == "q":
            break
        else:
            continue
        try:
            sock.sendall((json.dumps(cmd) + "\n").encode("utf-8"))
            print(f"{CLR_Y}[→] Enviado: {cmd}{CLR_0}")
        except OSError as e:
            print(f"{CLR_R}[-] Error de envío: {e}{CLR_0}")
            return

    sock.close()
    print("[*] Cerrado.")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        pass
