<template>
  <view class="page-shell">
    <view class="top-bar surface-card">
      <text class="back" @click="goBack">< 返回</text>
      <text class="title">订单详情</text>
      <text style="width:80rpx;"></text>
    </view>

    <template v-if="detail">
      <view class="surface-card section">
        <view class="row"><text class="label">订单号</text><text class="bold">{{ detail.order.orderNo }}</text></view>
        <view class="row"><text class="label">状态</text><ji-status-pill :text="orderStatusText(detail.order.status)" :tone="ORDER_STATUS[detail.order.status]?.tone || 'idle'" /></view>
        <view class="row"><text class="label">金额</text><text class="price">¥{{ detail.order.amount }}</text></view>
        <view class="row"><text class="label">买家</text><text>{{ detail.order.buyerName }}</text></view>
        <view class="row"><text class="label">卖家</text><text>{{ detail.order.sellerName }}</text></view>
        <view class="row"><text class="label">服务</text><text>{{ detail.order.serviceTitle }}</text></view>
        <view class="row"><text class="label">备注</text><text>{{ detail.order.remark || "无" }}</text></view>
        <view class="row"><text class="label">交付内容</text><text>{{ detail.order.deliverText || "无" }}</text></view>
      </view>

      <view class="surface-card section">
        <text class="section-title">时间线</text>
        <view class="row"><text class="label">创建</text><text>{{ detail.order.createTime }}</text></view>
        <view class="row"><text class="label">支付</text><text>{{ detail.order.payTime || "-" }}</text></view>
        <view class="row"><text class="label">接单</text><text>{{ detail.order.acceptTime || "-" }}</text></view>
        <view class="row"><text class="label">交付</text><text>{{ detail.order.deliverTime || "-" }}</text></view>
        <view class="row"><text class="label">完成</text><text>{{ detail.order.confirmTime || "-" }}</text></view>
      </view>

      <view class="surface-card section">
        <text class="section-title">支付记录</text>
        <view v-for="p in detail.payments" :key="p.id" class="row">
          <text>{{ p.payChannel }} · ¥{{ p.amount }}</text>
          <text>{{ p.status === 2 ? "已支付" : "待支付" }}</text>
        </view>
        <text v-if="!detail.payments.length" class="muted">无</text>
      </view>

      <view class="surface-card section">
        <text class="section-title">操作日志</text>
        <view v-for="l in detail.logs" :key="l.id" class="row">
          <text class="muted">{{ l.createTime }}</text>
          <text>{{ l.remark }}</text>
        </view>
      </view>
    </template>

    <view v-else style="text-align:center;padding:80rpx;">加载中...</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getAdminOrderDetail, type AdminOrderDetail } from "@/api/admin";
import JiStatusPill from "@/components/ji-status-pill.vue";
import { ORDER_STATUS, orderStatusText } from "@/utils/status";

const detail = ref<AdminOrderDetail | null>(null);

onLoad(async (query) => {
  if (!uni.getStorageSync("adminToken")) { uni.redirectTo({ url: "/pages/admin/login" }); return; }
  const orderId = Number(query?.orderId);
  if (!orderId) return;
  try {
    detail.value = await getAdminOrderDetail(orderId);
  } catch { /* ignore */ }
});

function goBack() { uni.navigateBack(); }
</script>

<style scoped>
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 30rpx; margin-bottom: 24rpx;
}
.back { color: #1f4fd8; font-size: 28rpx; font-weight: 700; }
.title { font-size: 32rpx; font-weight: 900; color: #152033; }

.section {
  margin-bottom: 20rpx; padding: 24rpx;
}
.section-title {
  display: block; margin-bottom: 16rpx; font-size: 30rpx; font-weight: 900; color: #152033;
}
.row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12rpx 0; font-size: 26rpx;
}
.label { color: #7a869d; min-width: 140rpx; }
.bold { font-weight: 900; color: #152033; }
.price { font-size: 32rpx; font-weight: 900; color: #1f4fd8; }
</style>
