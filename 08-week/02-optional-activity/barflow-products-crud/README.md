# BarFlow — CRUD REST de productos (Semana 8)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

CRUD REST completo del recurso **`Product`** de BarFlow, pasando por las cuatro
capas diseñadas en la [semana 6](../../../06-week/02-optional-activity/barflow-layered-api/):
entity → repository → service → controller. La entity y el repository son los de
la [semana 7](../../../07-week/02-optional-activity/barflow-entity-repository/).

## Cómo ejecutarlo

Requisitos: JDK 17 y Maven 3.9. No hace falta instalar base de datos: usa H2 en
memoria y carga 4 productos de ejemplo al arrancar.

```bash
mvn spring-boot:run      # API en http://localhost:8080/api/v1/products
mvn test                 # pruebas automáticas de extremo a extremo
```

## Endpoints

| Acción | Método | URL | Éxito | Errores |
|---|---|---|---|---|
| Listar | `GET` | `/api/v1/products` | `200` | — |
| Listar por categoría | `GET` | `/api/v1/products?category=cervezas` | `200` | — |
| Obtener | `GET` | `/api/v1/products/{id}` | `200` | `404` no existe · `400` id mal formado |
| Crear | `POST` | `/api/v1/products` | `201` + `Location` | `400` cuerpo inválido · `409` nombre repetido |
| Actualizar | `PUT` | `/api/v1/products/{id}` | `200` | `400` · `404` · `409` |
| Borrar | `DELETE` | `/api/v1/products/{id}` | `204` sin cuerpo | `404` |

Las URLs usan el **sustantivo en plural** (`products`) y la acción la dice el
**método HTTP**, nunca la URL (no hay `/getProducts` ni `/deleteProduct`). Siguen
las convenciones REST de BarFlow: base `/api/v1`, errores con el formato
`{ "error", "message", "details" }`.

Cuerpo para `POST` y `PUT`:

```json
{ "name": "Cerveza Poker", "category": "Cervezas", "price": 4500, "stockQuantity": 60 }
```

## Capas

```
src/main/java/co/edu/corhuila/barflow/inventory/
├── controller/ProductController.java      HTTP → service; códigos de estado
├── service/ProductService.java            reglas de negocio + @Transactional
├── repository/ProductRepository.java      JpaRepository<Product, UUID>
├── entity/Product.java                    @Entity + invariantes (precio y stock ≥ 0)
├── dto/ProductRequest.java                entrada validada con @Valid
├── dto/ProductResponse.java               salida (nunca se expone la entity)
├── exception/GlobalExceptionHandler.java  excepción → 400 / 404 / 409
└── config/SampleDataLoader.java           datos de ejemplo al arrancar
```

| Capa | Regla que respeta |
|---|---|
| Controller | Solo conoce al service. No tiene `if` de negocio ni llama al repository. |
| Service | No importa nada de HTTP. Lanza excepciones propias (`ProductNotFoundException`, `DuplicateProductException`). |
| Repository | Solo acceso a datos; las consultas salen del nombre del método. |
| Entity | No depende de ninguna otra capa. |

En `PUT` el service no llama a `save`: modifica la entity dentro de la
transacción y JPA hace el `UPDATE` al confirmarla (*dirty checking*).

## Evidencia de pruebas

### Pruebas automáticas

[`ProductControllerTest`](src/test/java/co/edu/corhuila/barflow/inventory/controller/ProductControllerTest.java)
levanta la aplicación completa (`@SpringBootTest` + `MockMvc`) y recorre
HTTP → controller → service → repository → H2:

| Prueba | Qué comprueba |
|---|---|
| `fullCrudFlow` | Crear (`201` + `Location`) → listar → obtener → actualizar → borrar (`204`) → obtener otra vez (`404`) |
| `listFiltersByCategory` | `?category=licores` devuelve solo los licores |
| `createRejectsInvalidBodyWithFieldDetails` | Nombre vacío y precio negativo → `400` con el detalle de los 2 campos |
| `createRejectsDuplicateNameWithConflict` | Mismo nombre (sin importar mayúsculas) → `409` |
| `unknownOrMalformedIdIsReported` | Id inexistente → `404`; id que no es UUID → `400` |

```
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Pruebas manuales con curl

Con la API corriendo probé los cinco endpoints y los casos de error. La sesión
completa, con cada petición y su respuesta, está en
[`evidencias/pruebas-curl.md`](evidencias/pruebas-curl.md). Resumen:

| # | Petición | Respuesta |
|---|---|---|
| 1 | `GET /api/v1/products` | `200` — 4 productos de ejemplo |
| 2 | `GET /api/v1/products?category=cervezas` | `200` — 2 cervezas |
| 3 | `POST /api/v1/products` (Cerveza Poker) | `201` + `Location: /api/v1/products/{id}` |
| 4 | `GET /api/v1/products/{id}` | `200` |
| 5 | `PUT /api/v1/products/{id}` (precio 4800) | `200` con el precio nuevo |
| 6 | `DELETE /api/v1/products/{id}` | `204` |
| 7 | `GET /api/v1/products/{id}` | `404 NOT_FOUND` |
| 8 | `POST` con nombre vacío y precio negativo | `400 VALIDATION_ERROR` |
| 9 | `POST` con nombre repetido | `409 CONFLICT` |
| 10 | `GET /api/v1/products/no-es-un-uuid` | `400 MALFORMED_REQUEST` |

Para repetirlas desde VS Code (extensión REST Client) o IntelliJ está
[`requests.http`](requests.http).

## Diferencias con BarFlow en producción

Esta entrega usa Spring Boot + JPA porque es el stack del curso. El backend real
de BarFlow se diseñó con NestJS + Prisma + PostgreSQL, pero con las mismas cuatro
capas y el mismo contrato (`/api/v1/products`, mismo formato de error). Dos
simplificaciones de esta práctica:

- `category` es texto; en BarFlow es una tabla `categories` aparte.
- `DELETE` borra la fila; en BarFlow un producto con ventas se retira
  (`isActive = false`) para no perder el historial.
