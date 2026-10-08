# BarFlow — API de productos probada y documentada (Semana 10)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

Cierre del Corte 2: la API REST de BarFlow del recurso **`Product`**, consolidada
(CRUD en capas + persistencia con JPA), documentada con **Swagger** y probada con
**Postman**. El proyecto está en [`barflow-products-api/`](barflow-products-api/) y
parte de la [`c2-activity` de la semana 9](../../09-week/c2-activity/barflow-products-api/).

## Cómo ejecutarla

Requisitos: JDK 17 o superior y Maven 3.9. No hace falta instalar base de datos: usa H2
en memoria y carga 4 productos de ejemplo al arrancar.

```bash
cd barflow-products-api
mvn spring-boot:run      # API en http://localhost:8080/api/v1/products
mvn test                 # 5 pruebas automáticas (CRUD completo y errores)
```

## Endpoints

| Acción | Método | URL | Éxito | Errores |
|---|---|---|---|---|
| Listar | `GET` | `/api/v1/products` (filtro opcional `?category=`) | `200` | — |
| Obtener | `GET` | `/api/v1/products/{id}` | `200` | `404` no existe · `400` id mal formado |
| Crear | `POST` | `/api/v1/products` | `201` + `Location` | `400` cuerpo inválido · `409` nombre repetido |
| Actualizar | `PUT` | `/api/v1/products/{id}` | `200` | `400` · `404` · `409` |
| Borrar | `DELETE` | `/api/v1/products/{id}` | `204` sin cuerpo | `404` |

Cuerpo para `POST` y `PUT`:

```json
{ "name": "Cerveza Poker", "category": "Cervezas", "price": 4500, "stockQuantity": 60 }
```

Todos los errores responden con el mismo formato JSON: `{ "error": "...", "message": "..." }`
(y los de validación agregan `details` con los campos inválidos).

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

Usa la dependencia `springdoc-openapi-starter-webmvc-ui` (en el
[`pom.xml`](barflow-products-api/pom.xml)); no necesita más configuración. Con la API encendida:

| URL | Qué muestra |
|---|---|
| <http://localhost:8080/swagger-ui.html> | Responde `302` y redirige a `/swagger-ui/index.html` (`200`), la interfaz donde se prueban los endpoints desde el navegador |
| <http://localhost:8080/v3/api-docs> | Responde `200` con la descripción de la API en JSON (OpenAPI 3) |

Swagger se genera solo, sin anotaciones: muestra `200` como código de éxito por defecto en
todos los endpoints. Los códigos reales de éxito (`201` al crear, `204` al borrar) son los de la
tabla de [Endpoints](#endpoints).

## Pruebas con Postman

La colección [`postman/barflow-products.postman_collection.json`](barflow-products-api/postman/barflow-products.postman_collection.json)
se importa en Postman con *Import*. Prueba 3 endpoints, uno de ellos un caso de error. La
ejecuté con Newman, el ejecutor de línea de comandos de Postman, con la API encendida; la salida
completa está en [`evidencias/postman.md`](barflow-products-api/evidencias/postman.md).

```bash
npx newman run postman/barflow-products.postman_collection.json
```

| # | Petición | Código obtenido | Qué significa |
|---|---|---|---|
| 1 | `GET /api/v1/products` | `200 OK` | La consulta funcionó y el cuerpo trae el arreglo de productos |
| 2 | `POST /api/v1/products` | `201 Created` | Se creó el producto; la cabecera `Location` apunta a su URL nueva |
| 3 | `GET /api/v1/products/00000000-0000-0000-0000-000000000000` | `404 Not Found` | El id está bien formado pero no existe: error del cliente, el cuerpo es `{"error":"NOT_FOUND",...}` |

Las 3 peticiones pasaron sus 6 verificaciones (0 fallidas).
