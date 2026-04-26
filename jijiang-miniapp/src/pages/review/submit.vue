<template>
  <view class="page-shell">
    <view class="surface-card card">
      <text class="title">这次服务值得几颗星？</text>
      <view class="stars">
        <text v-for="n in 5" :key="n" :class="{ active: n <= score }" @click="score = n">★</text>
      </view>
      <textarea v-model="content" class="field-textarea" placeholder="分享讲师的专业度、响应速度和学习收获" />
      <button class="primary-btn" @click="submit">提交评价</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { submitReview } from "@/api/review";
import { toast } from "@/utils/toast";

const orderId = ref(0);
const score = ref(5);
const content = ref("");

onLoad((query) => {
  orderId.value = Number(query?.orderId || 0);
});

async function submit() {
  await submitReview({ orderId: orderId.value, score: score.value, content: content.value || "服务体验很好" });
  toast("评价成功", "success");
  uni.navigateBack();
}
</script>

<style scoped>
.card {
  padding: 36rpx;
}

.title {
  display: block;
  color: #152033;
  font-size: 36rpx;
  font-weight: 900;
}

.stars {
  display: flex;
  gap: 18rpx;
  margin: 30rpx 0;
}

.stars text {
  color: #cbd5e1;
  font-size: 60rpx;
}

.stars .active {
  color: #ffb020;
}

.primary-btn {
  margin-top: 28rpx;
}
</style>
