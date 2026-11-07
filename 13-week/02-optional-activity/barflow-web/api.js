/**
 * barflow — Acceso a la API (semana 13)
 * Se separó de app.js para poder probarlo solo: aquí no se toca el DOM.
 */

export const API_URL = "http://localhost:8080/api/v1";
const TOKEN_KEY = "barflow.token";

// ---------- Token (localStorage) ----------
// Se guarda para no pedir login cada vez que se recarga la página.
export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const saveToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

// ---------- Llamadas a la API ----------
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export async function request(path, options = {}) {
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

export async function login(username, password) {
  const data = await request("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password }),
  });
  return data.token;
}

export function fetchProducts() {
  return request("/products", {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
}
