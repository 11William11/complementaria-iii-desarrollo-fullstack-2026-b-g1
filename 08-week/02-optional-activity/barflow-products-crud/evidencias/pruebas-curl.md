# Evidencia de pruebas con curl

API levantada con `java -jar target/barflow-products-crud-0.0.1-SNAPSHOT.jar`
(4 productos de ejemplo cargados al arrancar). Ejecutado el 2026-09-24 13:11.

### 1. Listar productos (GET)

```bash
curl -i http://localhost:8080/api/v1/products
```

```http
HTTP/1.1 200 

[
  {
    "id": "f2f94c79-c9b3-4b99-a2fa-1be9df9b505e",
    "name": "Aguardiente Doble Anis",
    "category": "Licores",
    "price": 65000,
    "stockQuantity": 12,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.633409Z"
  },
  {
    "id": "68a00880-3229-4028-9ac9-9b3fa41de9cf",
    "name": "Cerveza Aguila",
    "category": "Cervezas",
    "price": 5000,
    "stockQuantity": 48,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.615406Z"
  },
  {
    "id": "47099b6a-010c-4474-a290-61f463b1d276",
    "name": "Club Colombia",
    "category": "Cervezas",
    "price": 6000,
    "stockQuantity": 36,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.632410Z"
  },
  {
    "id": "6d886951-456e-4303-8819-56394dbb545e",
    "name": "Gaseosa Postobon",
    "category": "Bebidas",
    "price": 3000,
    "stockQuantity": 24,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.633409Z"
  }
]
```

### 2. Filtrar por categoría (GET ?category=)

```bash
curl -i "http://localhost:8080/api/v1/products?category=cervezas"
```

```http
HTTP/1.1 200 

[
  {
    "id": "68a00880-3229-4028-9ac9-9b3fa41de9cf",
    "name": "Cerveza Aguila",
    "category": "Cervezas",
    "price": 5000,
    "stockQuantity": 48,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.615406Z"
  },
  {
    "id": "47099b6a-010c-4474-a290-61f463b1d276",
    "name": "Club Colombia",
    "category": "Cervezas",
    "price": 6000,
    "stockQuantity": 36,
    "isActive": true,
    "createdAt": "2026-09-24T18:10:53.632410Z"
  }
]
```

### 3. Crear un producto (POST)

```bash
curl -i -X POST http://localhost:8080/api/v1/products -H "Content-Type: application/json" -d '{"name":"Cerveza Poker","category":"Cervezas","price":4500,"stockQuantity":60}'
```

```http
HTTP/1.1 201 
Location: http://localhost:8080/api/v1/products/c4ca2783-2d53-4782-a4a9-b5b1abcaac83

{
  "id": "c4ca2783-2d53-4782-a4a9-b5b1abcaac83",
  "name": "Cerveza Poker",
  "category": "Cervezas",
  "price": 4500,
  "stockQuantity": 60,
  "isActive": true,
  "createdAt": "2026-09-24T18:11:02.345674500Z"
}
```

### 4. Obtener el producto creado (GET /{id})

```bash
curl -i http://localhost:8080/api/v1/products/c4ca2783-2d53-4782-a4a9-b5b1abcaac83
```

```http
HTTP/1.1 200 

{
  "id": "c4ca2783-2d53-4782-a4a9-b5b1abcaac83",
  "name": "Cerveza Poker",
  "category": "Cervezas",
  "price": 4500,
  "stockQuantity": 60,
  "isActive": true,
  "createdAt": "2026-09-24T18:11:02.345675Z"
}
```

### 5. Actualizar precio y stock (PUT /{id})

```bash
curl -i -X PUT http://localhost:8080/api/v1/products/c4ca2783-2d53-4782-a4a9-b5b1abcaac83 -H "Content-Type: application/json" -d '{"name":"Cerveza Poker","category":"Cervezas","price":4800,"stockQuantity":55}'
```

```http
HTTP/1.1 200 

{
  "id": "c4ca2783-2d53-4782-a4a9-b5b1abcaac83",
  "name": "Cerveza Poker",
  "category": "Cervezas",
  "price": 4800,
  "stockQuantity": 55,
  "isActive": true,
  "createdAt": "2026-09-24T18:11:02.345675Z"
}
```

### 6. Borrar el producto (DELETE /{id})

```bash
curl -i -X DELETE http://localhost:8080/api/v1/products/c4ca2783-2d53-4782-a4a9-b5b1abcaac83
```

```http
HTTP/1.1 204 

(sin cuerpo)
```

### 7. Confirmar que ya no existe (GET /{id} → 404)

```bash
curl -i http://localhost:8080/api/v1/products/c4ca2783-2d53-4782-a4a9-b5b1abcaac83
```

```http
HTTP/1.1 404 

{
  "error": "NOT_FOUND",
  "message": "product c4ca2783-2d53-4782-a4a9-b5b1abcaac83 does not exist"
}
```

### 8. Cuerpo inválido (POST → 400)

```bash
curl -i -X POST http://localhost:8080/api/v1/products -H "Content-Type: application/json" -d '{"name":"","category":"Cervezas","price":-100,"stockQuantity":10}'
```

```http
HTTP/1.1 400 

{
  "error": "VALIDATION_ERROR",
  "message": "the request body is not valid",
  "details": [
    {
      "field": "price",
      "message": "debe ser mayor que o igual a 0"
    },
    {
      "field": "name",
      "message": "no debe estar vacío"
    }
  ]
}
```

### 9. Nombre repetido (POST → 409)

```bash
curl -i -X POST http://localhost:8080/api/v1/products -H "Content-Type: application/json" -d '{"name":"cerveza aguila","category":"Cervezas","price":5000,"stockQuantity":10}'
```

```http
HTTP/1.1 409 

{
  "error": "CONFLICT",
  "message": "a product named 'cerveza aguila' already exists"
}
```

### 10. Id con formato inválido (GET → 400)

```bash
curl -i http://localhost:8080/api/v1/products/no-es-un-uuid
```

```http
HTTP/1.1 400 

{
  "error": "MALFORMED_REQUEST",
  "message": "id must be a valid UUID"
}
```

