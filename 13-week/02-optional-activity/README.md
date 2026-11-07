# BarFlow: pruebas de la aplicación (Semana 13)

Actividad opcional de refuerzo · Complementaria III — Desarrollo Fullstack · 2026-B
**Autor:** William Erney Collo Narvaez (`11William11`)

Al proyecto de la [semana 12](../../12-week/02-optional-activity/) (API con JWT + frontend) le
agregué **pruebas de backend** y **pruebas de frontend**. Las dos cubren casos de error, no solo el
camino feliz.

| Carpeta | Qué es |
|---|---|
| [`barflow-products-api/`](barflow-products-api/) | La API de la semana 12 + `ProductServiceTest` (JUnit 5 + Mockito) |
| [`barflow-web/`](barflow-web/) | El frontend de la semana 12 + pruebas con **Vitest** y **jsdom** |

## Cómo correr las pruebas

```bash
# Backend (JDK 17+ y Maven 3.9)
cd barflow-products-api
mvn test

# Frontend (Node 20+)
cd barflow-web
npm install
npm test
```

## 1. Prueba de backend (service)

[`ProductServiceTest`](barflow-products-api/src/test/java/co/edu/corhuila/barflow/inventory/service/ProductServiceTest.java)
prueba las reglas de negocio de `ProductService` **sin Spring, sin base de datos y sin HTTP**: el
`ProductRepository` es un *mock* de Mockito, así que la prueba solo depende del service.

| Prueba | Tipo | Qué comprueba |
|---|---|---|
| `createTrimsTheFieldsAndSavesTheProduct` | Camino feliz | Guarda el producto con el nombre y la categoría sin espacios sobrantes |
| `createWithADuplicateNameThrowsAndDoesNotSave` | **Error** | Si el nombre ya existe lanza `DuplicateProductException` y **nunca** llama a `save` |
| `getAnUnknownIdThrowsProductNotFound` | **Error** | Un id que no existe lanza `ProductNotFoundException` |
| `updateWithANegativeStockIsRejected` | **Límite** | Stock `-1` se rechaza (regla BR-02: el stock nunca queda por debajo de cero) |

Las pruebas de endpoint de las semanas 11 y 12 siguen en el proyecto (`ProductControllerTest`,
`AuthControllerTest`, `CorsTest`).

**Resultado (2026-11-07):** `mvn test` → **15 pruebas, 0 fallos** (las 11 que ya había + las 4
nuevas del service).

## 2. Prueba de frontend (componente)

Para poder probar el frontend lo separé en dos archivos (ahora son módulos ES, por eso el
`index.html` carga `<script type="module" src="app.js">`):

- [`api.js`](barflow-web/api.js): token en `localStorage` y llamadas a la API. No toca el DOM.
- [`app.js`](barflow-web/app.js): vistas, estados y eventos de la página.

Las pruebas usan **Vitest** con **jsdom**, que simula el navegador (DOM y `localStorage`) dentro de
Node. La API se reemplaza por un `fetch` falso (`vi.stubGlobal("fetch", ...)`), así que no hace
falta tenerla encendida.

[`tests/api.test.js`](barflow-web/tests/api.test.js): pruebas unitarias del cliente de la API.

| Prueba | Tipo |
|---|---|
| `login` devuelve el token cuando la API responde 200 | Camino feliz |
| `fetchProducts` envía la cabecera `Authorization: Bearer <token>` | Camino feliz |
| Un `401` lanza `ApiError` con el estado y el mensaje de la API | **Error** |
| Con la API apagada lanza `ApiError` con estado `0` | **Error** |

[`tests/app.test.js`](barflow-web/tests/app.test.js): prueba de **componente**. Carga el
`index.html` real en jsdom, importa `app.js` y usa la página como el usuario: llena el formulario,
lo envía y revisa lo que se muestra.

| Prueba | Tipo | Qué comprueba |
|---|---|---|
| Login correcto | Camino feliz | Guarda el token y la tabla muestra los 2 productos con el precio en pesos (`$5.000`) |
| Token vencido (`401`) | **Error** | Borra el token y vuelve al login con «Tu sesión expiró. Inicia sesión de nuevo.» |
| Contraseña incorrecta (`401`) | **Error** | Muestra «Usuario o contraseña incorrectos.» y no guarda token |
| API apagada | **Error** | Muestra el panel de error con el mensaje y el botón **Reintentar** |

**Resultado (2026-11-07):**

```text
 ✓ tests/api.test.js (4 tests)
 ✓ tests/app.test.js (4 tests)

 Test Files  2 passed (2)
      Tests  8 passed (8)
```

Un cambio pequeño en `app.js` salió de las pruebas: el formulario leía los campos con
`form.username`, que funciona en los navegadores pero no en jsdom. Ahora usa
`form.elements.username`, que es la forma estándar y funciona en los dos.

## Ejecutar la aplicación

Igual que en la semana 12: la API con `mvn spring-boot:run` (puerto 8080) y el frontend en el
puerto 5500 con `npx serve -l 5500 barflow-web`. Usuario de prueba: `admin` / `barflow123`.
