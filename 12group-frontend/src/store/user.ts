import { defineStore } from "pinia";
import { loginByCode, refreshToken, switchRole } from "@/api/auth";
import { getMe } from "@/api/user";
import { DEV_LOGIN_CODE } from "@/utils/env";
import type { Role, UserInfo } from "@/types/domain";

interface UserState {
  token: string;
  refreshTokenValue: string;
  userInfo: UserInfo | null;
  currentRole: Role;
}

export const useUserStore = defineStore("user", {
  state: (): UserState => ({
    token: uni.getStorageSync("token") || "",
    refreshTokenValue: uni.getStorageSync("refreshToken") || "",
    userInfo: uni.getStorageSync("userInfo") || null,
    currentRole: (uni.getStorageSync("currentRole") || 1) as Role,
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    isVerified: (state) => Number(state.userInfo?.verifyStatus) === 2,
    isSellerVerified: (state) => Number(state.userInfo?.isSellerVerified) === 1,
    hasDeposit: (state) => Number(state.userInfo?.depositPaid) === 1,
    campusId: (state) => Number(state.userInfo?.campusId || 1),
  },
  actions: {
    async login() {
      let code = DEV_LOGIN_CODE;
      // #ifdef MP-WEIXIN
      const wxLogin = await uni.login({ provider: "weixin" });
      code = wxLogin.code || DEV_LOGIN_CODE;
      // #endif
      const result = await loginByCode(code);
      this.applyLogin(result.accessToken, result.refreshToken, result.userInfo);
    },
    async refreshUserInfo() {
      try {
        const result = await refreshToken();
        this.applyLogin(result.accessToken, result.refreshToken, result.userInfo);
      } catch {
        const user = await getMe();
        this.userInfo = user;
        uni.setStorageSync("userInfo", user);
      }
    },
    async switchIdentity(role: Role) {
      const result = await switchRole(role);
      this.applyLogin(result.accessToken, result.refreshToken, result.userInfo);
    },
    applyLogin(token: string, refresh: string, info: UserInfo) {
      this.token = token;
      this.refreshTokenValue = refresh;
      this.userInfo = info;
      this.currentRole = info.currentRole || this.currentRole;
      uni.setStorageSync("token", token);
      uni.setStorageSync("refreshToken", refresh);
      uni.setStorageSync("userInfo", info);
      uni.setStorageSync("currentRole", this.currentRole);
    },
    setLocalRole(role: Role) {
      this.currentRole = role;
      uni.setStorageSync("currentRole", role);
    },
    clearSession() {
      this.token = "";
      this.refreshTokenValue = "";
      this.userInfo = null;
      this.currentRole = 1;
      uni.removeStorageSync("token");
      uni.removeStorageSync("refreshToken");
      uni.removeStorageSync("userInfo");
      uni.setStorageSync("currentRole", 1);
    },
    updateDepositPaid(paid: boolean) {
      if (this.userInfo) {
        this.userInfo = { ...this.userInfo, depositPaid: paid ? 1 : 0 };
        uni.setStorageSync("userInfo", this.userInfo);
      }
    },
    updateVerifyStatus(status: number) {
      if (this.userInfo) {
        this.userInfo = { ...this.userInfo, verifyStatus: status, isSellerVerified: status === 2 ? 1 : 0 };
        uni.setStorageSync("userInfo", this.userInfo);
      }
    },
  },
});
