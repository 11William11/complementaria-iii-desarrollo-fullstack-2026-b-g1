/**
 * barflow — Frontend integrado con la API (semana 12)
 * Login contra la API real, guarda el JWT y lo envía para listar los productos,
 * que es un endpoint protegido.
 *
 * Estructura del archivo:
 *  1. Configuración y referencias al DOM
 *  2. Token (localStorage)
 *  3. Llamadas a la API (login y productos)
 *  4. Vistas y estados (loading / data / error / empty)
 *  5. Eventos
 */

// ---------- 1. Configuración y referencias al DOM ----------
const API_URL = "http://localhost:8080/api/v1";
const TOKEN_KEY = "barflow.token";

const els = {
  loginView: document.getElementById("login-view"),
  loginForm: document.getElementById("login-form"),
  loginBtn: document.getElementById("login-btn"),
  loginError: document.getElementById("login-error"),
  productsView: document.getElementById("products-view"),
  logoutBtn: document.getElementById("logout-btn"),
  reloadBtn: document.getElementById("reload-btn"),
  retryBtn: document.getElementById("retry-btn"),
  loading: document.getElementById("state-loading"),
  error: document.getElementById("state-error"),
  errorDetail: document.getElementById("error-detail"),
  empty: document.getElementById("state-empty"),
  data: document.getElementById("state-data"),
  productsBody: document.getElementById("products-body"),
};

// ---------- 2. Token (localStorage) ----------
// Se guarda para no pedir login cada vez que se recarga la página.
const getToken = () => localStorage.getItem(TOKEN_KEY);
const saveToken = (token) => localStorage.setItem(TOKEN_KEY, token);
const clearToken = () => localStorage.removeItem(TOKEN_KEY);

// ---------- 3. Llamadas a la API ----------
class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(API_URL + path, options);
  } catch {
    // fetch solo falla así cuando no hubo respuesta: API apagada, sin red o bloqueada por CORS
    throw new ApiError(0, "No se pudo conectar con la API. Revisa que esté encendida en el puerto 8080.");
  }

  // fetch NO lanza error con 401, 404 o 500: hay que revisar response.ok a mano
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(response.status, body?.message ?? `Error HTTP ${response.status}`);
  }
  return body;
}

async function login(username, password) {
  const data = await request("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  return data.token;
}

function fetchProducts() {
  return request("/products", {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
}

// ---------- 4. Vistas y estados ----------
function showView(view) {
  els.loginView.hidden = view !== "login";
  els.productsView.hidden = view !== "products";
  els.logoutBtn.hidden = view !== "products";
}

function setState(state) {
  els.loading.hidden = state !== "loading";
  els.error.hidden = state !== "error";
  els.empty.hidden = state !== "empty";
  els.data.hidden = state !== "data";
}

function showLoginError(message) {
  els.loginError.textContent = message;
  els.loginError.hidden = !message;
}

async function loadProducts() {
  setState("loading");
  try {
    const products = await fetchProducts();
    renderProducts(products);
  } catch (error) {
    if (error.status === 401) {
      // El token venció o no es válido: hay que volver a iniciar sesión
      logout("Tu sesión expiró. Inicia sesión de nuevo.");
      return;
    }
    els.errorDetail.textContent = error.message;
    setState("error");
  }
}

function renderProducts(products) {
  if (products.length === 0) {
    setState("empty");
    return;
  }

  els.productsBody.innerHTML = "";
  products.forEach((product) => {
    const row = document.createElement("tr");
    // textContent (no innerHTML) para que un nombre con HTML no se ejecute en la página
    [
      product.name,
      product.category,
      `$${product.price.toLocaleString("es-CO")}`,
      product.stockQuantity,
    ].forEach((value, i) => {
      const cell = document.createElement("td");
      cell.textContent = value;
      if (i >= 2) cell.className = "num";
      row.appendChild(cell);
    });
    els.productsBody.appendChild(row);
  });
  setState("data");
}

function logout(message = "") {
  clearToken();
  showView("login");
  showLoginError(message);
}

// ---------- 5. Eventos ----------
els.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const username = els.loginForm.username.value.trim();
  const password = els.loginForm.password.value;
  if (!username || !password) {
    showLoginError("Escribe el usuario y la contraseña.");
    return;
  }

  showLoginError("");
  els.loginBtn.disabled = true;
  els.loginBtn.textContent = "Ingresando…";
  try {
    saveToken(await login(username, password));
    els.loginForm.reset();
    showView("products");
    loadProducts();
  } catch (error) {
    showLoginError(error.status === 401 ? "Usuario o contraseña incorrectos." : error.message);
  } finally {
    els.loginBtn.disabled = false;
    els.loginBtn.textContent = "Ingresar";
  }
});

els.logoutBtn.addEventListener("click", () => logout());
els.reloadBtn.addEventListener("click", loadProducts);
els.retryBtn.addEventListener("click", loadProducts);

// Al abrir: si ya hay un token guardado se va directo a los productos
if (getToken()) {
  showView("products");
  loadProducts();
} else {
  showView("login");
}
