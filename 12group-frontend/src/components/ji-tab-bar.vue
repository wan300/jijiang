<template>
  <view class="ji-tab-bar">
    <view
      v-for="item in tabs"
      :key="item.url"
      class="ji-tab-item"
      :class="{ 'ji-tab-active': current === item.url }"
      @click="jump(item.url)"
    >
      <text class="ji-tab-icon">{{ item.icon }}</text>
      <text>{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useUserStore } from "@/store/user";

const user = useUserStore();
const buyerTabs = [
  { text: "首页", icon: "⌂", url: "/pages/tabbar/home/index" },
  { text: "发现", icon: "✦", url: "/pages/tabbar/discover/index" },
  { text: "消息", icon: "✉", url: "/pages/tabbar/message/index" },
  { text: "我的", icon: "◉", url: "/pages/tabbar/mine/index" },
];
const sellerTabs = [
  { text: "工作台", icon: "⌁", url: "/pages/tabbar/seller-desk/index" },
  { text: "订单", icon: "▣", url: "/pages/tabbar/seller-order/index" },
  { text: "服务", icon: "✎", url: "/pages/tabbar/seller-service/index" },
  { text: "我的", icon: "◉", url: "/pages/tabbar/mine/index" },
];

const tabs = computed(() => (user.currentRole === 2 ? sellerTabs : buyerTabs));
const current = computed(() => {
  const pages = getCurrentPages();
  const route = pages[pages.length - 1]?.route || "";
  return `/${route}`;
});

function jump(url: string) {
  if (current.value === url) return;
  const nativeTab = buyerTabs.some((item) => item.url === url);
  if (nativeTab) uni.switchTab({ url });
  else uni.reLaunch({ url });
}
</script>

<style>
.ji-tab-bar {
  position: fixed;
  right: 24rpx;
  bottom: 22rpx;
  left: 24rpx;
  z-index: 90;
  display: flex;
  justify-content: space-around;
  height: 104rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.85);
  border-radius: 36rpx;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 18rpx 50rpx rgba(28, 43, 76, 0.16);
}

.ji-tab-item {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  flex-direction: column;
  gap: 6rpx;
  color: #8a95aa;
  font-size: 21rpx;
  font-weight: 700;
}

.ji-tab-icon {
  font-size: 30rpx;
}

.ji-tab-active {
  color: #1f4fd8;
}
</style>
