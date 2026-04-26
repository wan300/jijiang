<template>
  <view class="page-shell result">
    <view class="surface-card panel">
      <view class="mark">✓</view>
      <text class="title">支付成功</text>
      <text class="desc">资金已进入平台担保，等待讲师接单。</text>
      <button class="primary-btn" @click="detail">查看订单</button>
      <button class="ghost-btn" @click="home">回到首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";

const orderId = ref(0);
onLoad((query) => {
  orderId.value = Number(query?.orderId || 0);
});

function detail() {
  uni.redirectTo({ url: `/pages/order/detail?orderId=${orderId.value}` });
}

function home() {
  uni.switchTab({ url: "/pages/tabbar/home/index" });
}
</script>

<style scoped>
.result {
  display: flex;
  align-items: center;
  justify-content: center;
}

.panel {
  width: 100%;
  padding: 60rpx 36rpx;
  text-align: center;
}

.mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 140rpx;
  height: 140rpx;
  margin: 0 auto 30rpx;
  border-radius: 50%;
  color: #fff;
  font-size: 80rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #25c5a9, #1f4fd8);
}

.title,
.desc {
  display: block;
}

.title {
  color: #152033;
  font-size: 40rpx;
  font-weight: 900;
}

.desc {
  margin: 18rpx 0 40rpx;
  color: #718096;
}

.ghost-btn {
  margin-top: 20rpx;
}
</style>
