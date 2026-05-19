import { fileURLToPath, URL } from "node:url";
import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const adminProxyTarget = env.VITE_ADMIN_PROXY_TARGET || "http://localhost:8080";

  return {
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
  };
});
