<template>
  <view class="page-shell">
    <view class="hero-card income">
      <text class="label">可提现余额</text>
      <text class="amount">¥0.00</text>
      <text class="desc">账户概览接口 /api/account/overview 待后端接入。</text>
    </view>
    <view class="surface-card card">
      <view class="row"><text>冻结中</text><text>¥0.00</text></view>
      <view class="row"><text>累计收入</text><text>待接入</text></view>
      <view class="row"><text>平台佣金</text><text>订单完成后按规则结算</text></view>
    </view>
    <button class="primary-btn" @click="withdraw">申请提现</button>
  </view>
</template>

<script setup lang="ts">
function withdraw() {
  uni.navigateTo({ url: "/pages/seller/withdraw" });
}
</script>

<style scoped>
.income {
  text-align: center;
}

.label,
.amount,
.desc {
  display: block;
}

.amount {
  margin: 18rpx 0;
  font-size: 70rpx;
  font-weight: 900;
}

.desc {
  opacity: 0.82;
}

.card {
  margin: 24rpx 0;
  padding: 30rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf2f8;
}
</style>
