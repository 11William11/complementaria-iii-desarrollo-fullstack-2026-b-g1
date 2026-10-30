# BarFlow: API de productos protegida con JWT (Semana 11)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

La API REST de productos de BarFlow de la [semana 10](../../10-week/02-optional-activity/) ahora
tiene **login con JWT**: `POST /api/v1/auth/login` entrega un token, y todos los endpoints de
productos exigen ese token en la cabecera `Authorization: Bearer <token>`. El proyecto está en
[`barflow-products-api/`](barflow-products-api/).

## Cómo ejecutarla

Requisitos: JDK 17 o superior y Maven 3.9. Usa H2 en memoria, así que no hace falta instalar
base de datos.

```bash
cd barflow-products-api
mvn spring-boot:run      # API en http://localhost:8080
mvn test                 # 9 pruebas: 5 del CRUD con token y 4 de login/401/200
```

Usuario de prueba (definido en `application.properties`): **`admin` / `barflow123`**.

## Flujo

```bash
# 1. Sin token → 401
curl -i http://localhost:8080/api/v1/products

# 2. Login → 200 con el token
curl -X POST http://localhost:8080/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"barflow123"}'
# {"token":"eyJhbGciOiJIUzM4NCJ9...","tokenType":"Bearer","expiresIn":3600}

# 3. Con token → 200
curl -i http://localhost:8080/api/v1/products -H "Authorization: Bearer <token>"
```

| Endpoint | ¿Pide token? | Sin token o token inválido |
|---|---|---|
| `POST /api/v1/auth/login` | No | — (credenciales malas → `401 INVALID_CREDENTIALS`) |
| `GET/POST/PUT/DELETE /api/v1/products/**` | Sí | `401 UNAUTHORIZED` |
| Swagger (`/swagger-ui.html`, `/v3/api-docs`) | No | — |

El token dura 60 minutos (`barflow.jwt.expiration-minutes`). Cuando expira, o si la firma no
coincide, la API lo trata igual que si no hubiera token: `401`.

## Qué se agregó a la API de la semana 10

```
src/main/java/co/edu/corhuila/barflow/inventory/
├── controller/AuthController.java           POST /api/v1/auth/login → JWT
├── dto/LoginRequest.java, LoginResponse.java cuerpo del login y su respuesta
├── security/JwtService.java                 crea el token (jjwt, firma HMAC) y lo valida
├── security/JwtAuthenticationFilter.java    lee "Authorization: Bearer" y autentica la petición
├── security/SecurityConfig.java             qué rutas son públicas, sin sesión, 401 en JSON,
│                                            usuario en memoria con contraseña en BCrypt
└── exception/GlobalExceptionHandler.java    + credenciales incorrectas → 401
```

- Dependencias nuevas en el [`pom.xml`](barflow-products-api/pom.xml):
  `spring-boot-starter-security` y `jjwt` 0.12.6 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`).
- La API no guarda sesión (`STATELESS`): cada petición se autentica solo con su token. Por eso
  CSRF está desactivado.
- Los `401` usan el mismo formato de error que el resto de la API: `{ "error": "...", "message": "..." }`.
- La clave para firmar y el usuario están en `application.properties` porque es una actividad de
  práctica; en un despliegue real vendrían de variables de entorno.

## Pruebas: 401 sin token y 200 con token

**Automáticas** ([`AuthControllerTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/AuthControllerTest.java)):

| Prueba | Resultado esperado |
|---|---|
| `GET /api/v1/products` sin token | `401` y `error = UNAUTHORIZED` |
| Login correcto y luego `GET` con el token | `200` en el login (con token) y `200` en el listado |
| Login con contraseña incorrecta | `401` y `error = INVALID_CREDENTIALS` |
| `GET` con un token inventado | `401` |

Las 5 pruebas del CRUD de la semana 10 siguen pasando; ahora envían un token válido en cada
petición. Resultado de `mvn test`: **9 pruebas, 0 fallos**.

**Con Postman:** la colección
[`postman/barflow-jwt.postman_collection.json`](barflow-products-api/postman/barflow-jwt.postman_collection.json)
hace las 3 peticiones del flujo (sin token → 401, login → 200, con token → 200). La salida de
Newman y las respuestas completas están en
[`evidencias/postman.md`](barflow-products-api/evidencias/postman.md).

| # | Petición | Código obtenido |
|---|---|---|
| 1 | `GET /api/v1/products` sin token | `401 Unauthorized` |
| 2 | `POST /api/v1/auth/login` | `200 OK` con el token |
| 3 | `GET /api/v1/products` con `Authorization: Bearer {{token}}` | `200 OK` |

## Swagger

Swagger sigue disponible sin token en <http://localhost:8080/swagger-ui.html>. Como no tiene
configurado el botón *Authorize*, desde Swagger solo se puede probar el login; los endpoints de
productos responden `401` desde ahí y se prueban con Postman o `curl`.
