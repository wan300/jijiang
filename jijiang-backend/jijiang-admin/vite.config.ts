import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const adminProxyTarget = process.env.VITE_ADMIN_PROXY_TARGET || "http://localhost:8081";

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      "/admin": {
        target: adminProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
