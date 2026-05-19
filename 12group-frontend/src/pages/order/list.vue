<template>
  <view class="page-shell">
    <view class="tabs">
      <text class="tab" :class="{ active: role === 'buyer' }" @click="setRole('buyer')">我买到的</text>
      <text class="tab" :class="{ active: role === 'seller' }" @click="setRole('seller')">我卖出的</text>
    </view>
    <ji-empty v-if="orders.length === 0" title="暂无订单" desc="订单会按最新创建时间展示。" />
    <ji-order-card v-for="item in orders" :key="item.id" :order="item" :role="role" @click="open" />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import JiEmpty from "@/components/ji-empty.vue";
import JiOrderCard from "@/components/ji-order-card.vue";
import { listOrders } from "@/api/order";
import type { OrderItem } from "@/types/domain";

const role = ref<"buyer" | "seller">("buyer");
const orders = ref<OrderItem[]>([]);

onLoad((query) => {
  if (query?.role === "seller") role.value = "seller";
});
onShow(load);

async function load() {
  orders.value = await listOrders(role.value);
}

function setRole(value: "buyer" | "seller") {
  role.value = value;
  load();
}

function open(order: OrderItem) {
  uni.navigateTo({ url: `/pages/order/detail?orderId=${order.id}` });
}
</script>

<style scoped>
.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.tab {
  flex: 1;
  border-radius: 999rpx;
  padding: 20rpx;
  color: #718096;
  text-align: center;
  background: #fff;
}

.tab.active {
  color: #fff;
  font-weight: 900;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}
</style>
