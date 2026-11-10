# MyIphone 📱

Aplicación Android nativa (Kotlin + XML) de una **tienda virtual de iPhone nuevos en Colombia**, con tres roles: **comprador, vendedor y administrador**. Proyecto de la asignatura _Desarrollo de Aplicaciones Móviles Nativas_ – Fundación Universitaria Compensar.

## Funcionalidades por rol

| Rol               | Pantallas                                                                                                                               |
| ----------------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| **Transversal**   | Splash animado, bienvenida, login, registro, recuperar contraseña, **ingreso con huella (BiometricPrompt)**, Drawer + Bottom Navigation |
| **Comprador**     | Inicio, catálogo con búsqueda y filtros, detalle del producto, carrito, pasarela de pago (simulada), mis pedidos, perfil                |
| **Vendedor**      | Resumen de ventas, CRUD de productos, pedidos recibidos (cambio de estado), perfil                                                      |
| **Administrador** | Dashboard, CRUD de usuarios con roles, gestión de productos, reporte de ventas por periodo                                              |

## Tecnologías

Kotlin · Vistas XML + ViewBinding · Material 3 · Firebase Authentication (correo y Google) · Firebase Realtime Database · androidx.biometric · Glide · Poppins (Google Fonts) · Git/GitHub.

## Estructura

```
app/src/main/
├─ java/com/myiphone/store/
│  ├─ cliente/      login, registro y panel base del comprador
│  ├─ comprador/    catálogo, detalle, carrito, pago, pedidos, perfil
│  ├─ vendedor/     panel del vendedor y formulario de producto
│  ├─ admin/        login, dashboard, usuarios, productos y reporte
│  └─ BiometricAuth.kt · SesionUtil.kt · Splash · Bienvenida
└─ res/
   ├─ layout/ drawable/ menu/ color/ font/
   └─ values/  colors · themes · styles · dimens_myiphone · strings_*
```

Todos los colores, estilos, dimensiones y textos están centralizados en `res/values`.

## Base de datos (Realtime Database)

- `Usuarios/{uid}`: nombres, email, tipoUsuario (`cliente`, `vendedor`, `administrador`)
- `Productos/{id}`: nombre, serie, almacenamiento, color, precio, stock, imagenUrl, destacado
- `Pedidos/{id}`: comprador, dirección, método de pago, total, estado, fecha, items

## Cómo ejecutarlo

1. Clonar el repositorio y abrirlo en Android Studio.
2. Crear un proyecto en [Firebase](https://console.firebase.google.com), registrar la app `com.myiphone.store` y descargar **`google-services.json`** a la carpeta `app/` (no se sube al repositorio).
3. Activar _Authentication_ (correo/contraseña y Google) y _Realtime Database_ (modo de pruebas).
4. Ejecutar en un emulador con Android 6.0+ (API 23).
5. **Primer administrador:** registrarse como comprador y cambiar `tipoUsuario` a `administrador` en la consola; luego entrar por _Acceso administrador_.

El catálogo de ejemplo (7 iPhone) se crea solo la primera vez que se abre el catálogo.

## Notas

- La pasarela de pagos es **simulada** (no cobra ni guarda datos de tarjeta).
- Las reglas de la base de datos están en modo de pruebas; deben cerrarse por rol antes de publicar.

## Autor

Yonatan David Rojas Alarcon – Ingeniería de Software, UCompensar.
