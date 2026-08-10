# Monetización de Cavernia — pasos concretos

El código ya está listo. Solo falta cambiar 4 identificadores y crear las cuentas.
Con los IDs de prueba que vienen puestos puedes probar todo hoy sin tener cuenta
de AdMob: los anuncios aparecen pero no generan ni cobran nada.

## 1. AdMob (anuncios)
1. Entra a admob.google.com con la misma cuenta de Google del Play Console.
2. Apps → Agregar app → Android → "Sí, ya está publicada" (o "no" mientras pruebas).
3. Crea 3 bloques de anuncios: **Banner**, **Intersticial** y **Bonificado**.
4. Reemplaza en el código:

| Dónde | Qué cambiar |
|---|---|
| `AndroidManifest.xml` → `com.google.android.gms.ads.APPLICATION_ID` | ID de aplicación (empieza con `ca-app-pub-...~...`) |
| `MainActivity.java` → `ID_BANNER` | bloque banner (`ca-app-pub-.../...`) |
| `MainActivity.java` → `ID_INTERSTICIAL` | bloque intersticial |
| `MainActivity.java` → `ID_RECOMPENSADO` | bloque bonificado |

5. AdMob → Pagos: verifica identidad y dirección, y registra la cuenta bancaria.
   El pago sale alrededor del 21 del mes siguiente, cuando el saldo pasa los US$100.

⚠️ Nunca toques tus propios anuncios en el celular con los IDs reales: Google
suspende la cuenta por tráfico inválido. Para probar usa los IDs de prueba o
registra tu equipo como dispositivo de prueba en AdMob.

## 2. Compra "quitar anuncios"
1. Play Console → tu app → Monetizar → Productos → **Productos integrados**.
2. Crear producto con ID exacto: `quitar_anuncios`
   (si usas otro ID, cámbialo en `PRODUCTO_SIN_ANUNCIOS` de `MainActivity.java`).
3. Precio sugerido: US$1.99. Actívalo.
4. Requisito: la app debe estar subida al menos a una pista de prueba para que
   el producto responda. En depuración local la tienda dirá "no disponible":
   eso es normal, se prueba con la pista interna y una cuenta de tester de licencia.
5. Play Console → Configuración → Pruebas de licencia: agrega tu correo para
   comprar sin que te cobren.

## 3. Cómo se comporta el juego
- **Banner**: solo en los menús. Nunca aparece durante la partida.
- **Intersticial**: cada 3 etapas superadas, al pulsar "Siguiente etapa".
  El juego espera a que cierres el anuncio antes de cargar la etapa.
- **Video bonificado**: botón "Ver video · +3 vidas" en la pantalla de derrota.
  Es opcional, el jugador nunca queda obligado.
- **Quitar anuncios**: botón en el menú. Al comprar desaparece todo y queda
  guardado en el equipo; si reinstala, la compra se restaura sola al abrir.
- Si compró, no se carga ni se inicializa ningún anuncio.

## 4. Reglas de Play que debes cumplir
- En la ficha de la tienda declara que la app **contiene anuncios**.
- Prohibido el intersticial al abrir la app o justo al salir.
- En "Seguridad de datos" declara el uso del identificador de publicidad
  (AdMob lo usa) y que no recoges datos personales.
- Clasificación: si la marcas para menores de 13 entran las reglas de Familias
  (redes certificadas y sin anuncios personalizados). Si no quieres esa carga,
  clasifícala para 13+.
- El permiso `com.google.android.gms.permission.AD_ID` ya está en el manifiesto,
  es obligatorio desde Android 13.

## 5. Fuera de la app
- Recomendado: sube el juego a itch.io o a una web tuya con AdSense. Es tráfico
  extra y ahí el HTML corre tal cual, sin recompilar.
- El proyecto sirve de vitrina: el mismo contenedor WebView + AdMob te vale para
  vender juegos o apps sencillas a clientes.
