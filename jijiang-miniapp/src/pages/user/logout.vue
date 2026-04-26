<template>
  <view class="page-shell">
    <view class="surface-card card">
      <text class="title">账号注销</text>
      <text class="desc">注销申请接口待后端接入。若需要退出当前设备，可先清除本地登录态。</text>
      <button class="primary-btn" @click="exit">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { logout } from "@/api/auth";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();

async function exit() {
  await logout().catch(() => undefined);
  user.clearSession();
  toast("已退出登录", "success");
  uni.switchTab({ url: "/pages/tabbar/home/index" });
}
</script>

<style scoped>
.card {
  padding: 36rpx;
}

.title,
.desc {
  display: block;
}

.title {
  color: #152033;
  font-size: 38rpx;
  font-weight: 900;
}

.desc {
  margin: 18rpx 0 34rpx;
  color: #748198;
  line-height: 1.7;
}
</style>
