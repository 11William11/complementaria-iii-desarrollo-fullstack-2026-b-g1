import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, fetchProducts, login, saveToken } from "../api.js";

/** Respuesta falsa de fetch: solo lo que usa api.js (ok, status y json). */
const jsonResponse = (status, body) => ({
  ok: status >= 200 && status < 300,
  status,
  json: async () => body,
});

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

describe("api.js", () => {
  it("login devuelve el token cuando la API responde 200", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, { token: "abc.def.ghi" }));
    vi.stubGlobal("fetch", fetchMock);

    await expect(login("admin", "barflow123")).resolves.toBe("abc.def.ghi");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/auth/login",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("fetchProducts envía el token en la cabecera Authorization", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(200, []));
    vi.stubGlobal("fetch", fetchMock);
    saveToken("abc.def.ghi");

    await fetchProducts();

    const [, options] = fetchMock.mock.calls[0];
    expect(options.headers.Authorization).toBe("Bearer abc.def.ghi");
  });

  // ---------- Casos de error ----------

  it("un 401 lanza ApiError con el estado y el mensaje de la API", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(jsonResponse(401, { message: "invalid credentials" })));

    const error = await login("admin", "mala").catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(401);
    expect(error.message).toBe("invalid credentials");
  });

  it("si la API está apagada lanza ApiError con estado 0", async () => {
    // Así falla fetch cuando no hay respuesta (servidor apagado, sin red o CORS)
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    const error = await fetchProducts().catch((e) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.status).toBe(0);
    expect(error.message).toMatch(/No se pudo conectar con la API/);
  });
});
