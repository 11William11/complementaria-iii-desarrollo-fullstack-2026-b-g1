# BarFlow — Entity y repository con JPA (Semana 7)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

Modelo la entidad **`Product`** del catálogo de BarFlow (los productos que vende
el bar) y su **`ProductRepository`**, siguiendo el diseño en capas de la
[semana 6](../../../06-week/02-optional-activity/barflow-layered-api/).

## Cómo ejecutarlo

Requisitos: JDK 17 y Maven 3.9.

```bash
mvn test
```

Las pruebas (`ProductRepositoryTest`, con `@DataJpaTest` y base de datos H2 en
memoria) crean la tabla, insertan productos y comprueban cada operación CRUD y
cada consulta. Resultado:

```
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

## Estructura

```
src/main/java/co/edu/corhuila/barflow/inventory/
├── InventoryApplication.java
├── entity/Product.java                 ← @Entity
└── repository/ProductRepository.java   ← extends JpaRepository
src/test/java/.../repository/ProductRepositoryTest.java
```

## 1. La entity: `Product`

[`entity/Product.java`](src/main/java/co/edu/corhuila/barflow/inventory/entity/Product.java)

| Atributo Java | Anotación | Columna generada | Por qué |
|---|---|---|---|
| `UUID id` | `@Id` + `@GeneratedValue(strategy = UUID)` | `id uuid not null` (PK) | Clave primaria; la genera JPA al guardar. BarFlow usa UUID en sus contratos. |
| `String name` | `@Column(nullable = false, length = 80)` | `name varchar(80) not null` + `unique` | Nombre del producto, sin repetir (`@UniqueConstraint uk_products_name`). |
| `String category` | `@Column(nullable = false, length = 40)` | `category varchar(40) not null` | Cervezas, Licores, Bebidas... |
| `Integer price` | `@Column(nullable = false)` | `price integer not null` | Pesos colombianos enteros (sin centavos). |
| `Integer stockQuantity` | `@Column(name = "stock_quantity")` | `stock_quantity integer not null` | Unidades disponibles. |
| `boolean active` | `@Column(name = "is_active")` | `is_active boolean not null` | Un producto retirado no se borra, se desactiva. |
| `Instant createdAt` | `@Column(updatable = false)` + `@PrePersist` | `created_at timestamp not null` | Se llena solo al insertar. |

`@Entity` le dice a JPA que la clase es una tabla y `@Table(name = "products")`
le da el nombre. Esta es la sentencia que Hibernate generó al correr las pruebas:

```sql
create table products (
    is_active boolean not null,
    price integer not null,
    stock_quantity integer not null,
    created_at timestamp(6) with time zone not null,
    id uuid not null,
    category varchar(40) not null,
    name varchar(80) not null,
    primary key (id),
    constraint uk_products_name unique (name)
)
```

La entity además protege las reglas de BarFlow: `changePrice` y `changeStock`
lanzan `IllegalArgumentException` si el valor es negativo (el stock nunca baja
de cero, **BR-02**), y tiene un constructor vacío `protected` porque JPA lo
necesita para crear el objeto al leer una fila.

## 2. El repository: `ProductRepository`

[`repository/ProductRepository.java`](src/main/java/co/edu/corhuila/barflow/inventory/repository/ProductRepository.java)

```java
public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc(String category);

    List<Product> findByStockQuantityLessThanEqualAndActiveTrue(int threshold);

    boolean existsByNameIgnoreCase(String name);
}
```

Es solo una interfaz: Spring Data crea la implementación al arrancar.
`JpaRepository<Product, UUID>` significa "repository de `Product` cuya llave es
`UUID`" y trae el CRUD ya hecho.

### Consultas por nombre de método

Spring Data lee el nombre del método y arma la consulta. Por ejemplo,
`findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc` se descompone así:

| Parte del nombre | Significa |
|---|---|
| `findBy` | `SELECT ... FROM products WHERE` |
| `CategoryIgnoreCase` | `upper(category) = upper(?)` |
| `And` `ActiveTrue` | `and is_active = true` |
| `OrderByNameAsc` | `order by name asc` |

SQL que generó Hibernate en la prueba:

```sql
select ... from products p1_0
where upper(p1_0.category) = upper(?) and p1_0.is_active
order by p1_0.name
```

| Método | Para qué lo usa BarFlow |
|---|---|
| `findByCategoryIgnoreCaseAndActiveTrueOrderByNameAsc("cervezas")` | Mostrar la carta por categoría, sin productos retirados. |
| `findByStockQuantityLessThanEqualAndActiveTrue(5)` | Alerta de inventario: qué hay que volver a pedir. |
| `existsByNameIgnoreCase("Cerveza Aguila")` | Evitar registrar dos veces el mismo producto. |

## 3. Las operaciones CRUD

Todas vienen heredadas de `JpaRepository`; no escribí SQL para ninguna.

| Operación | Método | SQL | Para qué en BarFlow |
|---|---|---|---|
| **Create** | `save(new Product(...))` | `INSERT` | El administrador registra un producto nuevo en la carta. |
| **Read** | `findAll()`, `findById(id)` | `SELECT` | Listar la carta; abrir el detalle de un producto al agregarlo a un pedido. `findById` devuelve `Optional`, así se maneja el "no existe" sin `null`. |
| **Update** | `save(productoModificado)` | `UPDATE` | Cambiar el precio o ajustar el stock tras un conteo. `save` sabe que es `UPDATE` porque la entity ya tiene `id`. |
| **Delete** | `deleteById(id)` | `DELETE` | Borrar un producto creado por error. En la operación normal BarFlow prefiere `retire()` (desactivar) para no perder el historial de ventas. |

Cada fila de esta tabla tiene su prueba en
[`ProductRepositoryTest`](src/test/java/co/edu/corhuila/barflow/inventory/repository/ProductRepositoryTest.java).

En la [semana 8](../../../08-week/02-optional-activity/barflow-products-crud/)
estas operaciones se exponen como endpoints REST con service y controller.
