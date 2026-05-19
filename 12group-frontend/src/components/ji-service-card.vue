<template>
  <view class="ji-service-card surface-card" @click="$emit('click', service)">
    <view class="ji-service-cover" :style="{ backgroundImage: coverBg }">
      <text v-if="!service.coverUrl">技匠</text>
    </view>
    <view class="ji-service-content">
      <view class="ji-service-title-row">
        <text class="ji-service-title">{{ service.title }}</text>
        <text class="ji-service-price">¥{{ money(service.price) }}</text>
      </view>
      <text class="ji-service-desc">{{ service.description || "这位讲师暂未填写描述" }}</text>
      <view class="ji-service-meta">
        <text class="ji-service-meta-item">{{ service.sellerName || `讲师 #${service.sellerId}` }}</text>
        <text class="ji-service-meta-item">评分 {{ Number(service.scoreAvg || 5).toFixed(1) }}</text>
        <text class="ji-service-meta-item">成交 {{ service.salesCount || 0 }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { money } from "@/utils/money";
import type { ServiceItem } from "@/types/domain";

const props = defineProps<{ service: ServiceItem }>();
defineEmits<{ click: [service: ServiceItem] }>();

const coverBg = computed(() => (props.service.coverUrl ? `url(${props.service.coverUrl})` : ""));
</script>

<style>
.ji-service-card {
  display: flex;
  gap: 22rpx;
  padding: 20rpx;
  margin-bottom: 22rpx;
}

.ji-service-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 176rpx;
  height: 176rpx;
  border-radius: 28rpx;
  color: #fff;
  font-size: 34rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
  background-size: cover;
  background-position: center;
}

.ji-service-content {
  min-width: 0;
  flex: 1;
}

.ji-service-title-row {
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
  justify-content: space-between;
}

.ji-service-title {
  flex: 1;
  color: #152033;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 1.35;
}

.ji-service-price {
  color: #ff7a45;
  font-size: 30rpx;
  font-weight: 900;
}

.ji-service-desc {
  display: -webkit-box;
  overflow: hidden;
  margin-top: 12rpx;
  color: #6f7c93;
  font-size: 24rpx;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.ji-service-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
}

.ji-service-meta-item {
  border-radius: 999rpx;
  padding: 8rpx 14rpx;
  color: #59657a;
  font-size: 22rpx;
  background: #f0f4fb;
}
</style>
