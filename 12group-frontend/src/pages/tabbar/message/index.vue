<template>
  <view class="page-shell page-with-tab">
    <view class="hero-card slim">
      <text class="title">订单站内信</text>
      <text class="desc">沟通留在平台内，保障交易凭证完整。</text>
    </view>

    <ji-empty v-if="!user.isLogin" title="登录后查看消息" desc="订单沟通、系统提醒都会聚合在这里。" action-text="去登录" @action="login" />
    <template v-else>
      <ji-empty v-if="orders.length === 0" title="暂无会话" desc="创建订单后即可与讲师沟通。" />
      <view v-for="item in orders" :key="item.id" class="conversation surface-card" @click="openChat(item.id)">
        <view class="avatar">信</view>
        <view class="main">
          <text class="name">订单 {{ item.orderNo }}</text>
          <text class="muted">点击进入订单站内信</text>
        </view>
        <text class="status">{{ item.status }}</text>
      </view>
    </template>
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import JiEmpty from "@/components/ji-empty.vue";
import JiTabBar from "@/components/ji-tab-bar.vue";
import { listOrders } from "@/api/order";
import { useUserStore } from "@/store/user";
import type { OrderItem } from "@/types/domain";

const user = useUserStore();
const orders = ref<OrderItem[]>([]);

onShow(async () => {
  if (user.isLogin) orders.value = await listOrders(user.currentRole === 2 ? "seller" : "buyer");
});

function login() {
  uni.navigateTo({ url: "/pages/login/index" });
}

function openChat(orderId: number) {
  uni.navigateTo({ url: `/pages/chat/detail?orderId=${orderId}` });
}
</script>

<style scoped>
.slim {
  padding: 34rpx;
}

.title {
  display: block;
  font-size: 42rpx;
  font-weight: 900;
}

.desc {
  display: block;
  margin-top: 16rpx;
  opacity: 0.8;
  font-size: 25rpx;
}

.conversation {
  display: flex;
  align-items: center;
  gap: 22rpx;
  padding: 26rpx;
  margin-top: 22rpx;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 88rpx;
  height: 88rpx;
  border-radius: 30rpx;
  color: #fff;
  font-weight: 900;
  background: linear-gradient(135deg, #25c5a9, #1f4fd8);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.name {
  color: #152033;
  font-size: 28rpx;
  font-weight: 800;
}

.status {
  color: #1f4fd8;
  font-weight: 900;
}
</style>
