# App Control — Gestor de Aplicaciones de Android

App Control monitorea la actividad de las apps instaladas, detecta reapariciones y permite **detener los procesos en segundo plano** de las apps que elijas con un solo botón. Funciona sin root, sin Shizuku y sin ADB.

> **Grado del proyecto Gradle:** `appkiller` · **Repositorio / carpeta de clonado:** `app-killer`

---

## Características

- **Detección de aplicaciones instaladas** — Enumera todas las apps del sistema y del usuario
- **Selección y monitoreo** — Elige qué apps vigilar y cuáles excluir
- **Detección de actividad** — Usa `UsageStatsManager` para saber qué apps están en ejecución
- **Detención de apps** — Botón **Stop Apps** (dashboard) y **Stop App** (detalle) que detiene de verdad los procesos en segundo plano mediante `ActivityManager.killBackgroundProcesses`
- **Detección de reaparición** — Registra y notifica cuando una app detenida vuelve a ejecutarse
- **Historial de eventos** — Registro de actividad, detenciones y errores
- **Notificaciones opcionales** — Acción rápida para detener la app directamente desde la notificación
- **Perfiles configurables** — Reglas por contexto (Noche, Trabajo, Juegos, etc.)
- **Programación con WorkManager y AlarmManager** — Monitoreo periódico y nocturno automático
- **Interfaz Material 3** — Diseño moderno con soporte dark y light mode

---

## Cómo detiene las apps

El flujo del botón **Stop Apps** es:

1. Lee las apps seleccionadas (sin contar las excluidas).
2. Comprueba cuáles se están ejecutando en segundo plano (`UsageStatsManager`).
3. Llama a `ActivityManager.killBackgroundProcesses` para cada una de ellas.
4. Muestra el resumen: **Stopped X of Y apps** (o error si alguna falla).
5. Registra cada detención en el historial como evento de acción.

La detención usa la API oficial con el permiso normal `KILL_BACKGROUND_PROCESSES` (se concede automáticamente, sin diálogos). No usa root, Shizuku, ADB ni comandos de sistema. Ver [Limitaciones](#limitaciones-de-android).

---

## Arquitectura

| Capa | Tecnología |
|------|-----------|
| UI | Jetpack Compose + Material 3 |
| Patrón | MVVM + Clean Architecture |
| Persistencia | Room + DataStore |
| Programación | WorkManager + AlarmManager |
| Navegación | Navigation Compose |
| Di | Manual (ViewModel Factories) |
| Coroutines | Kotlin Coroutines + Flow |

**Estructura de capas:**

```
feature/        → UI Compose, ViewModels, navegación
domain/         → Casos de uso y modelos de negocio
data/           → Repositorios, providers de sistema (ProcessStopper, UsageStats…)
core/           → Room, DataStore, WorkManager, notificaciones, permisos
```

---

## Requisitos

- **Android 8.0 (API 26)** o superior
- Permisos de **Usage Access** (para detectar la actividad de las apps)
- **Kill background processes** — permiso normal, se concede automáticamente
- Sin root, sin Shizuku, sin ADB

---

## Compilación desde un equipo nuevo

Guía para compilar el APK desde una máquina sin nada de Android instalado. El proyecto incluye el Gradle Wrapper (`gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`), así que **no necesitas instalar Gradle**, solo el JDK y el Android SDK.

### 1. Requisitos previos

| Herramienta | Versión | Notas |
|-------------|---------|-------|
| **JDK** | OpenJDK 17 (o 21) | Obligatorio para AGP 8.7.3 |
| **Android SDK** | platform 35 + build-tools 35 | Se instala con `sdkmanager` |
| **ADB / platform-tools** | Última | Solo para instalar/ejecutar en dispositivo |

Verifica el JDK antes de continuar:

```bash
java -version   # debe reportar 17.x (o 21.x)
```

Si no lo tienes:
- **Linux/WSL**: `sudo apt install openjdk-17-jdk` (o `sdkman` / la distro que uses)
- **macOS**: `brew install openjdk@17`
- **Windows**: instalar OpenJDK 17 y definir `JAVA_HOME`

### 2. Instalar el Android SDK

Descarga las Commandline Tools desde <https://developer.android.com/studio#command-line-tools-only>
o, si usas Android Studio, se instalan desde el SDK Manager. En Linux/macOS:

```bash
# Descomprimir en el directorio del SDK
mkdir -p "$HOME/Android/Sdk/cmdline-tools"
unzip commandlinetools-linux-*.zip -d "$HOME/Android/Sdk/cmdline-tools"
mv "$HOME/Android/Sdk/cmdline-tools/cmdline-tools" "$HOME/Android/Sdk/cmdline-tools/latest"

# Aceptar licencias e instalar los componentes mínimos
"$HOME/Android/Sdk/cmdline-tools/latest/bin/sdkmanager" --licenses
"$HOME/Android/Sdk/cmdline-tools/latest/bin/sdkmanager" \
  "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### 3. Indicar al proyecto dónde está el SDK

Crea `local.properties` en la raíz del proyecto (tras `git clone` no existe):

```properties
sdk.dir=/home/tu-usuario/Android/Sdk
```

> También sirve la variable de entorno `ANDROID_HOME`. Android Studio genera `local.properties` automáticamente.

### 4. Compilar

```bash
cd app-killer

# APK de depuración (firmado por defecto)
./gradlew assembleDebug

# APK de release (sin firmar)
./gradlew assembleRelease
```

**Resultado:**

```
app/build/outputs/apk/debug/app-debug.apk     # instalable directamente
app/build/outputs/apk/release/app-release-unsigned.apk
```

### 5. Ejecutar en un dispositivo

```bash
# Con ADB instalado y debugging USB habilitado
adb install app/build/outputs/apk/debug/app-debug.apk
```

En el primer arranque la app pedirá los permisos: **Usage Access** (obligatorio), **Notificaciones** y **Alarmas exactas**. El permiso **Kill background processes** se concede solo automáticamente.

### 6. Ejecutar las pruebas

```bash
# Tests unitarios (JVM, no requieren dispositivo)
./gradlew testDebugUnitTest

# Tests instrumentados (requieren dispositivo/emulador conectado)
./gradlew connectedDebugAndroidTest
```

### 7. Opción A — Android Studio (alternativa gráfica)

1. `File → Open` y selecciona la carpeta raíz del proyecto.
2. `File → Settings → Build, Execution, Deployment → Build Tools → Gradle` y elige **Gradle JDK 17**.
3. `Sync Project with Gradle Files`.
4. `Run ▶` (dispositivo/emulador) o `Build → Build APK(s)`.

### Solución de problemas habituales

| Error | Causa | Solución |
|-------|-------|----------|
| `SDK location not found` | Falta `sdk.dir` o `ANDROID_HOME` | Crear `local.properties` (paso 3) |
| `Unsupported class file major version …` | JDK incorrecto | Usar JDK 17 (o 21), revisar `Gradle JDK` |
| `License for package … not accepted` | Licencias sin aceptar | `sdkmanager --licenses` |
| Descarga lenta de Gradle la primera vez | El wrapper descarga la distribución (~140 MB) | Solo ocurre la primera compilación |
| `aapt2`/`AGP` falla en CI | Variables de entorno sin exportar | Exportar `JAVA_HOME` y `ANDROID_HOME` en el shell |

---

## Permisos

| Permiso | Uso | Obligatorio |
|---------|-----|:-----------:|
| **Usage Access** | Detectar qué apps se ejecutan y cuándo | Sí |
| **Kill background processes** | Detener los procesos en segundo plano de las apps | Sí (permiso normal, automático) |
| **Acceso a lista de apps** | Enumerar apps instaladas en el dispositivo | Sí |
| **Notificaciones** | Alertar sobre reaparición de apps | No (pero recomendado) |
| **Alarmas exactas** | Programación precisa del monitoreo nocturno | No |

---

## Limitaciones de Android

La API `ActivityManager.killBackgroundProcesses` **sí detiene de verdad los procesos en segundo plano**, pero Android impone límites que impiden un cierre total:

- **No detiene apps en primer plano** (con una actividad visible): el sistema bloquea la operación.
- **No es un `force-stop`**: la app no queda marcada como "detenida" a nivel de sistema, así que puede
  volver a ejecutarse (por sí sola o porque la abras). Por eso App Control también monitorea y alerta
  sobre reapariciones.
- El resultado del botón **Stop Apps** / **Stop App** refleja exactamente lo ocurrido: apps detenidas,
  apps inactivas y errores.

**Lo que hace App Control:**

- **Detiene** los procesos en segundo plano de las apps seleccionadas con un solo botón (dashboard y detalle)
- **Detiene la app** directamente desde la notificación de reaparición (acción «Stop»)
- **Registra** en el historial cada detención y cada error
- **Detecta y alerta** cada vez que una app reaparece tras haber sido detenida
- **Abre la configuración del sistema** de una app para casos que requieran más control (p. ej. apps en primer plano)

**Lo que NO hace:**

- No usa root
- No usa Shizuku
- No usa ADB
- No usa APIs ocultas o no documentadas
- No ejecuta comandos de sistema
- No fuerza el cierre de apps en primer plano

Todo funciona dentro del modelo de seguridad estándar de Android.

---

## Estructura del Proyecto

```
app-killer/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/appcontrol/
│       │   │   ├── AppControlApplication.kt
│       │   │   ├── core/        # Room, DataStore, notificaciones, permisos, sistema, tiempo
│       │   │   ├── data/        # Repositorios, providers de sistema (ProcessStopper, UsageStats…)
│       │   │   ├── domain/      # Modelos de negocio y casos de uso
│       │   │   ├── engine/      # Motor de monitoreo
│       │   │   ├── feature/     # UI Compose (screens, viewmodels) y navegación
│       │   │   ├── receiver/    # BootReceiver, NotificationReceiver
│       │   │   ├── scheduler/   # WorkManager y AlarmManager
│       │   │   └── worker/      # Workers de WorkManager
│       │   └── res/
│       ├── test/                # Tests unitarios (JVM)
│       └── androidTest/         # Tests instrumentados y de Compose UI
├── gradle/wrapper/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts          # rootProject.name = "appkiller"
├── build.gradle.kts
├── gradle.properties
├── .gitignore
└── README.md
```

---

## Configuración

### Perfiles

Cada perfil define un conjunto de reglas:

- **Horario** — Rango de horas en que aplica
- **Apps incluidas** — Qué aplicaciones monitorea
- **Acción** — Notificar, mostrar diálogo o silenciar

### Monitoreo

- **Intervalo de revisión** — Frecuencia de comprobación (configurable)
- **Modo nocturno** — Monitoreo automático en horarios programados

### Notificaciones

- Activar o desactivar globalmente
- Notificación persistente mientras el monitoreo está activo
- Alertas por evento (aparición de app no autorizada), con acción rápida **Stop**

### Historial

- Registro cronológico de todos los eventos detectados (actividad y detenciones)
- Opción de limpiar historial manualmente

---

## Privacidad

- **100% local** — Todos los datos se almacenan en el dispositivo
- **Sin servidores** — No se envía ninguna información a internet
- **Sin analytics** — No se recopilan métricas de uso
- **Sin tracking** — No se usan SDKs de terceros para rastreo
- **Control total** — El usuario puede eliminar todos sus datos en cualquier momento desde la app

---

## Licencia

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```