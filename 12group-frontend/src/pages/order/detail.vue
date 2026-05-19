<template>
  <view class="page-shell detail">
    <view v-if="order" class="hero-card order-head">
      <ji-status-pill :text="orderStatusText(order.status)" :tone="ORDER_STATUS[order.status]?.tone || 'idle'" />
      <text class="no">{{ order.orderNo }}</text>
      <text class="amount">¥{{ money(order.amount) }}</text>
    </view>

    <view v-if="order" class="surface-card card">
      <view class="row"><text>服务编号</text><text>#{{ order.serviceId }}</text></view>
      <view class="row"><text>买家</text><text>#{{ order.buyerId }}</text></view>
      <view class="row"><text>讲师</text><text>#{{ order.sellerId }}</text></view>
      <view class="row"><text>备注</text><text>{{ order.remark || "无" }}</text></view>
      <view v-if="order.deliverText" class="deliver">{{ order.deliverText }}</view>
    </view>

    <view v-if="order" class="surface-card card">
      <view class="section-title compact"><text>下一步</text></view>
      <button v-if="isBuyer && order.status === 10" class="primary-btn" :loading="paying" @click="continuePay">继续支付</button>
      <button v-if="isBuyer && order.status === 40" class="primary-btn" @click="confirm">确认完成</button>
      <button v-if="isBuyer && order.status === 50" class="primary-btn" @click="review">评价订单</button>
      <button v-if="isSeller && order.status === 20" class="primary-btn" @click="accept">接单</button>
      <view v-if="isSeller && order.status === 30" class="field">
        <view class="field-label">交付说明</view>
        <textarea v-model="deliverText" class="field-textarea" placeholder="说明已完成的内容、附件链接或课堂记录" />
        <button class="primary-btn" @click="deliver">提交交付</button>
      </view>
      <button class="ghost-btn" @click="chat">订单站内信</button>
      <button class="ghost-btn danger" @click="refund">申请退款/仲裁</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import JiStatusPill from "@/components/ji-status-pill.vue";
import { acceptOrder, confirmOrder, deliverOrder, getOrderDetail } from "@/api/order";
import { payOrder } from "@/api/payment";
import { useUserStore } from "@/store/user";
import type { OrderItem } from "@/types/domain";
import { money } from "@/utils/money";
import { ORDER_STATUS, orderStatusText } from "@/utils/status";
import { toast } from "@/utils/toast";

const user = useUserStore();
const orderId = ref(0);
const order = ref<OrderItem | null>(null);
const deliverText = ref("");
const paying = ref(false);
const isBuyer = computed(() => user.userInfo?.id === order.value?.buyerId);
const isSeller = computed(() => user.userInfo?.id === order.value?.sellerId);

onLoad(async (query) => {
  orderId.value = Number(query?.orderId || 0);
  await load();
});

async function load() {
  if (orderId.value) order.value = await getOrderDetail(orderId.value);
}

async function accept() {
  await acceptOrder(orderId.value);
  toast("已接单", "success");
  await load();
}

async function deliver() {
  await deliverOrder(orderId.value, deliverText.value || "讲师已完成交付");
  toast("已提交交付", "success");
  await load();
}

async function confirm() {
  await confirmOrder(orderId.value);
  toast("订单已完成", "success");
  await load();
}

async function continuePay() {
  if (!orderId.value || paying.value) return;
  paying.value = true;
  try {
    const cashier = await payOrder(orderId.value);
    uni.redirectTo({
      url: `/pages/order/pay-result?orderId=${orderId.value}&payUrl=${encodeURIComponent(cashier.payUrl)}&qrCodeUrl=${encodeURIComponent(cashier.qrCodeUrl || "")}`,
    });
  } finally {
    paying.value = false;
  }
}

function chat() {
  uni.navigateTo({ url: `/pages/chat/detail?orderId=${orderId.value}` });
}

function review() {
  uni.navigateTo({ url: `/pages/review/submit?orderId=${orderId.value}` });
}

function refund() {
  uni.navigateTo({ url: `/pages/order/refund?orderId=${orderId.value}` });
}
</script>

<style scoped>
.detail {
  padding-bottom: 40rpx;
}

.order-head {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.no {
  opacity: 0.82;
  font-size: 24rpx;
}

.amount {
  font-size: 58rpx;
  font-weight: 900;
}

.card {
  margin-top: 24rpx;
  padding: 30rpx;
}

.compact {
  margin-top: 0;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 20rpx 0;
  color: #5f6b80;
  border-bottom: 1rpx solid #edf2f8;
}

.row text:last-child {
  color: #152033;
  font-weight: 800;
}

.deliver {
  margin-top: 22rpx;
  border-radius: 24rpx;
  padding: 22rpx;
  color: #344158;
  background: #f3f7fb;
}

.ghost-btn {
  margin-top: 18rpx;
}

.danger {
  color: #d6455d;
}
</style>
