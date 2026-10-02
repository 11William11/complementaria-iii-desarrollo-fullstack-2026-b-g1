# Evidencia de pruebas con Postman

Colección: [`postman/barflow-products.postman_collection.json`](../postman/barflow-products.postman_collection.json)
(se importa en Postman con *Import*). Para dejar la salida por escrito la ejecuté con
**Newman**, el ejecutor de línea de comandos de Postman, con la API encendida
(`mvn spring-boot:run`) el 2026-10-02:

```bash
npx newman run postman/barflow-products.postman_collection.json
```

```
BarFlow · Products API (semana 9)

→ 1. Listar productos (200)
  GET http://localhost:8080/api/v1/products [200 OK, 901B, 47ms]
  √  Status code is 200
  √  Body is an array

→ 2. Crear producto (201)
  POST http://localhost:8080/api/v1/products [201 Created, 451B, 99ms]
  √  Status code is 201
  √  Location header is present

→ 3. Obtener producto que no existe (404)
  GET http://localhost:8080/api/v1/products/00000000-0000-0000-0000-000000000000 [404 Not Found, 264B, 16ms]
  √  Status code is 404
  √  Error body says NOT_FOUND

requests: 3 ejecutados, 0 fallidos · assertions: 6 ejecutadas, 0 fallidas
```

Cuerpo de la respuesta del caso de error (petición 3):

```http
HTTP/1.1 404
Content-Type: application/json

{"error":"NOT_FOUND","message":"product 00000000-0000-0000-0000-000000000000 does not exist"}
```
