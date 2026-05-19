<template>
  <view class="page-shell">
    <view class="top-bar surface-card">
      <text class="admin-name">{{ adminName }}</text>
      <text class="logout" @click="logout">退出</text>
    </view>

    <view class="metrics">
      <view class="metric surface-card" v-for="m in metricList" :key="m.label">
        <text class="val">{{ m.value }}</text>
        <text class="lbl">{{ m.label }}</text>
      </view>
    </view>

    <view class="section-title"><text>快捷操作</text></view>
    <view class="quick surface-card menu">
      <view class="row" @click="nav('/pages/admin/verify-list')">
        <text>实名认证审核</text>
        <text v-if="m.pendingVerifyTotal" class="badge">{{ m.pendingVerifyTotal }}</text>
        <text class="arrow">></text>
      </view>
      <view class="row" @click="nav('/pages/admin/service-list')">
        <text>服务审核</text>
        <text v-if="m.pendingServiceTotal" class="badge">{{ m.pendingServiceTotal }}</text>
        <text class="arrow">></text>
      </view>
      <view class="row" @click="nav('/pages/admin/order-list')">
        <text>订单管理</text>
        <text class="arrow">></text>
      </view>
    </view>

    <view class="section-title"><text>订单状态分布</text></view>
    <view class="surface-card" style="padding: 24rpx;">
      <view v-for="s in orderStatuses" :key="s.status" class="row">
        <text>{{ statusLabel(s.status) }}</text>
        <text class="num">{{ s.total }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getDashboard, type AdminMetrics } from "@/api/admin";
import { toast } from "@/utils/toast";

const adminName = computed(() => {
  const info = uni.getStorageSync("adminInfo");
  return info?.displayName || info?.username || "管理员";
});

const m = reactive<AdminMetrics>({
  userTotal: 0, verifiedUserTotal: 0, pendingVerifyTotal: 0,
  pendingServiceTotal: 0, onlineServiceTotal: 0, orderTotal: 0,
  paidGmv: 0, todayOrderTotal: 0,
});
const orderStatuses = ref<{ status: number; total: number }[]>([]);

const metricList = computed(() => [
  { label: "注册用户", value: m.userTotal },
  { label: "已实名", value: m.verifiedUserTotal },
  { label: "待审核", value: m.pendingVerifyTotal },
  { label: "在线服务", value: m.onlineServiceTotal },
  { label: "总订单", value: m.orderTotal },
  { label: "今日订单", value: m.todayOrderTotal },
]);

function statusLabel(s: number) {
  const map: Record<number, string> = { 10: "待付款", 20: "待接单", 30: "已接单", 40: "已交付", 50: "已完成" };
  return map[s] || `状态${s}`;
}

onShow(async () => {
  if (!uni.getStorageSync("adminToken")) {
    uni.redirectTo({ url: "/pages/admin/login" });
    return;
  }
  try {
    const data = await getDashboard();
    Object.assign(m, data.metrics);
    orderStatuses.value = data.orderStatus;
  } catch {
    // ignore
  }
});

function nav(url: string) {
  uni.navigateTo({ url });
}

function logout() {
  uni.removeStorageSync("adminToken");
  uni.removeStorageSync("adminInfo");
  toast("已退出管理后台");
  uni.switchTab({ url: "/pages/tabbar/mine/index" });
}
</script>

<style scoped>
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24rpx 30rpx;
  margin-bottom: 24rpx;
}

.admin-name {
  font-size: 32rpx;
  font-weight: 900;
  color: #152033;
}

.logout {
  color: #dc2626;
  font-size: 26rpx;
  font-weight: 700;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.metric {
  padding: 24rpx;
  text-align: center;
}

.val {
  display: block;
  font-size: 38rpx;
  font-weight: 900;
  color: #1f4fd8;
}

.lbl {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #7a869d;
}

.quick {
  padding: 10rpx 28rpx;
}

.row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #eef2f8;
  font-size: 28rpx;
  font-weight: 700;
  color: #1d2a42;
}

.row:last-child {
  border-bottom: 0;
}

.badge {
  background: #dc2626;
  color: #fff;
  border-radius: 999rpx;
  padding: 4rpx 16rpx;
  font-size: 22rpx;
  margin-left: auto;
  margin-right: 16rpx;
}

.arrow {
  color: #9aa5b8;
}

.num {
  color: #1f4fd8;
  font-weight: 900;
}
</style>
