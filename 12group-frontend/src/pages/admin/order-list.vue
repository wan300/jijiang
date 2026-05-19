<template>
  <view class="page-shell">
    <view class="top-bar surface-card">
      <text class="back" @click="goBack">< 返回</text>
      <text class="title">订单管理</text>
      <text style="width:80rpx;"></text>
    </view>

    <input v-model="keyword" class="field-input" placeholder="搜索订单号/买家/卖家/服务" confirm-type="search" @confirm="load" style="margin-bottom:20rpx;" />

    <view class="tabs scroll-x">
      <text :class="cur === null ? 'active' : ''" @click="filter(null)">全部</text>
      <text v-for="s in statuses" :key="s.v" :class="cur === s.v ? 'active' : ''" @click="filter(s.v)">{{ s.label }}</text>
    </view>

    <view v-if="items.length === 0" style="text-align:center;padding:80rpx;color:#9aa5b8;">暂无订单</view>

    <view v-for="item in items" :key="item.id" class="card surface-card" @click="open(item.id)">
      <view class="row"><text class="bold">#{{ item.orderNo }}</text><ji-status-pill :text="orderStatusText(item.status)" :tone="ORDER_STATUS[item.status]?.tone || 'idle'" /></view>
      <view class="row"><text>{{ item.serviceTitle }}</text><text class="price">¥{{ item.amount }}</text></view>
      <view class="row muted"><text>买家: {{ item.buyerName }}</text><text>卖家: {{ item.sellerName }}</text></view>
      <view class="row muted"><text>{{ item.createTime }}</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getAdminOrderList, type AdminOrderItem } from "@/api/admin";
import JiStatusPill from "@/components/ji-status-pill.vue";
import { ORDER_STATUS, orderStatusText } from "@/utils/status";

const items = ref<AdminOrderItem[]>([]);
const cur = ref<number | null>(null);
const keyword = ref("");
const statuses = [
  { v: 10, label: "待付款" }, { v: 20, label: "待接单" },
  { v: 30, label: "已接单" }, { v: 40, label: "已交付" }, { v: 50, label: "已完成" },
];

onShow(() => { if (!checkAuth()) return; load(); });

function checkAuth() {
  if (!uni.getStorageSync("adminToken")) { uni.redirectTo({ url: "/pages/admin/login" }); return false; }
  return true;
}

function filter(s: number | null) { cur.value = s; load(); }

async function load() {
  try {
    const result = await getAdminOrderList({
      status: cur.value ?? undefined,
      keyword: keyword.value || undefined,
      pageSize: 50,
    });
    items.value = result.items;
  } catch { /* ignore */ }
}

function open(id: number) { uni.navigateTo({ url: `/pages/admin/order-detail?orderId=${id}` }); }
function goBack() { uni.navigateBack(); }
</script>

<style scoped>
.top-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 24rpx 30rpx; margin-bottom: 24rpx;
}
.back { color: #1f4fd8; font-size: 28rpx; font-weight: 700; }
.title { font-size: 32rpx; font-weight: 900; color: #152033; }

.tabs {
  display: flex; gap: 20rpx; margin-bottom: 20rpx; overflow-x: auto;
  font-size: 26rpx; font-weight: 700; color: #9aa5b8; white-space: nowrap;
}
.tabs .active { color: #1f4fd8; border-bottom: 4rpx solid #1f4fd8; padding-bottom: 8rpx; }

.card {
  margin-bottom: 16rpx; padding: 24rpx;
}
.row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8rpx 0; font-size: 26rpx;
}
.bold { font-weight: 900; color: #152033; }
.price { font-size: 32rpx; font-weight: 900; color: #1f4fd8; }
</style>
