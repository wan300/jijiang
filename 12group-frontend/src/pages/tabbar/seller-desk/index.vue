<template>
  <view class="page-shell page-with-tab">
    <view class="hero-card">
      <text class="hello">讲师工作台</text>
      <text class="headline">{{ user.userInfo?.nickname || "同学" }}，今天也把技能变成价值</text>
      <view class="quick">
        <button @click="publish">发布服务</button>
        <button @click="orders">处理订单</button>
      </view>
    </view>

    <view class="stats">
      <view class="surface-card stat"><text class="num">{{ sellerOrders.length }}</text><text>卖家订单</text></view>
      <view class="surface-card stat"><text class="num">{{ waiting }}</text><text>待处理</text></view>
      <view class="surface-card stat"><text class="num">{{ user.userInfo?.creditScore || 100 }}</text><text>信誉分</text></view>
    </view>

    <view class="surface-card checklist">
      <text class="title">开通检查</text>
      <view class="row"><text>实名认证</text><text>{{ user.isVerified ? "已完成" : "待完成" }}</text></view>
      <view class="row"><text>讲师资格</text><text>{{ user.isSellerVerified ? "已通过" : "待审核" }}</text></view>
      <view class="row"><text>保证金</text><text>{{ user.hasDeposit ? "已缴纳" : "后端待接入" }}</text></view>
    </view>
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import JiTabBar from "@/components/ji-tab-bar.vue";
import { listOrders } from "@/api/order";
import { useUserStore } from "@/store/user";
import type { OrderItem } from "@/types/domain";

const user = useUserStore();
const sellerOrders = ref<OrderItem[]>([]);
const waiting = computed(() => sellerOrders.value.filter((item) => item.status === 20 || item.status === 30).length);

onShow(async () => {
  if (user.isLogin) sellerOrders.value = await listOrders("seller");
});

function publish() {
  uni.navigateTo({ url: "/pages/service/publish" });
}

function orders() {
  uni.reLaunch({ url: "/pages/tabbar/seller-order/index" });
}
</script>

<style scoped>
.hello {
  opacity: 0.8;
  font-size: 24rpx;
}

.headline {
  display: block;
  margin-top: 30rpx;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.35;
}

.quick {
  display: flex;
  gap: 18rpx;
  margin-top: 36rpx;
}

.quick button {
  flex: 1;
  height: 76rpx;
  border-radius: 999rpx;
  color: #1f4fd8;
  font-weight: 900;
  background: #fff;
}

.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
  margin-top: 24rpx;
}

.stat {
  padding: 28rpx 10rpx;
  text-align: center;
  color: #7a869d;
  font-size: 22rpx;
}

.num {
  display: block;
  color: #152033;
  font-size: 36rpx;
  font-weight: 900;
}

.checklist {
  margin-top: 24rpx;
  padding: 30rpx;
}

.title {
  display: block;
  margin-bottom: 14rpx;
  color: #152033;
  font-size: 32rpx;
  font-weight: 900;
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #edf2f8;
}
</style>
