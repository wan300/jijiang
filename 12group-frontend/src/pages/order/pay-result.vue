<template>
  <view class="page-shell result">
    <view class="surface-card panel">
      <view class="mark" :class="{ pending: !paid }">{{ paid ? "✓" : "¥" }}</view>
      <text class="title">{{ paid ? "支付成功" : "等待支付确认" }}</text>
      <text class="desc">
        {{ paid ? "资金已进入平台担保，等待讲师接单。" : "完成付款后，页面会自动同步订单状态。" }}
      </text>

      <template v-if="paid">
        <button class="primary-btn" @click="detail">查看订单</button>
        <button class="ghost-btn" @click="home">回到首页</button>
      </template>

      <template v-else>
        <button v-if="payUrl" class="primary-btn" @click="openCashier">打开收银台</button>
        <button v-if="qrCodeUrl" class="ghost-btn" @click="previewQr">查看付款码</button>
        <button class="ghost-btn" :loading="checking" @click="checkPaid">刷新状态</button>
        <button class="ghost-btn" @click="detail">查看订单</button>
      </template>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onUnload } from "@dcloudio/uni-app";
import { getOrderDetail } from "@/api/order";
import { syncPaymentStatus } from "@/api/payment";
import { toast } from "@/utils/toast";

const orderId = ref(0);
const payUrl = ref("");
const qrCodeUrl = ref("");
const paid = ref(false);
const checking = ref(false);
let timer: ReturnType<typeof setInterval> | undefined;

onLoad((query) => {
  orderId.value = Number(query?.orderId || 0);
  payUrl.value = normalizeExternalUrl(decodeURIComponent(String(query?.payUrl || "")), "");
  qrCodeUrl.value = normalizeExternalUrl(decodeURIComponent(String(query?.qrCodeUrl || "")), payUrl.value);
  timer = setInterval(checkPaid, 2000);
  checkPaid();
});

onUnload(() => {
  if (timer) clearInterval(timer);
});

function openCashier() {
  if (!payUrl.value) return;
  uni.navigateTo({ url: `/pages/common/webview?url=${encodeURIComponent(payUrl.value)}&title=${encodeURIComponent("虎皮椒收银台")}` });
}

function previewQr() {
  if (!qrCodeUrl.value) return;
  uni.previewImage({ urls: [qrCodeUrl.value] });
}

function normalizeExternalUrl(value: string, baseUrl: string) {
  const trimmed = value.trim();
  if (!trimmed) return "";
  if (trimmed.startsWith("//")) return `https:${trimmed}`;
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  if (!baseUrl) return trimmed;
  try {
    return new URL(trimmed, baseUrl).toString();
  } catch {
    return trimmed;
  }
}

async function checkPaid() {
  if (!orderId.value || checking.value || paid.value) return;
  checking.value = true;
  try {
    const synced = await syncPaymentStatus(orderId.value);
    if (synced.paid || synced.status >= 20) {
      paid.value = true;
      if (timer) clearInterval(timer);
      toast("鏀粯鎴愬姛", "success");
      return;
    }
    const order = await getOrderDetail(orderId.value);
    if (order.status >= 20) {
      paid.value = true;
      if (timer) clearInterval(timer);
      toast("支付成功", "success");
    }
  } finally {
    checking.value = false;
  }
}

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
  font-size: 72rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #25c5a9, #1f4fd8);
}

.mark.pending {
  background: linear-gradient(135deg, #ffb84d, #ff7a45);
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
