# BarFlow — Documentar y probar la API (Semana 9)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

La API de productos de BarFlow de la [semana 8](../../08-week/02-optional-activity/barflow-products-crud/),
ahora con **Swagger** y probada con **Postman**. El proyecto está en
[`barflow-products-swagger/`](barflow-products-swagger/).

## Cómo ejecutarlo

Requisitos: JDK 17 y Maven 3.9. Usa H2 en memoria, no hace falta base de datos.

```bash
cd barflow-products-swagger
mvn spring-boot:run      # API en http://localhost:8080/api/v1/products
```

## Swagger

Se agregó la dependencia `springdoc-openapi-starter-webmvc-ui` en el
[`pom.xml`](barflow-products-swagger/pom.xml). Con la API encendida:

- <http://localhost:8080/swagger-ui.html> responde `302` y redirige a `/swagger-ui/index.html` (`200`), la interfaz para probar los endpoints.
- <http://localhost:8080/v3/api-docs> responde `200` con la descripción de la API en JSON.

## Pruebas con Postman

La colección [`postman/barflow-swagger.postman_collection.json`](barflow-products-swagger/postman/barflow-swagger.postman_collection.json)
(se importa en Postman con *Import*) prueba 3 endpoints, uno de ellos un error `400`.
La ejecuté con Newman, el ejecutor de línea de comandos de Postman, con la API encendida:

```bash
npx newman run postman/barflow-swagger.postman_collection.json
```

```
→ 1. Listar productos (200)
  GET http://localhost:8080/api/v1/products [200 OK, 901B, 133ms]
  √  Status code is 200

→ 2. Crear producto (201)
  POST http://localhost:8080/api/v1/products [201 Created, 451B, 425ms]
  √  Status code is 201

→ 3. Id mal formado (400)
  GET http://localhost:8080/api/v1/products/no-es-un-uuid [400 Bad Request, 209B, 11ms]
  √  Status code is 400

requests: 3 ejecutados, 0 fallidos · assertions: 3 ejecutadas, 0 fallidas
```

## Códigos de estado obtenidos

| # | Petición | Código | Cómo lo interpreto |
|---|---|---|---|
| 1 | `GET /api/v1/products` | `200 OK` | La consulta funcionó y trae la lista de productos |
| 2 | `POST /api/v1/products` | `201 Created` | Se creó el producto; la cabecera `Location` apunta a su URL nueva |
| 3 | `GET /api/v1/products/no-es-un-uuid` | `400 Bad Request` | El error es del cliente: el id no es un UUID válido. El cuerpo es `{"error":"MALFORMED_REQUEST","message":"id must be a valid UUID"}` |
