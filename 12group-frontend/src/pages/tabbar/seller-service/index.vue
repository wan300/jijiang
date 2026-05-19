<template>
  <view class="page-shell page-with-tab">
    <view class="hero-card slim">
      <text class="title">服务管理</text>
      <text class="desc">当前后端支持发布与审核，讲师服务列表接口待接入。</text>
      <button class="white-btn" @click="publish">发布新服务</button>
    </view>
    <ji-empty title="服务列表待接入" desc="文档中的 /api/service/my-list 尚未在当前后端实现，发布成功后可由后台审核上架。" action-text="去发布" @action="publish" />
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import JiEmpty from "@/components/ji-empty.vue";
import JiTabBar from "@/components/ji-tab-bar.vue";

function publish() {
  uni.navigateTo({ url: "/pages/service/publish" });
}
</script>

<style scoped>
.slim {
  padding: 34rpx;
}

.title,
.desc {
  display: block;
}

.title {
  font-size: 42rpx;
  font-weight: 900;
}

.desc {
  margin-top: 16rpx;
  opacity: 0.82;
  line-height: 1.6;
}

.white-btn {
  margin-top: 28rpx;
  border-radius: 999rpx;
  padding: 20rpx;
  color: #1f4fd8;
  font-weight: 900;
  background: #fff;
}
</style>
