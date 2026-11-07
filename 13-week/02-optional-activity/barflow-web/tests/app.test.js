import { readFileSync } from "node:fs";
import { join } from "node:path";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/**
 * Pruebas de componente: se carga el index.html real en jsdom, se importa app.js
 * (que conecta los eventos) y se usa la página como lo haría el usuario.
 * La API se reemplaza por un fetch falso.
 */

const html = readFileSync(join(import.meta.dirname, "..", "index.html"), "utf-8");

const jsonResponse = (status, body) => ({
  ok: status >= 200 && status < 300,
  status,
  json: async () => body,
});

const PRODUCTS = [
  { id: "1", name: "Cerveza Aguila", category: "Cervezas", price: 5000, stockQuantity: 48 },
  { id: "2", name: "Ron Medellín", category: "Licores", price: 65000, stockQuantity: 6 },
];

const $ = (selector) => document.querySelector(selector);

/** Monta la página y arranca app.js con el fetch falso indicado. */
async function startApp(fetchMock) {
  vi.stubGlobal("fetch", fetchMock);
  document.body.innerHTML = new DOMParser().parseFromString(html, "text/html").body.innerHTML;
  vi.resetModules(); // cada prueba vuelve a ejecutar app.js desde cero
  await import("../app.js");
}

function submitLogin(username, password) {
  $("#username").value = username;
  $("#password").value = password;
  $("#login-form").dispatchEvent(new Event("submit", { cancelable: true }));
}

beforeEach(() => localStorage.clear());
afterEach(() => vi.unstubAllGlobals());

describe("Vista de productos (app.js + index.html)", () => {
  it("login correcto: guarda el token y muestra la tabla con los productos", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(200, { token: "abc.def.ghi" })) // POST /auth/login
      .mockResolvedValueOnce(jsonResponse(200, PRODUCTS)); // GET /products
    await startApp(fetchMock);
    expect($("#login-view").hidden).toBe(false);

    submitLogin("admin", "barflow123");

    await vi.waitFor(() => expect($("#state-data").hidden).toBe(false));
    const rows = document.querySelectorAll("#products-body tr");
    expect(rows).toHaveLength(2);
    expect(rows[0].textContent).toContain("Cerveza Aguila");
    expect(rows[0].textContent).toContain("$5.000");
    expect(localStorage.getItem("barflow.token")).toBe("abc.def.ghi");
    expect(fetchMock.mock.calls[1][1].headers.Authorization).toBe("Bearer abc.def.ghi");
  });

  // ---------- Casos de error ----------

  it("token vencido (401): borra el token y vuelve al login con un aviso", async () => {
    localStorage.setItem("barflow.token", "token-vencido");
    await startApp(vi.fn().mockResolvedValue(jsonResponse(401, { message: "invalid or expired token" })));

    await vi.waitFor(() => expect($("#login-view").hidden).toBe(false));
    expect($("#products-view").hidden).toBe(true);
    expect($("#login-error").textContent).toBe("Tu sesión expiró. Inicia sesión de nuevo.");
    expect(localStorage.getItem("barflow.token")).toBeNull();
  });

  it("contraseña incorrecta: muestra el error y no guarda token", async () => {
    await startApp(vi.fn().mockResolvedValue(jsonResponse(401, { message: "invalid credentials" })));

    submitLogin("admin", "mala");

    await vi.waitFor(() => expect($("#login-error").hidden).toBe(false));
    expect($("#login-error").textContent).toBe("Usuario o contraseña incorrectos.");
    expect(localStorage.getItem("barflow.token")).toBeNull();
  });

  it("API apagada: muestra el panel de error con Reintentar", async () => {
    localStorage.setItem("barflow.token", "abc.def.ghi");
    await startApp(vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    await vi.waitFor(() => expect($("#state-error").hidden).toBe(false));
    expect($("#error-detail").textContent).toMatch(/No se pudo conectar con la API/);
    expect($("#retry-btn")).not.toBeNull();
  });
});
