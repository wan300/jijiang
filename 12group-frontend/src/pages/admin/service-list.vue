<template>
  <view class="page-shell">
    <view class="top-bar surface-card">
      <text class="back" @click="goBack">< 返回</text>
      <text class="title">服务审核</text>
      <text style="width:80rpx;"></text>
    </view>

    <view class="tabs">
      <text :class="tab === 0 ? 'active' : ''" @click="switchTab(0)">待审核</text>
      <text :class="tab === 1 ? 'active' : ''" @click="switchTab(1)">已上架</text>
      <text :class="tab === 2 ? 'active' : ''" @click="switchTab(2)">已下架</text>
    </view>

    <view v-if="items.length === 0" style="text-align:center;padding:80rpx;color:#9aa5b8;">暂无数据</view>

    <view v-for="item in items" :key="item.id" class="card surface-card">
      <image v-if="item.coverUrl" :src="item.coverUrl" mode="aspectFill" class="cover" />
      <view class="card-body">
        <text class="svc-title">{{ item.title }}</text>
        <text class="muted">{{ item.sellerName }} · ¥{{ item.price }}</text>
        <text class="muted">库存 {{ item.stock }} · 销量 {{ item.salesCount }}</text>
        <text class="muted" style="margin-top:6rpx;">{{ item.createTime }}</text>
      </view>
      <view class="card-actions">
        <button v-if="tab === 0" class="reject-btn sm" @click="review(item.id, false)">驳回</button>
        <button v-if="tab === 0" class="pass-btn sm" @click="review(item.id, true)">上架</button>
        <button v-if="tab === 1" class="reject-btn sm" @click="offline(item.id)">下架</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getPendingServices, getServiceList, reviewService, offlineService, type AdminServiceItem } from "@/api/admin";
import { toast } from "@/utils/toast";

const items = ref<AdminServiceItem[]>([]);
const tab = ref(0);

onShow(() => { if (!checkAuth()) return; load(); });

function checkAuth() {
  if (!uni.getStorageSync("adminToken")) { uni.redirectTo({ url: "/pages/admin/login" }); return false; }
  return true;
}

function switchTab(t: number) {
  tab.value = t;
  load();
}

async function load() {
  try {
    let result;
    if (tab.value === 0) result = await getPendingServices({ pageSize: 50 });
    else result = await getServiceList({ status: tab.value, pageSize: 50 });
    items.value = result.items;
  } catch { /* ignore */ }
}

async function review(id: number, passed: boolean) {
  uni.showModal({
    title: passed ? "确认上架？" : "确认驳回？",
    editable: !passed,
    placeholderText: "驳回原因",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await reviewService(id, passed, passed ? undefined : (res.content || "未填写原因"));
        toast(passed ? "已上架" : "已驳回", "success");
        load();
      } catch { /* ignore */ }
    },
  });
}

async function offline(id: number) {
  uni.showModal({
    title: "确认下架？",
    editable: true,
    placeholderText: "下架原因",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await offlineService(id, res.content || "未填写原因");
        toast("已下架", "success");
        load();
      } catch { /* ignore */ }
    },
  });
}

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
  display: flex;
  gap: 24rpx;
  margin-bottom: 20rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: #9aa5b8;
}
.tabs .active {
  color: #1f4fd8;
  border-bottom: 4rpx solid #1f4fd8;
  padding-bottom: 8rpx;
}

.card {
  display: flex;
  margin-bottom: 16rpx;
  padding: 20rpx;
  gap: 16rpx;
}
.cover {
  width: 140rpx; height: 140rpx; border-radius: 16rpx; flex-shrink: 0;
}
.card-body {
  flex: 1; display: flex; flex-direction: column; gap: 6rpx;
}
.svc-title {
  font-size: 28rpx; font-weight: 900; color: #152033; line-height: 1.2;
}
.card-actions {
  display: flex; flex-direction: column; gap: 12rpx; justify-content: center;
}
.sm {
  padding: 14rpx 20rpx; border-radius: 16rpx; font-size: 24rpx; font-weight: 800;
}
.reject-btn { color: #dc2626; border: 1rpx solid #fca5a5; background: #fef2f2; }
.pass-btn { color: #fff; background: #1f4fd8; }
</style>
