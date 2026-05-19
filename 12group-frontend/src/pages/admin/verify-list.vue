<template>
  <view class="page-shell">
    <view class="top-bar surface-card">
      <text class="back" @click="goBack">< 返回</text>
      <text class="title">实名认证审核</text>
      <text style="width:80rpx;"></text>
    </view>

    <input v-model="keyword" class="field-input" placeholder="搜索姓名/学号/用户" confirm-type="search" @confirm="load" style="margin-bottom:20rpx;" />

    <view v-if="items.length === 0" style="text-align:center;padding:80rpx;color:#9aa5b8;">暂无待审核记录</view>

    <view v-for="item in items" :key="item.id" class="verify-card surface-card">
      <view class="card-row">
        <text class="label">用户</text>
        <text>{{ item.nickname }}（#{{ item.userId }}）</text>
      </view>
      <view class="card-row">
        <text class="label">姓名</text>
        <text class="bold">{{ item.realName }}</text>
      </view>
      <view class="card-row">
        <text class="label">学号</text>
        <text>{{ item.studentNo }}</text>
      </view>
      <view class="card-row">
        <text class="label">学校</text>
        <text>{{ item.campusName }}</text>
      </view>
      <view class="card-row">
        <text class="label">OCR置信度</text>
        <text>{{ item.ocrConfidence ? (item.ocrConfidence * 100).toFixed(2) + '%' : '无' }}</text>
      </view>
      <view class="card-row">
        <text class="label">证件照片</text>
        <image v-if="item.certImageUrl" :src="item.certImageUrl" mode="widthFix" class="cert-img" @click="preview(item.certImageUrl)" />
        <text v-else class="muted">无</text>
      </view>
      <view class="card-row">
        <text class="label">提交时间</text>
        <text>{{ item.createTime }}</text>
      </view>
      <view class="actions">
        <button class="reject-btn" @click="review(item.id, false)">驳回</button>
        <button class="pass-btn" @click="review(item.id, true)">通过</button>
      </view>
    </view>

    <view v-if="total > pageSize" style="text-align:center;padding:30rpx;color:#9aa5b8;">
      <text>共 {{ total }} 条</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getPendingVerifies, reviewVerify, type VerifyItem } from "@/api/admin";
import { toast } from "@/utils/toast";

const items = ref<VerifyItem[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 20;
const keyword = ref("");

onShow(() => { if (!checkAuth()) return; load(); });

function checkAuth() {
  if (!uni.getStorageSync("adminToken")) { uni.redirectTo({ url: "/pages/admin/login" }); return false; }
  return true;
}

async function load() {
  try {
    const result = await getPendingVerifies({ page: page.value, pageSize, keyword: keyword.value || undefined });
    items.value = result.items;
    total.value = result.total;
  } catch { /* ignore */ }
}

async function review(recordId: number, passed: boolean) {
  const msg = passed ? "确认通过？" : "确认驳回？";
  uni.showModal({
    title: "审核操作",
    content: passed ? msg : "请输入驳回原因",
    editable: !passed,
    placeholderText: "填写驳回原因",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await reviewVerify(recordId, passed, passed ? undefined : (res.content || "未填写原因"));
        toast(passed ? "审核通过" : "已驳回", "success");
        load();
      } catch { /* ignore */ }
    },
  });
}

function preview(url: string) {
  uni.previewImage({ urls: [url] });
}

function goBack() {
  uni.navigateBack();
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

.back {
  color: #1f4fd8;
  font-size: 28rpx;
  font-weight: 700;
}

.title {
  font-size: 32rpx;
  font-weight: 900;
  color: #152033;
}

.verify-card {
  margin-bottom: 20rpx;
  padding: 28rpx;
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14rpx 0;
  font-size: 26rpx;
  border-bottom: 1rpx solid #f5f7fb;
}

.card-row:last-of-type {
  border-bottom: 0;
}

.label {
  color: #7a869d;
  font-weight: 700;
  min-width: 140rpx;
}

.bold {
  font-weight: 900;
  color: #152033;
}

.cert-img {
  width: 200rpx;
  border-radius: 12rpx;
  border: 1rpx solid #eef2f8;
}

.actions {
  display: flex;
  gap: 20rpx;
  margin-top: 20rpx;
}

.reject-btn, .pass-btn {
  flex: 1;
  padding: 22rpx 0;
  border-radius: 20rpx;
  font-size: 28rpx;
  font-weight: 800;
  text-align: center;
}

.reject-btn {
  color: #dc2626;
  border: 2rpx solid #fca5a5;
  background: #fef2f2;
}

.pass-btn {
  color: #fff;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}
</style>
