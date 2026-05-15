import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || "http://localhost:8080";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      "/api": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      "/admin": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
      "/actuator": {
        target: apiProxyTarget,
        changeOrigin: true,
      },
    },
  },
});
