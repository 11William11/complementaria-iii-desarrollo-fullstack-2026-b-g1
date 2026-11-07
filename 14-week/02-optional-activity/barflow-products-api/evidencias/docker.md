# Evidencia: la API corriendo en Docker (semana 14)

Fecha: 2026-11-07 · Docker 28.4.0 (Docker Desktop, Windows 11)

## 1. Construir la imagen

```text
$ docker build -t barflow-products-api:1.0 .
...
#15 naming to docker.io/library/barflow-products-api:1.0 done
#15 DONE 3.3s

$ docker images barflow-products-api
barflow-products-api:1.0   cd531c929401   548MB
```

La primera construcción tardó 1 min 33 s porque descarga las imágenes base y las dependencias de
Maven. Las siguientes son más rápidas: si `pom.xml` no cambia, Docker reutiliza la capa de
`mvn dependency:go-offline`.

## 2. Correr el contenedor

```text
$ docker run -d --name barflow-api -p 8080:8080 barflow-products-api:1.0

$ docker ps
barflow-api   barflow-products-api:1.0   Up 34 seconds   0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp

$ docker logs barflow-api
... Started InventoryApplication in 8.727 seconds (process running for 9.57)
```

## 3. La API responde desde el contenedor

Peticiones hechas desde el computador (fuera del contenedor) al puerto publicado 8080:

```text
$ curl -i http://localhost:8080/api/v1/products            # sin token
HTTP/1.1 401 
{"error":"UNAUTHORIZED","message":"a valid Bearer token is required"}

$ curl -i -X POST http://localhost:8080/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"barflow123"}'
HTTP/1.1 200 
{"token":"eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6M
…

$ curl -i http://localhost:8080/api/v1/products -H "Authorization: Bearer <token>"
HTTP/1.1 200 
 - Aguardiente Doble Anis | Licores | 65000
 - Cerveza Aguila | Cervezas | 5000
 - Club Colombia | Cervezas | 6000
 - Gaseosa Postobon | Bebidas | 3000
```

| Petición | Esperado | Obtenido |
|---|---|---|
| `GET /api/v1/products` sin token | 401 | **401** |
| `POST /api/v1/auth/login` con `admin` / `barflow123` | 200 + token | **200** + token |
| `GET /api/v1/products` con el token | 200 + productos | **200** + los 4 productos de ejemplo |
