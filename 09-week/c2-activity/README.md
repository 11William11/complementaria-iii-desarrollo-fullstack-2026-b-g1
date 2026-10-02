# BarFlow — API de productos con Swagger y Postman (Semana 9)

Actividad calificable c2 (Corte 2) · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

API REST de BarFlow en Spring Boot con arquitectura en capas
(entity → repository → service → controller), CRUD completo del recurso
**`Product`** con persistencia JPA, documentada con **Swagger** y probada con
**Postman**. El proyecto está en [`barflow-products-api/`](barflow-products-api/);
parte del CRUD de la [semana 8](../../08-week/02-optional-activity/barflow-products-crud/).

## Cómo ejecutarlo

Requisitos: JDK 17 y Maven 3.9. No hace falta instalar base de datos: usa H2 en
memoria y carga 4 productos de ejemplo al arrancar.

```bash
cd barflow-products-api
mvn spring-boot:run      # API en http://localhost:8080/api/v1/products
mvn test                 # 5 pruebas automáticas (CRUD completo y errores)
```

## Endpoints

| Acción | Método | URL | Éxito | Errores |
|---|---|---|---|---|
| Listar | `GET` | `/api/v1/products` | `200` | — |
| Obtener | `GET` | `/api/v1/products/{id}` | `200` | `404` no existe · `400` id mal formado |
| Crear | `POST` | `/api/v1/products` | `201` + `Location` | `400` cuerpo inválido · `409` nombre repetido |
| Actualizar | `PUT` | `/api/v1/products/{id}` | `200` | `400` · `404` · `409` |
| Borrar | `DELETE` | `/api/v1/products/{id}` | `204` sin cuerpo | `404` |

Cuerpo para `POST` y `PUT`:

```json
{ "name": "Cerveza Poker", "category": "Cervezas", "price": 4500, "stockQuantity": 60 }
```

## Capas

```
src/main/java/co/edu/corhuila/barflow/inventory/
├── controller/ProductController.java      HTTP → service; códigos de estado
├── service/ProductService.java            reglas de negocio + @Transactional
├── repository/ProductRepository.java      JpaRepository<Product, UUID> (JPA)
├── entity/Product.java                    @Entity + invariantes (precio y stock ≥ 0)
├── dto/ProductRequest.java                entrada validada con @Valid
├── dto/ProductResponse.java               salida (nunca se expone la entity)
└── exception/GlobalExceptionHandler.java  excepción → 400 / 404 / 409
```

## Swagger

Se agregó la dependencia `springdoc-openapi-starter-webmvc-ui` en el
[`pom.xml`](barflow-products-api/pom.xml); no necesita más configuración.
Con la API encendida:

| URL | Qué muestra |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Redirige (`302`) a `/swagger-ui/index.html`, la interfaz donde se prueban los endpoints desde el navegador |
| <http://localhost:8080/v3/api-docs> | La descripción de la API en JSON (OpenAPI 3), con los códigos `200`, `400`, `404` y `409` de cada endpoint |

## Pruebas con Postman

La colección [`postman/barflow-products.postman_collection.json`](barflow-products-api/postman/barflow-products.postman_collection.json)
prueba 3 endpoints, uno de ellos un caso de error. La salida completa está en
[`evidencias/postman.md`](barflow-products-api/evidencias/postman.md).

| # | Petición | Código obtenido | Qué significa |
|---|---|---|---|
| 1 | `GET /api/v1/products` | `200 OK` | La consulta funcionó y el cuerpo trae el arreglo de productos |
| 2 | `POST /api/v1/products` | `201 Created` | Se creó el producto; la cabecera `Location` apunta a su URL nueva |
| 3 | `GET /api/v1/products/00000000-0000-0000-0000-000000000000` | `404 Not Found` | El id está bien formado pero no existe: error del cliente, el cuerpo es `{"error":"NOT_FOUND",...}` |

Las 3 peticiones pasaron sus 6 verificaciones (0 fallidas).

## API reference

The BarFlow API exposes the `Product` resource under the base path `/api/v1/products`, and every endpoint answers in JSON.
`GET /api/v1/products` returns the list of all products with status 200, and it also accepts an optional `category` parameter to filter the list.
`GET /api/v1/products/{id}` returns one product with status 200, or status 404 when the id does not exist and status 400 when the id is not a valid UUID.
`POST /api/v1/products` creates a product from the JSON body and returns status 201 with a `Location` header, or status 400 when the body is invalid and status 409 when the name already exists.
`PUT /api/v1/products/{id}` replaces the data of an existing product and returns status 200, using the same error codes as the create endpoint plus 404 when the product is missing.
`DELETE /api/v1/products/{id}` removes a product and returns status 204 with no body, or status 404 when there is nothing to delete.
Every error response has the same JSON format with an `error` code and a `message`, and validation errors add a `details` list with the invalid fields, while the full interactive documentation is available at `/swagger-ui.html`.
