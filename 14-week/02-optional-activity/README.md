# BarFlow: API en un contenedor Docker (Semana 14)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

Empaqueté en una imagen Docker la API de productos de la
[semana 13](../../13-week/02-optional-activity/) (Spring Boot + JWT) y la corrí en un contenedor.
El proyecto está en [`barflow-products-api/`](barflow-products-api/).

## 1. Dockerfile

[`barflow-products-api/Dockerfile`](barflow-products-api/Dockerfile) tiene **dos etapas**:

| Etapa | Imagen base | Qué hace |
|---|---|---|
| `build` | `maven:3.9.9-eclipse-temurin-17` | Descarga las dependencias (`mvn dependency:go-offline`) y compila el `.jar` (`mvn package -DskipTests`) |
| final | `eclipse-temurin:17-jre` | Copia solo el `.jar` y lo arranca con `java -jar app.jar` en el puerto 8080 |

- Se copia primero el `pom.xml` y después `src/`: mientras las dependencias no cambien, Docker
  reutiliza esa capa y no vuelve a descargarlas.
- La imagen final no lleva Maven, el código fuente ni el JDK completo, solo el JRE y el `.jar`.
- Las pruebas no se corren dentro de la imagen; se corren aparte con `mvn test` (15 pruebas, ver la
  semana 13).
- [`.dockerignore`](barflow-products-api/.dockerignore) deja fuera `target/`, la colección de
  Postman y las evidencias, que no hacen falta para construir.

## 2. Construir la imagen y correr el contenedor

Requisito: Docker Desktop (o Docker Engine) encendido. No hace falta tener Java ni Maven instalados:
los pone la imagen de la etapa `build`.

```bash
cd barflow-products-api

# Construir la imagen
docker build -t barflow-products-api:1.0 .

# Correr el contenedor publicando el puerto 8080
docker run -d --name barflow-api -p 8080:8080 barflow-products-api:1.0

# Ver que está corriendo y sus logs
docker ps
docker logs barflow-api

# Detenerlo y borrarlo
docker stop barflow-api && docker rm barflow-api
```

## 3. Verificación

Con el contenedor corriendo, la API responde en <http://localhost:8080> igual que con
`mvn spring-boot:run`:

```bash
# Sin token → 401
curl -i http://localhost:8080/api/v1/products

# Login → 200 con el token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"barflow123"}'

# Con el token → 200 con los productos
curl http://localhost:8080/api/v1/products -H "Authorization: Bearer <token>"
```

Resultado (2026-11-07): **401** sin token, **200** en el login y **200** con los 4 productos de
ejemplo. La salida completa de `docker build`, `docker ps`, `docker logs` y las peticiones está en
[`evidencias/docker.md`](barflow-products-api/evidencias/docker.md).

Swagger UI también funciona desde el contenedor: <http://localhost:8080/swagger-ui.html>.
