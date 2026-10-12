# Barrio Alerta

Proyecto de la evaluación final de **Desarrollo de Aplicaciones Móviles**. Es una aplicación Android escrita en **Java** para registrar incidencias de un barrio. Guarda reportes en SQLite, muestra la ubicación del dispositivo y consulta el clima mediante la API REST de Open-Meteo.

Repositorio: <https://github.com/tm146796-arch/EvaluacionFinal>

## Funciones

- Crear reportes con título, categoría y descripción validados.
- Guardar y consultar reportes sin conexión mediante SQLite.
- Obtener la ubicación con permiso solicitado al usar la función. El permiso es opcional.
- Consultar temperatura y viento por HTTPS cuando hay conexión.
- Marcar reportes como resueltos, reabrirlos y eliminarlos con confirmación.

## Abrir y compilar

Requiere Android Studio, SDK de Android 36 y un dispositivo o emulador con Android 7.0 (API 24) o posterior. Gradle usa un daemon con JDK 25; la configuración del proyecto puede descargarlo durante la primera sincronización. El código Java se compila con nivel de lenguaje 17.

1. Clona el repositorio: `git clone https://github.com/tm146796-arch/EvaluacionFinal.git`.
2. Abre la carpeta clonada en Android Studio y deja que Gradle sincronice las dependencias.
3. Ejecuta `./gradlew assembleDebug` desde la raíz del proyecto.
4. Instala `app/build/outputs/apk/debug/app-debug.apk` o ejecuta la app desde Android Studio.

La primera compilación necesita conexión para descargar dependencias. Los archivos `local.properties`, los directorios `build/` y la carpeta `.gradle/` se crean localmente y están excluidos de Git.

APK de prueba: [descargar desde Google Drive](https://drive.google.com/file/d/1tiNKGOXwl2ii9iHNRa7GE7zORmXn3oAD/view?usp=sharing).

## Estructura

| Ruta | Contenido |
| --- | --- |
| `app/src/main/java/com/example/barrioalerta/` | Código Java: actividad y acceso a SQLite. |
| `app/src/main/res/layout/` | Pantalla principal y formulario. |
| `entrega/` | Informe PDF, APK de prueba, base SQLite y video con subtítulos. Solo existe localmente. |
| `evidencias/` | Capturas realizadas en el emulador. Solo existe localmente. |

Las carpetas **`entrega/` y `evidencias/` están en `.gitignore`**. No se descargan al clonar el repositorio ni se suben a GitHub.

## Entrega académica

La entrega se compone de dos elementos:

1. **Este repositorio:** código fuente Java, recursos y configuración para compilar la aplicación.
2. **El PDF `EvaluacionFinal_Cordero_Anyisell.pdf`:** investigación académica, descripción del caso, diseño de la base de datos, funcionalidades, capturas, fragmentos de código, resultados, conclusiones y referencias. Se envía por separado porque `entrega/` no se publica en Git.
