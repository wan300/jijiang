<template>
  <view class="page-shell">
    <view class="surface-card card">
      <view class="avatar">{{ (user.userInfo?.nickname || "技").slice(0, 1) }}</view>
      <text class="name">{{ user.userInfo?.nickname || "技匠用户" }}</text>
      <text class="muted">{{ user.userInfo?.campusName || "未选择校区" }}</text>
    </view>
    <view class="surface-card card">
      <view class="row"><text>用户编号</text><text>#{{ user.userInfo?.id || "-" }}</text></view>
      <view class="row"><text>当前身份</text><text>{{ user.currentRole === 2 ? "讲师" : "买家" }}</text></view>
      <view class="row"><text>实名认证</text><text>{{ user.isVerified ? "已通过" : "未完成" }}</text></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { useUserStore } from "@/store/user";

const user = useUserStore();
</script>

<style scoped>
.card {
  margin-bottom: 24rpx;
  padding: 34rpx;
  text-align: center;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132rpx;
  height: 132rpx;
  margin: 0 auto 20rpx;
  border-radius: 44rpx;
  color: #fff;
  font-size: 50rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}

.name {
  display: block;
  color: #152033;
  font-size: 34rpx;
  font-weight: 900;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #edf2f8;
}

.row:last-child {
  border-bottom: 0;
}
</style>
