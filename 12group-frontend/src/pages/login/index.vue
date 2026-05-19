<template>
  <view class="page-shell login">
    <view class="brand hero-card">
      <text class="eyebrow">Campus Craft Market</text>
      <text class="logo">技匠</text>
      <text class="slogan">把校园里的真本事，变成可信赖的服务。</text>
    </view>

    <view class="surface-card panel">
      <text class="title">微信授权登录</text>
      <text class="desc">用于实名认证、担保交易、订单沟通与信誉记录。</text>
      <button class="primary-btn" :loading="loading" @click="login">一键进入技匠</button>
      <button class="ghost-btn" @click="goHome">先逛逛</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();
const loading = ref(false);
const redirect = ref("/pages/tabbar/home/index");

onLoad((query) => {
  if (query?.redirect) redirect.value = decodeURIComponent(String(query.redirect));
});

async function login() {
  loading.value = true;
  try {
    await user.login();
    toast("欢迎来到技匠", "success");
    if (redirect.value.startsWith("/pages/tabbar/")) uni.switchTab({ url: redirect.value });
    else uni.redirectTo({ url: redirect.value });
  } finally {
    loading.value = false;
  }
}

function goHome() {
  uni.switchTab({ url: "/pages/tabbar/home/index" });
}
</script>

<style scoped>
.login {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 34rpx;
}

.brand {
  min-height: 430rpx;
}

.eyebrow {
  opacity: 0.76;
  font-size: 22rpx;
  letter-spacing: 3rpx;
}

.logo {
  display: block;
  margin-top: 58rpx;
  font-size: 86rpx;
  font-weight: 900;
}

.slogan {
  display: block;
  width: 520rpx;
  margin-top: 28rpx;
  font-size: 32rpx;
  font-weight: 700;
  line-height: 1.55;
}

.panel {
  padding: 38rpx;
}

.title {
  display: block;
  color: #152033;
  font-size: 38rpx;
  font-weight: 900;
}

.desc {
  display: block;
  margin: 16rpx 0 34rpx;
  color: #728099;
  font-size: 26rpx;
  line-height: 1.7;
}

.ghost-btn {
  margin-top: 20rpx;
}
</style>
