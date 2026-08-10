# Cavernia — proyecto Android listo para compilar

El juego completo ya está dentro: `app/src/main/assets/index.html`.
**100 etapas repartidas en 10 mundos**, con dificultad creciente y mecánicas
nuevas por mundo (lava, hielo resbaloso, viento, oscuridad, piso que se rompe).
No necesita internet: todo el arte, el sonido y la lógica van en ese archivo.

---

## Opción A — Jugarlo YA, sin APK (2 minutos)
Pasa `cavernia.html` al celular (WhatsApp, correo o cable) y ábrelo con Chrome.
Menú de Chrome → **Añadir a pantalla de inicio**. Queda con ícono como si fuera app
y funciona sin señal. Sirve para probar y mostrarlo a clientes mientras tanto.

## Opción B — APK gratis desde la nube (sin instalar nada)
1. Crea un repositorio en GitHub y sube esta carpeta completa.
2. Entra a la pestaña **Actions** → *Construir APK de Cavernia* → **Run workflow**.
3. A los ~4 minutos baja el artefacto `cavernia-apk`. Adentro está `app-debug.apk`.
4. En el celular: Ajustes → permitir instalación de origen desconocido → instalar.

## Opción C — Android Studio (necesaria para Play Store)
1. Android Studio → *Open* → elige esta carpeta. Deja que sincronice Gradle.
2. Probar: **Run ▶** con el celular conectado por USB (depuración USB activa).
3. Publicar: **Build → Generate Signed App Bundle** → AAB → crea tu keystore.
   ⚠️ Guarda el keystore y la contraseña. Si los pierdes no puedes volver a
   actualizar la app en Play Store, nunca.

---

## Antes de subir a Play Store
- Cambia `applicationId` en `app/build.gradle` si quieres otro ID.
- Sube `versionCode` en cada actualización (1, 2, 3...).
- Play Console: cuenta de desarrollador US$25 pago único.
- Necesitas: ícono 512x512 (`icono512.png` incluido), gráfico de cabecera 1024x500,
  mínimo 2 capturas, descripción, política de privacidad publicada en una URL,
  y el cuestionario de clasificación de contenido.
- Cuenta personal nueva: exige prueba cerrada con 12 testers durante 14 días
  antes de pasar a producción.
- Como el juego no recoge ningún dato, en la sección de Seguridad de datos
  declara "no se recopilan datos" (el progreso se guarda solo en el equipo).

## Monetización
El proyecto ya trae AdMob (banner, intersticial y video bonificado) y la compra
"quitar anuncios". Los pasos para poner tus propios identificadores están en
`MONETIZACION.md`. Viene con IDs de prueba, así que funciona desde el primer APK.

## Estructura
```
app/src/main/assets/index.html     <- el juego (edítalo aquí y recompila)
app/src/main/java/.../MainActivity.java  <- WebView + AdMob + compras en Play
app/src/main/res/mipmap-*/         <- íconos
.github/workflows/build-apk.yml    <- compilación automática del APK
MONETIZACION.md                    <- pasos de AdMob y de la compra integrada
```
