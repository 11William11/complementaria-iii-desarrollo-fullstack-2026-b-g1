/**
 * barflow — Frontend integrado con la API (C3, semana 14)
 * Login contra la API real, guarda el JWT y lo envía para listar los productos,
 * que es un endpoint protegido. Las llamadas a la API están en api.js.
 *
 * Estructura del archivo:
 *  1. Referencias al DOM
 *  2. Vistas y estados (loading / data / error / empty)
 *  3. Eventos
 */
import { clearToken, fetchProducts, getToken, login, saveToken } from "./api.js";

// ---------- 1. Referencias al DOM ----------
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

// ---------- 2. Vistas y estados ----------
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

// ---------- 3. Eventos ----------
els.loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const username = els.loginForm.elements.username.value.trim();
  const password = els.loginForm.elements.password.value;
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
