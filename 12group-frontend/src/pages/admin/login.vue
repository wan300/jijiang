<template>
  <view class="page-shell" style="justify-content: center;">
    <view class="hero-card" style="margin-bottom: 34rpx;">
      <text style="display:block;font-size:22rpx;opacity:0.7;letter-spacing:3rpx;">ADMIN</text>
      <text style="display:block;margin-top:24rpx;font-size:48rpx;font-weight:900;">管理后台</text>
    </view>
    <view class="surface-card" style="padding:38rpx;">
      <view class="field">
        <view class="field-label">管理员账号</view>
        <input v-model="username" class="field-input" placeholder="请输入账号" />
      </view>
      <view class="field">
        <view class="field-label">密码</view>
        <input v-model="password" class="field-input" password placeholder="请输入密码" />
      </view>
      <button class="primary-btn" :loading="loading" @click="login">登录后台</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { adminLogin } from "@/api/admin";
import { toast } from "@/utils/toast";

const username = ref("admin");
const password = ref("");
const loading = ref(false);

async function login() {
  if (!username.value || !password.value) {
    toast("请输入账号和密码");
    return;
  }
  loading.value = true;
  try {
    const result = await adminLogin(username.value.trim(), password.value.trim());
    uni.setStorageSync("adminToken", result.accessToken);
    uni.setStorageSync("adminInfo", result.adminInfo);
    toast("登录成功", "success");
    uni.redirectTo({ url: "/pages/admin/dashboard" });
  } catch {
    // error handled in request
  } finally {
    loading.value = false;
  }
}
</script>
