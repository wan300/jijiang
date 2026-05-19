<template>
  <view class="ji-order-card surface-card" @click="$emit('click', order)">
    <view class="ji-order-top">
      <text class="ji-order-no">{{ order.orderNo }}</text>
      <ji-status-pill :text="orderStatusText(order.status)" :tone="ORDER_STATUS[order.status]?.tone || 'idle'" />
    </view>
    <view class="ji-order-body">
      <view class="ji-order-icon">课</view>
      <view class="ji-order-info">
        <text class="ji-order-name">服务 #{{ order.serviceId }}</text>
        <text class="muted">对方用户 #{{ role === "seller" ? order.buyerId : order.sellerId }}</text>
      </view>
      <text class="ji-order-amount">¥{{ money(order.amount) }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import JiStatusPill from "@/components/ji-status-pill.vue";
import type { OrderItem } from "@/types/domain";
import { money } from "@/utils/money";
import { ORDER_STATUS, orderStatusText } from "@/utils/status";

withDefaults(defineProps<{ order: OrderItem; role?: "buyer" | "seller" }>(), { role: "buyer" });
defineEmits<{ click: [order: OrderItem] }>();
</script>

<style>
.ji-order-card {
  padding: 24rpx;
  margin-bottom: 22rpx;
}

.ji-order-top,
.ji-order-body {
  display: flex;
  align-items: center;
}

.ji-order-top {
  justify-content: space-between;
}

.ji-order-no {
  color: #6c7790;
  font-size: 23rpx;
}

.ji-order-body {
  gap: 20rpx;
  margin-top: 26rpx;
}

.ji-order-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 94rpx;
  height: 94rpx;
  border-radius: 28rpx;
  color: #fff;
  font-weight: 900;
  background: linear-gradient(135deg, #25c5a9, #1f4fd8);
}

.ji-order-info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10rpx;
}

.ji-order-name {
  color: #152033;
  font-size: 28rpx;
  font-weight: 800;
}

.ji-order-amount {
  color: #ff7a45;
  font-size: 30rpx;
  font-weight: 900;
}
</style>
