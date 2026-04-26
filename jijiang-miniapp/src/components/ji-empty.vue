<template>
  <view class="empty">
    <view class="orb">匠</view>
    <text class="title">{{ title }}</text>
    <text class="desc">{{ desc }}</text>
    <button v-if="actionText" class="ghost-btn action" @click="$emit('action')">{{ actionText }}</button>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  desc?: string;
  actionText?: string;
}>();
defineEmits<{ action: [] }>();
</script>

<style scoped>
.empty {
  padding: 70rpx 36rpx;
  text-align: center;
}

.orb {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132rpx;
  height: 132rpx;
  margin: 0 auto 24rpx;
  border-radius: 44rpx;
  color: #fff;
  font-size: 52rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}

.title {
  display: block;
  color: #152033;
  font-size: 30rpx;
  font-weight: 800;
}

.desc {
  display: block;
  margin-top: 12rpx;
  color: #7a869d;
  font-size: 24rpx;
  line-height: 1.7;
}

.action {
  width: 260rpx;
  margin: 28rpx auto 0;
}
</style>
