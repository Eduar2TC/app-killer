# App Control — Gestor Inteligente de Aplicaciones Android

Gestor de aplicaciones Android que monitorea la actividad de las apps instaladas, detecta reapariciones y ofrece perfiles configurables. Funciona completamente sin root, sin Shizuku y sin ADB.

---

## Características Principales

- **Detección de aplicaciones instaladas** — Enumera todas las apps del sistema y del usuario
- **Selección y monitoreo** — Elige qué apps quieres vigilar
- **Detección de actividad** — Usa `UsageStatsManager` para detectar qué apps se están ejecutando
- **Detección de reaparición** — Alerta cuando una app que esperabas cerrada vuelve a ejecutarse
- **Perfiles configurables** — Crea reglas por contexto (Noche, Trabajo, Juegos, etc.)
- **Historial de eventos** — Registro completo de actividad detectada
- **Notificaciones opcionales** — Alertas sobre cambios en el estado de apps
- **Programación con WorkManager y AlarmManager** — Monitoreo periódico y nocturno automático
- **Interfaz Material 3** — Diseño moderno con soporte dark y light mode

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
data/           → Repositorios, providers de sistema
core/           → Room, DataStore, WorkManager, notificaciones
```

---

## Requisitos

- **Android 8.0 (API 26)** o superior
- Permisos de **Usage Access** (para detectar actividad de apps)
- Sin root, sin Shizuku, sin ADB

---

## Compilación desde un equipo nuevo

Guía para compilar el APK desde una máquina que no tiene nada de Android instalado. El proyecto ya incluye el Gradle Wrapper (`gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`), por lo que **no necesitas instalar Gradle**, solo el JDK y el Android SDK.

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

Si no los tienes:
- **Linux/WSL**: `sudo apt install openjdk-17-jdk` (o `sdkman` / la distro que uses)
- **macOS**: `brew install openjdk@17`
- **Windows**: instalar OpenJDK 17 y definir `JAVA_HOME`

### 2. Instalar el Android SDK

Descarga Commandline Tools desde <https://developer.android.com/studio#command-line-tools-only>
o, si tienes Android Studio, se instala desde el SDK Manager. En Linux/macOS:

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

Crea `local.properties` en la raíz del proyecto (después de `git clone` no existe):

```properties
sdk.dir=/home/tu-usuario/Android/Sdk
```

> También sirve la variable de entorno `ANDROID_HOME` apuntando al mismo directorio. Si abres el proyecto en Android Studio, se genera `local.properties` automáticamente.

### 4. Compilar

```bash
cd appkiller

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
# Con ADB ya instalado y debugging USB habilitado
adb install app/build/outputs/apk/debug/app-debug.apk
```

En el primer arranque la app pedirá los permisos:
**Usage Access** (obligatorio), **Notificaciones** y **Alarmas exactas**.

### 6. Ejecutar las pruebas

```bash
# Tests unitarios (JVM, no requieren dispositivo)
./gradlew testDebugUnitTest

# Tests instrumentados (requieren dispositivo/emulador conectado)
./gradlew connectedDebugAndroidTest
```

### 7. Opción A — Android Studio (alternativa gráfica)

1. `File → Open` y selecciona la carpeta raíz del proyecto.
2. `File → Settings → Build, Execution, Deployment → Build Tools → Gradle`
   y elige **Gradle JDK 17**.
3. Espera a la sincronización (`Sync Project with Gradle Files`).
4. `Run ▶` (dispositivo/emulador) o `Build → Build APK(s)`.

### Solución de problemas habituales

| Error | Causa | Solución |
|-------|-------|----------|
| `SDK location not found` | Falta `sdk.dir` o `ANDROID_HOME` | Crear `local.properties` (paso 3) |
| `Unsupported class file major version …` | JDK incorrecto | Usar JDK 17 (o 21), revisar `Gradle JDK` |
| `License for package … not accepted` | Licencias sin aceptar | `sdkmanager --licenses` |
| Descarga lenta de Gradle la primera vez | El wrapper descarga el distribución (~140 MB) | Solo ocurre la primera compilación |
| `aapt2`/`AGP` falla en CI | Variables de entorno sin exportar | Exportar `JAVA_HOME` y `ANDROID_HOME` en el shell |

---

## Permisos

| Permiso | Uso | Obligatorio |
|---------|-----|:-----------:|
| **Usage Access** | Detectar qué apps se ejecutan y cuándo | Sí |
| **Notificaciones** | Alertar sobre reaparición de apps | No (pero recomendado) |
| **Alarmas exactas** | Programación precisa del monitoreo nocturno | No |
| **Acceso a lista de apps** | Enumerar apps instaladas en el dispositivo | Sí |

---

## Limitaciones de Android

Android no permite que una app de terceros ejecute `force-stop` sobre otra aplicación. Esta es una restricción de seguridad del sistema operativo que no se puede evadir sin permisos root o herramientas externas.

**Lo que hace App Control:**

- **Informa** al usuario cuando una app se está ejecutando sin autorización
- **Abre la configuración del sistema** para que el usuario pueda forzar el cierre manualmente
- **Detecta y registra** cada vez que una app reaparece tras haber sido cerrada
- **Sugiere acciones** dentro del marco que Android permite

**Lo que NO hace:**

- No usa root
- No usa Shizuku
- No usa ADB
- No usa APIs ocultas o no documentadas
- No ejecuta comandos de sistema

Todo funciona completamente dentro del modelo de seguridad estándar de Android.

---

## Estructura del Proyecto

```
appkiller/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/appkiller/
│   │   │   │   ├── App.kt
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── di/              # Módulos Koin
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/       # Room DB, DAOs, DataStore
│   │   │   │   │   └── repository/  # Implementaciones de repositorios
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/       # Modelos de negocio
│   │   │   │   │   └── usecase/     # Casos de uso
│   │   │   │   ├── presentation/
│   │   │   │   │   ├── screens/     # Pantallas Compose
│   │   │   │   │   ├── components/  # Componentes reutilizables
│   │   │   │   │   └── viewmodel/   # ViewModels
│   │   │   │   ├── service/         # Servicio de monitoreo
│   │   │   │   └── worker/          # Workers de WorkManager
│   │   │   ├── res/
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── build.gradle.kts
├── .gitignore
└── README.md
```

---

## Configuración

### Perfiles

Cada perfil define un conjunto de reglas:

- **Horario** — Rango de horas en que aplica
- **Apps incluidas** — Qué aplicaciones monitorea
- **Acción** — Notificar, mostrar diálogo, o silenciar

### Monitoreo

- **Intervalo de revisión** — Frecuencia de comprobación (configurable)
- **Modo nocturno** — Monitoreo automático en horarios programados

### Notificaciones

- Activar o desactivar globalmente
- Notificación persistente mientras el monitoreo está activo
- Alertas por evento (aparición de app no autorizada)

### Historial

- Registro cronológico de todos los eventos detectados
- Opción de limpiar historial manualmente

---

## Privacidad

- **100% local** — Todos los datos se almacenan en el dispositivo
- **Sin servidores** — No se envía ninguna información a internet
- **Sin analytics** — No se recopilan métricas de uso
- **Sin tracking** — No se usan SDKs de terceros para rastreo
- **Control total** — El usuario puede eliminar todos los datos en cualquier momento desde la app

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
