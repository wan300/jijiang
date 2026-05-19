<template>
  <view class="page-shell detail">
    <view class="cover" :style="{ backgroundImage: coverBg }">
      <text v-if="!service?.coverUrl">技匠服务</text>
    </view>
    <view v-if="service" class="surface-card panel">
      <view class="title-row">
        <text class="title">{{ service.title }}</text>
        <text class="price">¥{{ money(service.price) }}</text>
      </view>
      <view class="seller">
        <view class="avatar">{{ (service.sellerName || "讲").slice(0, 1) }}</view>
        <view>
          <text class="seller-name">{{ service.sellerName || `讲师 #${service.sellerId}` }}</text>
          <text class="muted">评分 {{ Number(service.scoreAvg || 5).toFixed(1) }} · 成交 {{ service.salesCount || 0 }}</text>
        </view>
      </view>
      <view class="section-title"><text>服务说明</text></view>
      <text class="desc">{{ service.description }}</text>
      <view class="notice">支付前请勿交换联系方式，平台担保交易更安全。</view>
    </view>

    <view class="bottom-bar">
      <button class="ghost-btn" @click="chat">咨询</button>
      <button class="primary-btn" @click="buy">立即下单</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getServiceDetail } from "@/api/service";
import { useOrderStore } from "@/store/order";
import { useUserStore } from "@/store/user";
import type { ServiceItem } from "@/types/domain";
import { money } from "@/utils/money";
import { toast } from "@/utils/toast";

const user = useUserStore();
const orderStore = useOrderStore();
const service = ref<ServiceItem | null>(null);
const coverBg = computed(() => (service.value?.coverUrl ? `url(${service.value.coverUrl})` : ""));

onLoad(async (query) => {
  const id = Number(query?.id);
  if (id) service.value = await getServiceDetail(id);
});

function chat() {
  toast("请先下单后在订单内沟通");
}

function buy() {
  if (!service.value) return;
  if (!user.isLogin) {
    uni.navigateTo({ url: `/pages/login/index?redirect=${encodeURIComponent(`/pages/service/detail?id=${service.value.id}`)}` });
    return;
  }
  orderStore.setService(service.value);
  uni.navigateTo({ url: `/pages/order/create?serviceId=${service.value.id}` });
}
</script>

<style scoped>
.detail {
  padding-bottom: 150rpx;
}

.cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 430rpx;
  border-radius: 40rpx;
  color: #fff;
  font-size: 48rpx;
  font-weight: 900;
  background: linear-gradient(135deg, #101c4c, #1f4fd8, #25c5a9);
  background-size: cover;
  background-position: center;
}

.panel {
  margin-top: -42rpx;
  padding: 34rpx;
}

.title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
}

.title {
  flex: 1;
  color: #152033;
  font-size: 38rpx;
  font-weight: 900;
  line-height: 1.35;
}

.price {
  color: #ff7a45;
  font-size: 42rpx;
  font-weight: 900;
}

.seller {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-top: 28rpx;
  padding: 22rpx;
  border-radius: 28rpx;
  background: #f4f7fc;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 82rpx;
  height: 82rpx;
  border-radius: 28rpx;
  color: #fff;
  font-weight: 900;
  background: linear-gradient(135deg, #25c5a9, #1f4fd8);
}

.seller-name,
.desc {
  display: block;
}

.seller-name {
  color: #152033;
  font-size: 28rpx;
  font-weight: 800;
}

.desc {
  color: #3a4658;
  font-size: 28rpx;
  line-height: 1.8;
}

.notice {
  margin-top: 28rpx;
  border-radius: 24rpx;
  padding: 22rpx;
  color: #946200;
  font-size: 24rpx;
  background: #fff4dc;
}

.bottom-bar {
  position: fixed;
  right: 24rpx;
  bottom: 24rpx;
  left: 24rpx;
  display: grid;
  grid-template-columns: 220rpx 1fr;
  gap: 18rpx;
}
</style>
