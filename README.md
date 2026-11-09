# MyIphone – Tienda virtual de iPhone nuevos

Aplicación Android nativa (Kotlin, vistas XML, Firebase) para la venta de iPhone nuevos en Colombia.
Proyecto de la asignatura **Desarrollo de Aplicaciones Móviles Nativas** – Fundación Universitaria Compensar.

Roles: **Comprador**, **Vendedor** y **Administrador**.

## Cómo ejecutarlo
1. Abrir el proyecto con Android Studio (versión estable reciente).
2. Crear un proyecto propio en [Firebase](https://console.firebase.google.com) y registrar la app con el paquete `com.myiphone.store`.
3. Activar *Authentication* (correo y Google), *Realtime Database* y *Storage*.
4. Descargar `google-services.json` y copiarlo en `app/` (el archivo está en `.gitignore`).
5. En `app/src/main/res/values/strings.xml` poner el *Web client ID* de Google en `default_web_client_id`.
6. (Opcional) Agregar `MAPAS_KEY=...` en `local.properties` para el mapa.

## Estado
Ver el historial de commits: el proyecto se construye por fases (identidad visual, autenticación,
comprador, vendedor, administrador).
