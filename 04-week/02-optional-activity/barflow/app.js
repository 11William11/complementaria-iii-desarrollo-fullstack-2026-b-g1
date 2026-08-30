/**
 * barflow — Vista de pedidos activos
 * Frontend que consume una API (de ejemplo) mediante fetch.
 *
 * La "API" es un endpoint estático de ejemplo (data/orders.json), servido
 * como cualquier recurso HTTP y consumido con fetch — cumple el requisito
 * de la actividad de "API pública o de ejemplo". Cuando barflow tenga
 * backend propio (NestJS), solo hay que cambiar API_URL.
 *
 * Estructura del archivo:
 *  1. Referencias al DOM
 *  2. Manejo de estados (loading / data / error / empty)
 *  3. Fetch a la API (con soporte para simular fallos)
 *  4. Componente: tarjeta de pedido (renderOrderCard)
 *  5. Filtro por estado del pedido
 *  6. Eventos
 */

// ---------- 1. Referencias al DOM ----------
const els = {
  loading: document.getElementById("state-loading"),
  error: document.getElementById("state-error"),
  errorDetail: document.getElementById("error-detail"),
  empty: document.getElementById("state-empty"),
  data: document.getElementById("state-data"),
  statusFilter: document.getElementById("status-filter"),
  reloadBtn: document.getElementById("reload-btn"),
  retryBtn: document.getElementById("retry-btn"),
  simulateErrorBtn: document.getElementById("simulate-error-btn"),
};

const API_URL = "data/orders.json";
const BROKEN_URL = "data/orders-not-found.json"; // usado solo para demostrar el estado de error

// Guardamos los pedidos ya cargados para poder filtrar sin volver a pedirlos
let allOrders = [];
let forceError = false; // se activa con el botón "Simular fallo de red"

// ---------- 2. Manejo de estados ----------
function setState(state) {
  els.loading.hidden = state !== "loading";
  els.error.hidden = state !== "error";
  els.empty.hidden = state !== "empty";
  els.data.hidden = state !== "data";
}

// ---------- 3. Fetch a la API ----------
async function loadOrders() {
  setState("loading");

  const url = forceError ? BROKEN_URL : API_URL;
  forceError = false; // el fallo simulado dura un solo intento

  try {
    // Pequeño retardo artificial para que el estado de carga sea visible
    await new Promise((resolve) => setTimeout(resolve, 500));

    const response = await fetch(url);

    if (!response.ok) {
      throw new Error(`Error HTTP ${response.status} al consultar ${url}`);
    }

    const orders = await response.json();
    allOrders = orders;

    populateStatusFilter(orders);
    applyFilterAndRender();
  } catch (error) {
    console.error("Error al cargar pedidos:", error);
    els.errorDetail.textContent = error.message;
    setState("error");
  }
}

// ---------- 4. Componente: tarjeta de pedido ----------
function statusClass(status) {
  return "status-" + status.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/\s+/g, "-");
}

function renderOrderCard(order) {
  const card = document.createElement("article");
  card.className = "order-card";

  const itemsHtml = order.items.map((item) => `<li>${item}</li>`).join("");

  card.innerHTML = `
    <div class="order-header">
      <span class="table-label">Mesa ${order.table}</span>
      <span class="zone-tag">${order.zone}</span>
    </div>
    <ul class="items">${itemsHtml}</ul>
    <div class="order-footer">
      <span class="total">$${order.total.toLocaleString("es-CO")}</span>
      <span class="status-badge ${statusClass(order.status)}">${order.status}</span>
    </div>
  `;

  return card;
}

function renderOrderList(orders) {
  els.data.innerHTML = "";

  if (orders.length === 0) {
    setState("empty");
    return;
  }

  const fragment = document.createDocumentFragment();
  orders.forEach((order) => {
    fragment.appendChild(renderOrderCard(order));
  });

  els.data.appendChild(fragment);
  setState("data");
}

// ---------- 5. Filtro por estado del pedido ----------
function populateStatusFilter(orders) {
  const statuses = [...new Set(orders.map((o) => o.status))];

  els.statusFilter.innerHTML = '<option value="all">Todos los estados</option>';

  statuses.forEach((status) => {
    const option = document.createElement("option");
    option.value = status;
    option.textContent = status;
    els.statusFilter.appendChild(option);
  });
}

function applyFilterAndRender() {
  const selected = els.statusFilter.value;

  const filtered =
    selected === "all" ? allOrders : allOrders.filter((o) => o.status === selected);

  renderOrderList(filtered);
}

// ---------- 6. Eventos ----------
els.statusFilter.addEventListener("change", applyFilterAndRender);
els.reloadBtn.addEventListener("click", loadOrders);
els.retryBtn.addEventListener("click", loadOrders);
els.simulateErrorBtn.addEventListener("click", () => {
  forceError = true;
  loadOrders();
});

// Carga inicial
loadOrders();
