# barflow — Vista de Pedidos (Corte 1, Semana 4)

Actividad calificable individual: diseño de mockup de una vista tipo lista y su
implementación como frontend que consume una API mediante `fetch`, manejando
los estados de **carga**, **datos** y **error**.

**barflow (bf)** es la plataforma de gestión que se está construyendo por
fases para bares y estaderos (pedidos, inventario, productos, ventas, mesas
y operación diaria). Esta entrega corresponde a la primera vista funcional:
el listado de pedidos activos.

## Overview (English)

barflow is a full-stack management platform for bars, built to handle
orders, inventory, products, sales, tables, and daily business operations.
This delivery implements its first working view: a live list of active
orders per table. The frontend is built with vanilla HTML, CSS, and
JavaScript, and consumes a sample API endpoint (`data/orders.json`) using
the native `fetch` API, in the same way it will later consume the real
backend once it is built with NestJS and PostgreSQL. The interface handles
three states: a **loading** state shown while the request is in flight, a
**data** state that renders each order as a reusable card component with
its table, items, total, and status, and an **error** state with a retry
button that appears if the request fails — which can be triggered on
demand with the "Simulate network failure" button for testing purposes.
A status filter also lets the staff narrow the list to pending, in
preparation, served, or paid orders.

## 🧩 Mockup de la vista (wireframe)

```
┌───────────────────────────────────────────────────────────┐
│  🍹 barflow                                                 │  ← Header
│  Gestión de pedidos, mesas e inventario para bares           │
└───────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ [ Estado ▾ ]      [ ↻ Recargar ]   [ ⚠️ Simular fallo ]      │  ← Controles
└───────────────────────────────────────────────────────────┘

  Estado CARGANDO        Estado ERROR              Estado DATOS
 ┌─────────────┐     ┌───────────────────┐   ┌──────────────┐ ┌──────────────┐
 │   (spinner) │     │ ⚠️ No se pudo      │   │ Mesa 3  Int. │ │ Mesa 7  Ext. │
 │  Cargando…  │     │ cargar.[Reintentar]│   │ · 2x Cerveza │ │ · 3x Aguard. │
 └─────────────┘     └───────────────────┘   │ $38.000 [prep│ │ $96.000[serv]│
                                               └──────────────┘ └──────────────┘
                                                (grid de tarjetas,
                                                 una por pedido)
└───────────────────────────────────────────────────────────┘
│  Fuente: data/orders.json consumido con fetch                │  ← Footer
└───────────────────────────────────────────────────────────┘
```

Solo un estado se muestra a la vez (loading, error, vacío o datos); el resto
permanece oculto (`hidden`) hasta que corresponda.

## 🗂️ Estructura del proyecto

```
barflow/
├── index.html          # Estructura de la vista y contenedores de cada estado
├── style.css            # Estilos (layout tipo grid, spinner, tarjetas, badges de estado)
├── app.js               # Fetch a la API, manejo de estados y render de componentes
├── data/
│   └── orders.json      # API de ejemplo: pedidos activos del bar
└── README.md             # Este archivo
```

## ⚙️ Cómo ejecutarlo

No requiere instalación ni dependencias (JavaScript vanilla).

**Servidor local (recomendado, `fetch` a un archivo local requiere http, no `file://`):**

```bash
cd barflow
python3 -m http.server 8080
# luego abre http://localhost:8080 en el navegador
```

## 🔌 API utilizada

- **API de ejemplo local**: `GET data/orders.json`, servida como cualquier
  recurso HTTP y consumida con `fetch`. Simula el endpoint de pedidos que
  más adelante expondrá el backend real de barflow (NestJS + PostgreSQL +
  Prisma), sin cambiar la forma en que el frontend la consume.

## ✅ Estados manejados

| Estado   | Descripción                                                          |
|----------|-----------------------------------------------------------------------|
| Carga    | Spinner mientras se espera la respuesta (con retardo simulado).       |
| Datos    | Grid de tarjetas de pedido (componente `renderOrderCard`).            |
| Error    | Si la petición falla (ruta inválida o HTTP != 200), muestra el mensaje y un botón "Reintentar". Se puede forzar con el botón "Simular fallo de red". |
| Vacío    | Si el filtro de estado no arroja resultados, se informa al usuario.   |

## 👤 Autor

- **Nombre:** William Erney Collo Narvaez
- **GitHub:** 11William11
- **Programa:** Ingeniería de Sistemas — CORHUILA
