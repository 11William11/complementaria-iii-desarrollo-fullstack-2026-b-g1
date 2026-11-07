# Evidencia de pruebas: 401 sin token y 200 con token

Colección: [`postman/barflow-jwt.postman_collection.json`](../postman/barflow-jwt.postman_collection.json)
(se importa en Postman con *Import*). La petición 2 guarda el token en la variable `{{token}}` de la
colección y la petición 3 lo envía en la cabecera `Authorization: Bearer {{token}}`.

Para dejar la salida por escrito la ejecuté con **Newman**, el ejecutor de línea de comandos de
Postman, con la API encendida (`mvn spring-boot:run`) el 2026-10-30:

```bash
npx newman run postman/barflow-jwt.postman_collection.json --disable-unicode
```

```
BarFlow · Products API con JWT (semana 11)

Root 1. Listar productos sin token (401)
  GET http://localhost:8080/api/v1/products [401 Unauthorized, 405B, 32ms]
  Pass  Status code is 401
  Pass  Error body says UNAUTHORIZED

Root 2. Login (200 + token)
  POST http://localhost:8080/api/v1/auth/login [200 OK, 536B, 368ms]
  Pass  Status code is 200
  Pass  Response has a token

Root 3. Listar productos con token (200)
  GET http://localhost:8080/api/v1/products [200 OK, 1.07kB, 148ms]
  Pass  Status code is 200
  Pass  Body is an array

requests: 3 ejecutados, 0 fallidos · assertions: 6 ejecutadas, 0 fallidas
```

(`--disable-unicode` solo cambia los símbolos de la salida: `Root` es la flecha de cada
petición y `Pass` es la marca de verificación.)

Respuestas completas de los casos de error, tomadas con `curl`:

```http
GET /api/v1/products            (sin cabecera Authorization)
HTTP/1.1 401
Content-Type: application/json

{"error":"UNAUTHORIZED","message":"a valid Bearer token is required"}
```

```http
POST /api/v1/auth/login         {"username":"admin","password":"mala"}
HTTP/1.1 401
Content-Type: application/json

{"error":"INVALID_CREDENTIALS","message":"username or password is incorrect"}
```
