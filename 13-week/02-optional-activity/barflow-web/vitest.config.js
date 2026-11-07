import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    // jsdom simula el navegador (document, localStorage) dentro de Node
    environment: "jsdom",
  },
});
