import { defineStore } from "pinia";
import { login as loginApi } from "@/api/auth";
import type { AdminInfo } from "@/types/admin";

interface AuthState {
  token: string;
  adminInfo: AdminInfo | null;
}

export const useAuthStore = defineStore("auth", {
  state: (): AuthState => ({
    token: localStorage.getItem("adminToken") || "",
    adminInfo: readAdminInfo(),
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
  },
  actions: {
    async login(username: string, password: string) {
      const result = await loginApi(username, password);
      this.token = result.accessToken;
      this.adminInfo = result.adminInfo;
      localStorage.setItem("adminToken", result.accessToken);
      localStorage.setItem("adminInfo", JSON.stringify(result.adminInfo));
    },
    logout() {
      this.token = "";
      this.adminInfo = null;
      localStorage.removeItem("adminToken");
      localStorage.removeItem("adminInfo");
    },
  },
});

function readAdminInfo() {
  const raw = localStorage.getItem("adminInfo");
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AdminInfo;
  } catch {
    localStorage.removeItem("adminInfo");
    return null;
  }
}
