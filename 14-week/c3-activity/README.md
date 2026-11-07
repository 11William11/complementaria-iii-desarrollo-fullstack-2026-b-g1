# C3 · BarFlow: app fullstack segura y probada

Actividad calificable · Corte 3 · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

La API de productos de BarFlow (Spring Boot) está protegida con **JWT**, y un frontend en
HTML + JavaScript la consume: inicia sesión y muestra los productos, que son un endpoint protegido.
Tiene **CORS** configurado, **pruebas de backend y de frontend** con casos de error, y la API corre
en un **contenedor Docker**.

| Carpeta | Qué es |
|---|---|
| [`barflow-products-api/`](barflow-products-api/) | API REST (Spring Boot 3.4, Java 17, H2, Spring Security + jjwt) con `Dockerfile` |
| [`barflow-web/`](barflow-web/) | Frontend (HTML, CSS y JS sin framework) con pruebas de Vitest + jsdom |

Se construyó sobre las actividades opcionales de las semanas
[11](../../11-week/02-optional-activity/) (JWT), [12](../../12-week/02-optional-activity/)
(frontend + CORS), [13](../../13-week/02-optional-activity/) (pruebas) y
[14](../../14-week/02-optional-activity/) (Docker).

## Cómo ejecutar

Requisitos: Docker Desktop encendido y Python 3 (o Node) para servir el frontend.

```bash
# 1. API en un contenedor (http://localhost:8080)
cd barflow-products-api
docker build -t barflow-products-api:c3 .
docker run -d --name barflow-c3 -p 8080:8080 barflow-products-api:c3

# 2. Frontend en http://localhost:5500 (otra terminal, desde 14-week/c3-activity)
python -m http.server 5500 --directory barflow-web     # o: npx serve -l 5500 barflow-web
```

Abrir <http://localhost:5500> e iniciar sesión con el usuario de prueba **`admin` / `barflow123`**.
Para detener la API: `docker stop barflow-c3 && docker rm barflow-c3`.

Sin Docker, la API también arranca con `mvn spring-boot:run` (JDK 17 y Maven 3.9).

## 1. JWT e integración con el frontend (CORS)

| Paso | Dónde |
|---|---|
| `POST /api/v1/auth/login` con `username` y `password` → `200 {"token": "..."}` o `401` | [`AuthController`](barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/controller/AuthController.java) |
| El token se firma con HMAC (clave `barflow.jwt.secret`) y vence a los 60 min | [`JwtService`](barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/JwtService.java) |
| Cada petición a `/api/v1/products/**` debe traer `Authorization: Bearer <token>`; si falta, es inválido o venció → `401` | [`JwtAuthenticationFilter`](barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/JwtAuthenticationFilter.java), [`SecurityConfig`](barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/SecurityConfig.java) |
| CORS para `http://localhost:5500` y `http://127.0.0.1:5500`, resuelto **antes** de la autenticación (el *preflight* `OPTIONS` no lleva token) | `SecurityConfig` + `barflow.cors.allowed-origins` |
| El frontend hace el login, guarda el token en `localStorage` y lo envía para listar los productos | [`barflow-web/api.js`](barflow-web/api.js) |
| Vistas y estados: cargando, datos, vacío y error con **Reintentar**; con `401` borra el token y vuelve al login | [`barflow-web/app.js`](barflow-web/app.js) |

**Prueba de extremo a extremo en el navegador** (2026-11-07, API en el contenedor Docker y
frontend en `localhost:5500`):

| Paso | Resultado |
|---|---|
| Login con contraseña incorrecta | `OPTIONS /auth/login → 200`, `POST /auth/login → 401`, «Usuario o contraseña incorrectos.» |
| Login con `admin` / `barflow123` | `POST /auth/login → 200`, `OPTIONS /products → 200`, `GET /products → 200`: tabla con los 4 productos |
| Token inválido en `localStorage` y recargar | `401` → vuelve al login con «Tu sesión expiró. Inicia sesión de nuevo.» y el token se borra |
| Contenedor detenido (`docker stop`) y recargar | Panel de error: «No se pudo conectar con la API. Revisa que esté encendida en el puerto 8080.» |

## 2. Pruebas (backend y frontend, con casos de error)

```bash
cd barflow-products-api && mvn test               # backend
cd barflow-web && npm install && npm test         # frontend
```

**Backend: 15 pruebas, 0 fallos** (JUnit 5, MockMvc y Mockito).

| Clase | Pruebas | Casos de error |
|---|---|---|
| [`ProductServiceTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/service/ProductServiceTest.java) (unitaria, repository simulado con Mockito) | 4 | Nombre duplicado → `DuplicateProductException` sin guardar; id inexistente → `ProductNotFoundException`; stock `-1` rechazado |
| [`AuthControllerTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/AuthControllerTest.java) | 4 | Sin token → `401`; contraseña incorrecta → `401`; token inválido → `401` |
| [`ProductControllerTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/ProductControllerTest.java) (HTTP → H2) | 5 | Validación → `400`, inexistente → `404`, duplicado → `409` |
| [`CorsTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/CorsTest.java) | 2 | Origen no permitido → `403` |

**Frontend: 8 pruebas, 0 fallos** (Vitest + jsdom; la API se reemplaza por un `fetch` falso).

| Archivo | Qué prueba | Casos de error |
|---|---|---|
| [`tests/api.test.js`](barflow-web/tests/api.test.js) | Cliente de la API: login, cabecera `Authorization` | `401` → `ApiError` con estado 401; API apagada → `ApiError` con estado 0 |
| [`tests/app.test.js`](barflow-web/tests/app.test.js) | Componente: carga el `index.html` real, llena el formulario y revisa lo que se ve | Token vencido → vuelve al login con aviso; contraseña incorrecta → mensaje; API apagada → panel de error |

## 3. Docker

[`barflow-products-api/Dockerfile`](barflow-products-api/Dockerfile) es de dos etapas: una imagen
con Maven compila el `.jar` y la imagen final (`eclipse-temurin:17-jre`) solo lleva el JRE y el
`.jar`. El usuario, la contraseña y la clave del JWT se pueden cambiar al correr el contenedor con
variables de entorno, sin reconstruir la imagen:

```bash
docker run -d -p 8080:8080 \
  -e BARFLOW_AUTH_PASSWORD=otraClave \
  -e BARFLOW_JWT_SECRET=<clave en Base64, mínimo 32 bytes> \
  barflow-products-api:c3
```

Probado el 2026-11-07: con `BARFLOW_AUTH_PASSWORD=otraClave` el login con `barflow123` responde
`401` y con `otraClave` responde `200`. La salida de `docker build`, `docker ps` y las peticiones al
contenedor está en [`evidencias/docker.md`](barflow-products-api/evidencias/docker.md).

## Security & run

The API uses stateless JWT authentication: the client sends a username and password to
`POST /api/v1/auth/login`, and if they are valid the server returns a token signed with an HMAC
secret that expires after 60 minutes. The frontend stores that token in `localStorage` and sends it
in the `Authorization: Bearer <token>` header on every request to `/api/v1/products`. A servlet
filter validates the signature and the expiration date, so a missing, tampered or expired token gets
a `401 Unauthorized` response, and the frontend then deletes the token and sends the user back to
the login screen. CORS only allows the frontend origins (`localhost:5500` and `127.0.0.1:5500`),
and it is evaluated before authentication because the browser's preflight request carries no token.
The backend has 15 automated tests (JUnit 5, MockMvc and Mockito) and the frontend has 8 tests
(Vitest with jsdom), and both suites include error cases such as wrong credentials, invalid tokens
and an unreachable API. To run the project with Docker, build the image with
`docker build -t barflow-products-api:c3 .` inside `barflow-products-api`, start it with
`docker run -d -p 8080:8080 barflow-products-api:c3`, and serve `barflow-web` on port 5500. The
credentials and the JWT secret are demo values, so in a real deployment they must be passed as
environment variables (`BARFLOW_AUTH_PASSWORD`, `BARFLOW_JWT_SECRET`) instead of being kept in
`application.properties`.
