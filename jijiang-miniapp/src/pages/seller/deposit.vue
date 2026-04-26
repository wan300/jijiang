<template>
  <view class="page-shell">
    <view class="hero-card">
      <text class="title">讲师保证金</text>
      <text class="amount">¥100</text>
      <text class="desc">用于约束履约与争议赔付。当前后端暂未实现保证金支付接口。</text>
    </view>
    <view class="surface-card card">
      <view class="row"><text>缴纳状态</text><text>{{ user.hasDeposit ? "已缴纳" : "待接入" }}</text></view>
      <view class="row"><text>退款规则</text><text>无未完结订单后可申请退还</text></view>
    </view>
    <button class="primary-btn" @click="pay">模拟了解规则</button>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();

function pay() {
  toast("保证金支付接口待接入");
}
</script>

<style scoped>
.title,
.amount,
.desc {
  display: block;
}

.title {
  font-size: 36rpx;
  font-weight: 900;
}

.amount {
  margin: 24rpx 0;
  font-size: 76rpx;
  font-weight: 900;
}

.desc {
  opacity: 0.82;
  line-height: 1.6;
}

.card {
  margin: 24rpx 0;
  padding: 30rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
}
</style>
