# Parcial práctico · Corte 3

Complementaria III — Desarrollo Fullstack · 2026-B · Semana 15
**Autor:** William Erney Collo Narvaez (`11William11`)

Las respuestas se apoyan en el proyecto que hice para la actividad C3,
[`14-week/c3-activity`](../../14-week/c3-activity/): **BarFlow**, una API de productos en Spring Boot
protegida con JWT (`barflow-products-api`) y un frontend en HTML + JavaScript que la consume
(`barflow-web`). Los enlaces llevan al código real.

---

## Problema 1 — Seguridad: login con JWT (1.5)

### 1.1 Cómo se emite el token

El cliente envía usuario y contraseña a un endpoint público. Si son correctos, el servidor crea un
JWT firmado con una clave secreta que solo él conoce y lo devuelve.

```text
POST /api/v1/auth/login   { "username": "admin", "password": "barflow123" }

servidor:
  si usuario/contraseña NO coinciden  → 401 Unauthorized
  si coinciden:
      payload = { sub: "admin", iat: ahora, exp: ahora + 60 min }
      token   = base64(header) + "." + base64(payload) + "." + HMAC_SHA256(header.payload, SECRETO)
      → 200 { "token": token, "tokenType": "Bearer", "expiresIn": 3600 }
```

En BarFlow, [`AuthController`](../../14-week/c3-activity/barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/controller/AuthController.java)
valida las credenciales con el `AuthenticationManager` de Spring Security (contraseña guardada con
BCrypt) y [`JwtService.generate()`](../../14-week/c3-activity/barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/JwtService.java)
arma el token con `subject`, `issuedAt` y `expiration`, y lo firma con HMAC.

El payload del JWT **no está cifrado**, solo codificado en Base64: cualquiera puede leerlo, pero
nadie puede modificarlo sin invalidar la firma. Por eso nunca se guarda la contraseña ni datos
sensibles dentro del token.

### 1.2 Dónde lo guarda el frontend

El frontend guarda el token en `localStorage` para no pedir login cada vez que se recarga la página,
y lo envía en la cabecera `Authorization` de cada petición protegida
([`barflow-web/api.js`](../../14-week/c3-activity/barflow-web/api.js)):

```js
const token = await login(username, password);   // POST /auth/login
localStorage.setItem("barflow.token", token);
fetch(API_URL + "/products", { headers: { Authorization: `Bearer ${token}` } });
```

Al cerrar sesión, o si la API responde `401` (token vencido), el token se borra con
`localStorage.removeItem`. Hay que tener en cuenta que `localStorage` lo puede leer cualquier script
de la página, así que el frontend evita XSS: pinta los datos con `textContent` y no con `innerHTML`.
La alternativa es una cookie `HttpOnly` + `Secure` + `SameSite`, que JavaScript no puede leer, pero
obliga a protegerse de CSRF.

### 1.3 Cómo protege el backend un endpoint

Un filtro intercepta **todas** las peticiones antes de llegar al controlador:

```text
para cada petición:
    si la ruta es pública (POST /api/v1/auth/login) → dejar pasar
    header = request.header("Authorization")
    si header empieza con "Bearer ":
        token = header sin "Bearer "
        verificar firma con SECRETO y que exp > ahora
        si es válido → marcar la petición como autenticada con el usuario de "sub"
    si la petición sigue sin autenticar → 401 Unauthorized
    si no → continuar al controlador
```

En BarFlow:

- [`JwtAuthenticationFilter`](../../14-week/c3-activity/barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/JwtAuthenticationFilter.java)
  (un `OncePerRequestFilter`) lee la cabecera, llama a `JwtService.validate()` y, si el token es
  válido, pone el usuario en el `SecurityContextHolder`.
- [`SecurityConfig`](../../14-week/c3-activity/barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/SecurityConfig.java)
  deja público solo el login (`permitAll`) y pide `authenticated()` para el resto. Es *stateless*
  (sin sesión en el servidor) y, si falta el token o es inválido, responde `401` en JSON.

### 1.4 Autenticación vs. autorización

| | Autenticación | Autorización |
|---|---|---|
| Pregunta | ¿**Quién** eres? | ¿**Qué puedes** hacer? |
| Cuándo | Primero: en el login y al validar el token | Después de saber quién es |
| Cómo | Usuario + contraseña → JWT; firma y fecha del token | Roles o permisos del usuario contra la ruta |
| Si falla | `401 Unauthorized` | `403 Forbidden` |

Ejemplo: en BarFlow, sin token `GET /api/v1/products` responde `401` (**autenticación**). Si la API
tuviera dos roles, se agregaría el rol al token (`"roles": ["ADMIN"]`) y se limitaría qué puede
hacer cada uno (**autorización**):

```java
.requestMatchers(HttpMethod.GET, "/api/v1/products/**").hasAnyRole("ADMIN", "MESERO")
.requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("ADMIN")
```

Así, un mesero con token válido puede listar productos, pero si intenta borrar uno recibe `403`: está
autenticado, pero no autorizado.

### 1.5 English requirement

Authentication verifies **who** the user is: the user sends a username and a password, and if they
are valid the server returns a signed JWT that proves the user's identity on every request.
Authorization happens after that and decides **what** the authenticated user is allowed to do, for
example, only an `ADMIN` can delete a product. A missing or invalid token is an authentication error
(`401 Unauthorized`), while a valid token without the required role is an authorization error
(`403 Forbidden`).

---

## Problema 2 — Integración y pruebas (2.0)

### 2.1 Cómo el frontend consume un endpoint protegido

Flujo en [`barflow-web/app.js`](../../14-week/c3-activity/barflow-web/app.js) y
[`api.js`](../../14-week/c3-activity/barflow-web/api.js):

```js
async function loadProducts() {
  setState("loading");                                   // 1. muestra "Cargando…"
  try {
    const res = await fetch(API_URL + "/products", {
      headers: { Authorization: `Bearer ${getToken()}` } // 2. envía el token
    });
    if (!res.ok) throw new ApiError(res.status, ...);    // 3. fetch NO lanza error con 401/500
    const products = await res.json();
    products.length === 0 ? setState("empty")            // 4a. lista vacía
                          : renderProducts(products);    // 4b. tabla con los datos
  } catch (error) {
    if (error.status === 401) {                          // 5. token vencido o inválido
      clearToken();
      showView("login");                                 //    → vuelve al login con aviso
      return;
    }
    setState("error");                                   // 6. otro error → panel con "Reintentar"
  }
}
```

| Estado | Qué ve el usuario |
|---|---|
| Cargando | Mensaje «Cargando productos…» mientras llega la respuesta; el botón de login se deshabilita para evitar doble envío |
| Datos | Tabla de productos |
| Vacío | «No hay productos registrados.» |
| `401` | Se borra el token y vuelve al login: «Tu sesión expiró. Inicia sesión de nuevo.» |
| API apagada / sin red | `fetch` lanza `TypeError` → «No se pudo conectar con la API…» con botón **Reintentar** |
| `4xx`/`5xx` | Muestra el `message` que manda la API en su JSON de error |

### 2.2 Cómo resolver CORS

El frontend corre en `http://localhost:5500` y la API en `http://localhost:8080`. Son **orígenes
distintos** (cambia el puerto), así que el navegador bloquea la respuesta si la API no lo autoriza.
Además, como la petición lleva la cabecera `Authorization`, el navegador primero manda un
**preflight** `OPTIONS` sin token.

La solución va en el **backend** (el frontend no puede saltarse CORS). En
[`SecurityConfig`](../../14-week/c3-activity/barflow-products-api/src/main/java/co/edu/corhuila/barflow/inventory/security/SecurityConfig.java):

```java
http.cors(Customizer.withDefaults());   // CORS antes que la autenticación: el preflight no trae token

CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(List.of("http://localhost:5500", "http://127.0.0.1:5500")); // nunca "*" con credenciales
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
source.registerCorsConfiguration("/api/**", config);
```

Los orígenes permitidos salen de una propiedad (`barflow.cors.allowed-origins`), así que en
producción se cambian por el dominio real del frontend sin tocar el código. Otra opción es poner
frontend y API bajo el mismo dominio con un *reverse proxy* (Nginx: `/` → frontend, `/api` → backend);
así no hay petición entre orígenes y CORS no aplica.

### 2.3 Prueba unitaria propuesta

**Qué verifica:** la regla de negocio «no se pueden crear dos productos con el mismo nombre», de
forma aislada: sin Spring, sin base de datos y sin HTTP. El repository es un *mock* de Mockito.

```java
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @InjectMocks ProductService service;

    @Test
    void createWithADuplicateNameThrowsAndDoesNotSave() {
        when(repository.existsByNameIgnoreCase("Cerveza Aguila")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new ProductRequest("Cerveza Aguila", "Cervezas", 5000, 48)))
                .isInstanceOf(DuplicateProductException.class);

        verify(repository, never()).save(any(Product.class));   // no se guardó nada
    }
}
```

Implementada en
[`ProductServiceTest`](../../14-week/c3-activity/barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/service/ProductServiceTest.java).

### 2.4 Prueba de integración propuesta

**Qué verifica:** que las piezas funcionan juntas de punta a punta dentro del backend (filtro JWT +
Spring Security + controlador + service + base H2): sin token la API responde `401`, y con el token
que devuelve el login responde `200`.

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;

    @Test
    void protectedEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsTokenThatOpensProtectedEndpoint() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"barflow123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mvc.perform(get("/api/v1/products").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
```

Implementada en
[`AuthControllerTest`](../../14-week/c3-activity/barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/controller/AuthControllerTest.java).
En el frontend hay pruebas equivalentes con Vitest + jsdom
([`tests/app.test.js`](../../14-week/c3-activity/barflow-web/tests/app.test.js)): con un `fetch` falso
que responde `401`, se comprueba que la app vuelve al login y muestra el aviso.

| | Unitaria | Integración |
|---|---|---|
| Alcance | Una clase (`ProductService`) | Varias capas juntas (HTTP → seguridad → controlador → BD) |
| Dependencias | Simuladas con Mockito | Reales (contexto de Spring + H2 en memoria) |
| Velocidad | Milisegundos | Segundos (levanta Spring) |
| Encuentra | Errores en reglas de negocio | Errores de configuración y de conexión entre capas |

---

## Problema 3 — Contenedores y despliegue (1.5)

### 3.1 Dockerfile del backend

Es el [`Dockerfile`](../../14-week/c3-activity/barflow-products-api/Dockerfile) de BarFlow, de dos
etapas: la primera compila con Maven y la segunda solo lleva el JRE y el `.jar`, así la imagen final
es más liviana y no incluye Maven ni el código fuente.

```dockerfile
# Etapa 1: compilar el .jar con Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline          # capa en caché mientras no cambien las dependencias
COPY src ./src
RUN mvn -q package -DskipTests            # las pruebas se corren aparte con "mvn test"

# Etapa 2: imagen final, solo el JRE y el .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 3.2 Pasos para desplegar

**Local (Docker Desktop):**

```bash
cd barflow-products-api
mvn test                                              # 1. pasar las pruebas antes de empaquetar
docker build -t barflow-products-api:1.0 .            # 2. construir la imagen
docker run -d --name barflow -p 8080:8080 \
  -e BARFLOW_JWT_SECRET=<clave Base64 de 32+ bytes> \
  -e BARFLOW_AUTH_PASSWORD=<contraseña> \
  barflow-products-api:1.0                            # 3. correr el contenedor con sus secretos
docker ps && docker logs barflow                      # 4. revisar que arrancó
curl -i http://localhost:8080/api/v1/products         # 5. sin token debe responder 401
```

**Nube** (por ejemplo Render, Railway o una VM con Docker):

1. Subir el código a GitHub (o la imagen a un registro: `docker tag` + `docker push` a Docker Hub).
2. Crear el servicio en la nube apuntando al repositorio o a la imagen; la plataforma construye con el
   mismo `Dockerfile`.
3. Configurar las **variables de entorno** en el panel del servicio (no en el código).
4. Publicar el puerto 8080 y usar la URL HTTPS que entrega la plataforma.
5. Actualizar `API_URL` del frontend con esa URL y agregar el dominio del frontend a
   `BARFLOW_CORS_ALLOWED_ORIGINS`.
6. Verificar: login → `200` con token; `GET /products` sin token → `401`.

### 3.3 Variables de entorno y por qué

Spring Boot convierte cada variable de entorno en su propiedad (`BARFLOW_JWT_SECRET` →
`barflow.jwt.secret`), y la variable gana sobre el valor de `application.properties`.

| Variable | Propiedad | Por qué va afuera del código |
|---|---|---|
| `BARFLOW_JWT_SECRET` | `barflow.jwt.secret` | Es el secreto que firma los tokens: si se filtra en GitHub, cualquiera puede fabricar tokens válidos |
| `BARFLOW_AUTH_USERNAME` / `BARFLOW_AUTH_PASSWORD` | `barflow.auth.*` | Credenciales: no deben quedar en el repositorio |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | `spring.datasource.*` | Cambia por ambiente (H2 local, PostgreSQL en la nube) y lleva contraseña |
| `BARFLOW_CORS_ALLOWED_ORIGINS` | `barflow.cors.allowed-origins` | El dominio del frontend es distinto en local y en producción |
| `BARFLOW_JWT_EXPIRATION_MINUTES` | `barflow.jwt.expiration-minutes` | Se ajusta según el ambiente sin recompilar |
| `SERVER_PORT` | `server.port` | Algunas plataformas asignan el puerto |

**Por qué:** la misma imagen sirve para todos los ambientes (*build once, run anywhere*): solo cambia
la configuración al correrla, sin reconstruir. Además, los secretos no quedan en el código, en el
historial de Git ni dentro de la imagen, y se pueden cambiar (rotar) sin un nuevo despliegue del
código. En BarFlow ya se probó: con `-e BARFLOW_AUTH_PASSWORD=otraClave`, el login con la contraseña
original responde `401` y con `otraClave` responde `200`
([README del C3, sección Docker](../../14-week/c3-activity/README.md#3-docker)).
