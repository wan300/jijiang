<template>
  <view class="page-shell page-with-tab">
    <view class="hero-card profile">
      <view class="avatar">{{ (user.userInfo?.nickname || "技").slice(0, 1) }}</view>
      <view class="info">
        <text class="name">{{ user.userInfo?.nickname || "未登录同学" }}</text>
        <text class="sub">{{ user.userInfo?.campusName || "登录后同步校区与信誉" }}</text>
      </view>
      <button v-if="!user.isLogin" class="login-btn" @click="login">登录</button>
    </view>

    <view class="stats surface-card">
      <view>
        <text class="num">{{ user.userInfo?.creditScore || 100 }}</text>
        <text class="label">信誉分</text>
      </view>
      <view>
        <text class="num">{{ verifyText }}</text>
        <text class="label">实名状态</text>
      </view>
      <view>
        <text class="num">{{ user.currentRole === 2 ? "讲师" : "买家" }}</text>
        <text class="label">当前身份</text>
      </view>
    </view>

    <view class="menu surface-card">
      <view v-for="item in menus" :key="item.text" class="row" @click="open(item.url)">
        <text>{{ item.text }}</text>
        <text class="arrow">></text>
      </view>
      <view class="row" @click="switchMode">
        <text>{{ user.currentRole === 2 ? "切回买家模式" : "进入讲师模式" }}</text>
        <text class="arrow">></text>
      </view>
    </view>
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import JiTabBar from "@/components/ji-tab-bar.vue";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();
const verifyText = computed(() => (user.isVerified ? "已实名" : user.userInfo?.verifyStatus === 1 ? "审核中" : "未实名"));
const menus = [
  { text: "实名认证", url: "/pages/user/verify" },
  { text: "个人资料", url: "/pages/user/profile" },
  { text: "我的信誉", url: "/pages/user/my-credit" },
  { text: "我的服务", url: "/pages/user/my-service" },
  { text: "收益中心", url: "/pages/seller/income" },
  { text: "保证金", url: "/pages/seller/deposit" },
  { text: "账号注销", url: "/pages/user/logout" },
  { text: "管理后台", url: "/pages/admin/dashboard" },
];

function login() {
  uni.navigateTo({ url: "/pages/login/index" });
}

function open(url: string) {
  if (url.startsWith("/pages/admin")) {
    const adminToken = uni.getStorageSync("adminToken");
    if (adminToken) uni.navigateTo({ url });
    else uni.navigateTo({ url: "/pages/admin/login" });
    return;
  }
  if (!user.isLogin && !url.includes("profile")) {
    login();
    return;
  }
  uni.navigateTo({ url });
}

function switchMode() {
  if (!user.isLogin) return login();
  if (user.currentRole === 2) {
    user.setLocalRole(1);
    uni.switchTab({ url: "/pages/tabbar/home/index" });
    return;
  }
  if (!user.isSellerVerified) toast("请先完成实名认证");
  user.setLocalRole(2);
  uni.reLaunch({ url: "/pages/tabbar/seller-desk/index" });
}
</script>

<style scoped>
.profile {
  display: flex;
  align-items: center;
  gap: 24rpx;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 116rpx;
  height: 116rpx;
  border-radius: 38rpx;
  color: #1f4fd8;
  font-size: 44rpx;
  font-weight: 900;
  background: rgba(255, 255, 255, 0.9);
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.name {
  font-size: 34rpx;
  font-weight: 900;
}

.sub {
  opacity: 0.82;
  font-size: 24rpx;
}

.login-btn {
  border-radius: 999rpx;
  padding: 16rpx 28rpx;
  color: #1f4fd8;
  font-weight: 900;
  background: #fff;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin-top: 24rpx;
  padding: 30rpx 12rpx;
  text-align: center;
}

.num,
.label {
  display: block;
}

.num {
  color: #152033;
  font-size: 30rpx;
  font-weight: 900;
}

.label {
  margin-top: 8rpx;
  color: #7a869d;
  font-size: 22rpx;
}

.menu {
  margin-top: 24rpx;
  padding: 10rpx 28rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #eef2f8;
  color: #1d2a42;
  font-size: 28rpx;
  font-weight: 700;
}

.row:last-child {
  border-bottom: 0;
}

.arrow {
  color: #9aa5b8;
}
</style>
