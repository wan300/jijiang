<template>
  <view class="page-shell">
    <view v-if="service" class="surface-card card">
      <text class="title">{{ service.title }}</text>
      <text class="muted">讲师 #{{ service.sellerId }}</text>
      <view class="amount">¥{{ money(service.price) }}</view>
    </view>

    <view class="surface-card card">
      <view class="field">
        <view class="field-label">学习需求备注</view>
        <textarea v-model="remark" class="field-textarea" placeholder="写下希望上课的时间、目标和基础情况" />
      </view>
      <view class="agreement" @click="agreed = !agreed">
        <text class="check">{{ agreed ? "✓" : "" }}</text>
        <text>我同意平台担保交易规则，支付前不交换联系方式。</text>
      </view>
    </view>

    <button class="primary-btn submit" :disabled="!agreed || loading" :loading="loading" @click="submit">
      提交订单并支付
    </button>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { createOrder } from "@/api/order";
import { payOrder } from "@/api/payment";
import { getServiceDetail } from "@/api/service";
import { useOrderStore } from "@/store/order";
import { money } from "@/utils/money";
import { toast } from "@/utils/toast";
import type { ServiceItem } from "@/types/domain";

const orderStore = useOrderStore();
const service = ref<ServiceItem | null>(null);
const serviceId = ref(0);
const remark = ref("");
const agreed = ref(true);
const loading = ref(false);

onLoad(async (query) => {
  serviceId.value = Number(query?.serviceId || 0);
  service.value = orderStore.pendingService?.id === serviceId.value ? orderStore.pendingService : null;
  if (!service.value && serviceId.value) service.value = await getServiceDetail(serviceId.value);
});

async function submit() {
  if (!serviceId.value) return;
  loading.value = true;
  try {
    const order = await createOrder({ serviceId: serviceId.value, remark: remark.value });
    orderStore.setLastOrder(order.orderId);
    const cashier = await payOrder(order.orderId);
    toast("支付单已生成", "success");
    uni.redirectTo({
      url: `/pages/order/pay-result?orderId=${order.orderId}&payUrl=${encodeURIComponent(cashier.payUrl)}&qrCodeUrl=${encodeURIComponent(cashier.qrCodeUrl || "")}`,
    });
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.card {
  padding: 32rpx;
  margin-bottom: 24rpx;
}

.title {
  display: block;
  color: #152033;
  font-size: 34rpx;
  font-weight: 900;
}

.amount {
  margin-top: 24rpx;
  color: #ff7a45;
  font-size: 52rpx;
  font-weight: 900;
}

.agreement {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  color: #5f6b80;
  font-size: 24rpx;
  line-height: 1.6;
}

.check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34rpx;
  height: 34rpx;
  border-radius: 12rpx;
  color: #fff;
  background: #1f4fd8;
}

.submit {
  margin-top: 34rpx;
}
</style>
