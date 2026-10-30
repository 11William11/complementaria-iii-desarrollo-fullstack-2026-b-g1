# BarFlow: frontend integrado con la API (Semana 12)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

Un frontend en HTML, CSS y JavaScript (sin framework, como el de BarFlow de la
[semana 4](../../04-week/02-optional-activity/barflow/)) conectado a la API real de productos:
hace **login**, **guarda el JWT**, lo **envía para listar los productos** (endpoint protegido)
y maneja los estados de **carga y error**. La API tiene **CORS** configurado para aceptar al
frontend.

| Carpeta | Qué es |
|---|---|
| [`barflow-products-api/`](barflow-products-api/) | La API con JWT de la [semana 11](../../11-week/02-optional-activity/), más la configuración de CORS |
| [`barflow-web/`](barflow-web/) | El frontend: `index.html`, `app.js`, `style.css` |

## Cómo ejecutarlo

Requisitos: JDK 17+, Maven 3.9 y cualquier servidor estático en el puerto **5500** (Live Server
de VS Code usa ese puerto por defecto).

```bash
# Terminal 1: la API en http://localhost:8080
cd barflow-products-api
mvn spring-boot:run

# Terminal 2: el frontend en http://localhost:5500
npx serve -l 5500 barflow-web        # o: python -m http.server 5500 --directory barflow-web
```

Abrir <http://localhost:5500> e iniciar sesión con el usuario de prueba **`admin` / `barflow123`**.

El frontend tiene que abrirse desde `http://localhost:5500` (o `http://127.0.0.1:5500`): son los
orígenes que la API acepta por CORS. Abierto con doble clic (`file://`) el navegador bloquea las
peticiones.

## 1. Login y token

`app.js` envía `POST /api/v1/auth/login` con el usuario y la contraseña. Si la API responde `200`,
guarda el token en `localStorage` (clave `barflow.token`) y muestra los productos. Al recargar la
página, si ya hay un token guardado, entra directo sin pedir login. **Cerrar sesión** borra el token.

Si las credenciales son incorrectas la API responde `401` y el formulario muestra
«Usuario o contraseña incorrectos.».

## 2. Consumo del endpoint protegido

`GET /api/v1/products` se pide con la cabecera `Authorization: Bearer <token>`:

```js
function fetchProducts() {
  return request("/products", {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
}
```

Si la API responde `401` (token vencido o inválido), el frontend borra el token y vuelve al login
con el mensaje «Tu sesión expiró. Inicia sesión de nuevo.».

## 3. CORS y estados de carga y error

**CORS.** El frontend (`localhost:5500`) y la API (`localhost:8080`) están en orígenes distintos,
así que el navegador primero envía un *preflight* `OPTIONS` y solo deja pasar la petición si la API
responde con `Access-Control-Allow-Origin`. En
[`SecurityConfig`](barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/SecurityConfig.java)
se agregó:

- `.cors(Customizer.withDefaults())`: el CORS se resuelve antes que la autenticación, porque el
  preflight no lleva token.
- Un `CorsConfigurationSource` para `/api/**` con los orígenes de
  `barflow.cors.allowed-origins` (`http://localhost:5500` y `http://127.0.0.1:5500`), los métodos
  `GET`, `POST`, `PUT`, `DELETE` y `OPTIONS`, y las cabeceras `Authorization` y `Content-Type`.

**Estados.** La vista de productos tiene cuatro paneles y `setState()` muestra solo uno:

| Estado | Cuándo aparece |
|---|---|
| Cargando (spinner) | Mientras se espera la respuesta de la API |
| Datos (tabla) | La API respondió `200` con productos |
| Vacío | La API respondió `200` con una lista vacía |
| Error (con **Reintentar**) | La API está apagada o respondió un error distinto de `401` |

Además, el botón **Ingresar** se deshabilita y cambia a «Ingresando…» mientras se procesa el login.

## Pruebas

**Automáticas (API):** `mvn test` corre **11 pruebas, 0 fallos**: las 9 de la semana 11 (CRUD con
token, login, 401 y 200) y 2 nuevas de CORS en
[`CorsTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/CorsTest.java):

| Prueba | Resultado esperado |
|---|---|
| Preflight `OPTIONS` desde `http://localhost:5500` sin token | `200` con `Access-Control-Allow-Origin: http://localhost:5500` |
| Preflight desde `http://otro-sitio.com` | `403` |

**En el navegador** (2026-10-30, API con `java -jar` y frontend en el puerto 5500):

| Paso | Resultado |
|---|---|
| Login con contraseña incorrecta | `OPTIONS /auth/login → 200`, `POST /auth/login → 401`, mensaje «Usuario o contraseña incorrectos.» |
| Login con `admin` / `barflow123` | `POST /auth/login → 200`, token guardado en `localStorage` |
| Listado | `OPTIONS /products → 200`, `GET /products → 200` con el token; tabla con los 4 productos de ejemplo |
| Token inválido en `localStorage` y recargar | `GET /products → 401`; vuelve al login con «Tu sesión expiró…» y el token se borra |
| API apagada y **Recargar** | Panel de error: «No se pudo conectar con la API. Revisa que esté encendida en el puerto 8080.» |
