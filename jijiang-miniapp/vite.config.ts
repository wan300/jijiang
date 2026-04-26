import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [uni()],
  server: {
    proxy: {
      "/api": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
      "/admin": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
      "/actuator": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
