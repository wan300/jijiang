<template>
  <view class="page-shell">
    <view class="surface-card form">
      <view class="field">
        <view class="field-label">提现金额</view>
        <input v-model="amount" class="field-input" type="digit" placeholder="请输入金额" />
      </view>
      <view class="field">
        <view class="field-label">收款信息</view>
        <input v-model="account" class="field-input" placeholder="如 微信实名：张*" />
      </view>
      <button class="primary-btn" @click="submit">提交提现申请</button>
      <text class="tips">提现接口待后端接入，当前页面用于流程占位与交互验收。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { toast } from "@/utils/toast";

const amount = ref("");
const account = ref("");

function submit() {
  if (!amount.value || !account.value) {
    toast("请填写金额和收款信息");
    return;
  }
  toast("提现接口待接入");
}
</script>

<style scoped>
.form {
  padding: 34rpx;
}

.tips {
  display: block;
  margin-top: 24rpx;
  color: #7a869d;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
