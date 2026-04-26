<template>
  <view class="page-shell">
    <view class="hero-card">
      <text class="title">退款与仲裁</text>
      <text class="desc">先协商，再举证；平台会基于订单、聊天与凭证判断。</text>
    </view>
    <view class="surface-card form">
      <view class="field">
        <view class="field-label">期望退款金额</view>
        <input v-model="amount" class="field-input" type="digit" placeholder="请输入金额" />
      </view>
      <view class="field">
        <view class="field-label">原因说明</view>
        <textarea v-model="reason" class="field-textarea" placeholder="请说明未履约、交付不符或其他争议点" />
      </view>
      <button class="primary-btn" @click="submit">提交仲裁申请</button>
      <text class="tips">退款/仲裁接口待后端接入，当前页面先完成流程占位。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { hitSensitiveContact } from "@/utils/regex";
import { toast } from "@/utils/toast";

const orderId = ref(0);
const amount = ref("");
const reason = ref("");

onLoad((query) => {
  orderId.value = Number(query?.orderId || 0);
});

function submit() {
  if (!reason.value) {
    toast("请填写仲裁原因");
    return;
  }
  if (hitSensitiveContact(reason.value)) {
    toast("原因中请勿填写联系方式");
    return;
  }
  toast(`订单 #${orderId.value} 仲裁接口待接入`);
}
</script>

<style scoped>
.title,
.desc,
.tips {
  display: block;
}

.title {
  font-size: 42rpx;
  font-weight: 900;
}

.desc {
  margin-top: 18rpx;
  opacity: 0.82;
  line-height: 1.6;
}

.form {
  margin-top: 24rpx;
  padding: 34rpx;
}

.tips {
  margin-top: 24rpx;
  color: #7a869d;
  font-size: 24rpx;
  line-height: 1.6;
}
</style>
