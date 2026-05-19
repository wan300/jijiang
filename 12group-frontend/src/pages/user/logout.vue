<template>
  <view class="page-shell">
    <view class="surface-card card">
      <text class="title">账号注销</text>

      <!-- 冷静期中 -->
      <template v-if="deletionStatus === 1">
        <view class="notice">
          <text class="notice-title">冷静期中</text>
          <text class="notice-desc">注销将于 {{ coolingUntil }} 后生效，在此之前可随时取消。</text>
        </view>
        <button class="primary-btn" :loading="loading" @click="cancelDelete">取消注销</button>
      </template>

      <!-- 已注销 -->
      <template v-else-if="deletionStatus === 2">
        <view class="notice">
          <text class="notice-title done">账号已注销</text>
          <text class="notice-desc">您的账号已完成注销。</text>
        </view>
      </template>

      <!-- 默认：未申请或已取消 -->
      <template v-else>
        <text class="desc">注销后将清除您的实名信息和账号数据，订单记录将匿名保留。提交后有 7 天冷静期，期间可随时取消。</text>
        <button class="danger-btn" :loading="loading" @click="requestDelete">申请注销账号</button>
      </template>
    </view>

    <view class="surface-card card">
      <text class="card-title">退出当前设备</text>
      <text class="desc">仅清除本地登录记录，不影响账号数据。</text>
      <button class="ghost-btn" @click="doExit">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { logout } from "@/api/auth";
import { requestDeletion, cancelDeletion, getDeletionStatus } from "@/api/deletion";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();
const loading = ref(false);
const deletionStatus = ref(0);
const coolingUntil = ref("");

async function fetchStatus() {
  try {
    const result = await getDeletionStatus();
    deletionStatus.value = result.deletionStatus || 0;
    if (result.coolingUntil) {
      coolingUntil.value = result.coolingUntil.replace("T", " ");
    }
  } catch {
    deletionStatus.value = 0;
  }
}

async function requestDelete() {
  loading.value = true;
  try {
    const result = await requestDeletion();
    toast(result.message, "success");
    await fetchStatus();
  } catch {
    // 已在 request 中处理
  } finally {
    loading.value = false;
  }
}

async function cancelDelete() {
  loading.value = true;
  try {
    const result = await cancelDeletion();
    toast(result.message, "success");
    deletionStatus.value = 0;
  } catch {
    // 已在 request 中处理
  } finally {
    loading.value = false;
  }
}

function doExit() {
  user.clearSession();
  uni.clearStorageSync();
  uni.reLaunch({ url: "/pages/tabbar/home/index" });
}

fetchStatus();
</script>

<style scoped>
.card {
  margin-bottom: 24rpx;
  padding: 36rpx;
}

.title,
.card-title {
  color: #152033;
  font-size: 38rpx;
  font-weight: 900;
}

.card-title {
  font-size: 30rpx;
}

.desc {
  display: block;
  margin: 18rpx 0 34rpx;
  color: #748198;
  font-size: 26rpx;
  line-height: 1.7;
}

.notice {
  margin: 18rpx 0 34rpx;
  padding: 24rpx;
  border-radius: 16rpx;
  background: #fef9c3;
}

.notice-title {
  display: block;
  font-size: 32rpx;
  font-weight: 900;
  color: #a16207;
}

.notice-title.done {
  color: #6b7280;
}

.notice-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 26rpx;
  color: #854d0e;
  line-height: 1.6;
}

.danger-btn {
  width: 100%;
  margin-top: 20rpx;
  padding: 28rpx 0;
  border-radius: 28rpx;
  color: #fff;
  font-size: 30rpx;
  font-weight: 800;
  background: #dc2626;
}

.ghost-btn {
  width: 100%;
  margin-top: 20rpx;
  padding: 28rpx 0;
  border: 2rpx solid #d1d5db;
  border-radius: 28rpx;
  color: #374151;
  font-size: 30rpx;
  font-weight: 800;
  background: transparent;
}
</style>
