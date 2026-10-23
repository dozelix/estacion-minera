# Marco Simulador Android Predeterminado — Estación Minera TI3042

Configuración de la vista web interactiva para renderizar de manera predeterminada un **marco de smartphone Android realista** con barra de estado del sistema, navegación por gestos y diseño Material 3 industrial, resolviendo la visualización de la app en entornos AI Studio Dynamic mientras se preserva el código fuente nativo en `estacion-minera/`.

---

## Decisiones Críticas y Confirmación del Usuario

> [!IMPORTANT]
> **Decisiones confirmadas por el usuario en la fase de aclaración:**
> - **Entorno de interacción:** Utilizar el **marco simulador Android en la vista web** interactiva para obtener retroalimentación visual inmediata sin tiempos muertos de compilación de emulador pesado.
> - **Modo de visualización predeterminado:** **Vista móvil vertical con marco de smartphone** (ratio 9:19.5, 390×844 px) con marco físico de dispositivo, cámara frontal perforada, barra de estado de Android (reloj en tiempo real, Wi-Fi, batería, señal móvil) y barra de gestos inferior.

---

## 1. Visión General del Producto

### ¿Qué resuelve este ajuste?
Al importar el repositorio `dozelix/estacion-minera` desde GitHub, AI Studio provisionó el contenedor en el entorno web dinámico (`GOOGLE_RUNTIME=nodejs22`). Al configurar el componente `DeviceFrame` para que inicie de manera predeterminada en **Vista Móvil Android**:
1. El usuario visualiza de inmediato la aplicación dentro de la silueta de un teléfono Android en el panel de vista previa de AI Studio.
2. Se mantiene la capacidad de alternar en cualquier momento a la **Vista Estación HMI** para pantallas amplias o tablets.
3. Se conservan todas las funcionalidades industriales:
   - Login cifrado ISO 27400 con credenciales demo preconfiguradas (`operador@faena.cl`).
   - Identificación de rol visible: `ROL: ESTACIÓN DE SUPERVISIÓN`.
   - Control de relé (Parada de emergencia y Rearme con confirmación de seguridad).
   - Osciloscopio en vivo de vibración RMS según norma ISO 10816-3 en Canvas.
   - Inyección de fallas críticas para evaluación sumativa.
   - Telemetría MPU-6050 y consola de tramas bidireccional `>_`.

---

## 2. Experiencia de Usuario y Diseño Visual

### Marco del Dispositivo Móvil (Smartphone Shell)
```
┌──────────────────────────────────────┐
│  [●] Simulación Activa | TI3042 ES2  │ Barra Superior del Entorno
├──────────────────────────────────────┤
│                                      │
│        ╭────────────────────╮        │
│        │   (●) Cámara       │        │ Notch / Agujero de cámara
│        │ 12:45   📶 98% 🔋  │        │ Barra de estado Android
│        ├────────────────────┤        │
│        │ ROL: ESTACIÓN ...  │        │
│        │ Motor Minero #01   │        │ TopAppBar Material 3
│        │ ────────────────── │        │
│        │ [RELÉ CERRADO]     │        │
│        │ [PARADA EMERGENCIA]│        │ Botones táctiles ≥ 48 dp
│        │ ────────────────── │        │
│        │ [ OSCILOSCOPIO ]   │        │ Canvas HMI oscuro (ScopePanel)
│        │ RMS: 2.34 mm/s     │        │ Umbrales ISO 10816-3
│        ├────────────────────┤        │
│        │      ━━━━━━━       │        │ Barra de navegación por gestos
│        ╰────────────────────╯        │
│                                      │
└──────────────────────────────────────┘
```

- **Dimensiones del viewport:** 390px × 844px con bordes curvos (`rounded-[46px]`), sombra industrial profunda y bisel exterior metálico.
- **Micro-interacciones:** Botón en la esquina superior derecha de la barra para alternar entre "Vista Móvil Android" y "Vista Estación HMI (Pantalla Completa)".
- **Accesibilidad:** Cumplimiento de la norma de toques ≥ 48 dp en todos los controles interactivos.

---

## 3. Decisiones Técnicas y Arquitectura

```
┌─────────────────────────────────────────────────────────────┐
│                       APP CONTROLLER                        │
│                         (App.tsx)                           │
│  • Estado de sesión activa (authStore ISO 27400)            │
│  • Enrutamiento: LoginScreen <-> MonitoringScreen           │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    DEVICE FRAME COMPONENT                   │
│                      (DeviceFrame.tsx)                      │
│  • isMobileMode = true (POR DEFECTO)                        │
│  • Android Status Bar (Hora real, Wi-Fi, Batería 98%)       │
│  • Viewport móvil con scroll táctil suave                   │
│  • Android Gesture Navigation Pill                          │
│  • Selector rápido a modo HMI de pantalla completa          │
└──────────────────────────────┬──────────────────────────────┘
                               │
               ┌───────────────┴───────────────┐
               ▼                               ▼
┌─────────────────────────────┐ ┌─────────────────────────────┐
│        LOGIN SCREEN         │ │      MONITORING SCREEN      │
│ • Credenciales demo ISO     │ │ • Rol visible inalterable   │
│ • Tabs Login / Registro     │ │ • Relé con confirmación     │
│ • Doble codificación        │ │ • Osciloscopio Canvas       │
│ • Cifrado AES-256 GCM       │ │ • MPU6050 + Consola `>_`    │
└─────────────────────────────┘ └─────────────────────────────┘
```

### Cambios Concretos a Realizar:
1. **`src/components/DeviceFrame.tsx`**:
   - Establecer `const [isMobileMode, setIsMobileMode] = useState(true);` para que al cargar la página o refrescar la vista previa, el smartphone Android sea la vista activa inmediata.
   - Ajustar el contenedor para centrar perfectamente el dispositivo en la pantalla y permitir scroll fluido del contenido interno.
2. **Verificación de Compilación**:
   - Ejecutar `compile_applet` para asegurar que el bundle de Vite compile con 0 errores y el servidor dev esté listo.

---

## 4. Plan de Ejecución Paso a Paso

1. **Ajuste de Estado Predeterminado**:
   - Modificar `src/components/DeviceFrame.tsx` para inicializar `isMobileMode` en `true`.
2. **Optimización de Estilos del Frame**:
   - Asegurar que el viewport del teléfono permita desplazamiento vertical ergonómico e interactividad completa con los diálogos modales (Consola de tramas, configuración de red y confirmación de relé).
3. **Verificación y Despliegue**:
   - Ejecutar `compile_applet` para confirmar la compilación exitosa.
