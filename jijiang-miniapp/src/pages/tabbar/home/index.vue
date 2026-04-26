<template>
  <view class="page-shell page-with-tab">
    <view class="hero-card">
      <view class="topline">
        <text>{{ user.userInfo?.campusName || "默认校区" }}</text>
        <button class="role" @click="toggleRole">切换{{ user.currentRole === 1 ? "讲师" : "买家" }}</button>
      </view>
      <text class="headline">找到身边同学的拿手技能</text>
      <view class="search" @click="goSearch">
        <text class="search-icon">搜索</text>
        <text class="muted-text">Python、考研、摄影、简历优化...</text>
      </view>
    </view>

    <view class="section-title">
      <text>技能分类</text>
      <text class="link" @click="goSearch">全部</text>
    </view>
    <view class="category-grid">
      <view v-for="item in categories" :key="item.id" class="category surface-card" @click="goCategory(item.id)">
        <text class="cat-icon">{{ item.name.slice(0, 1) }}</text>
        <text>{{ item.name }}</text>
      </view>
    </view>

    <view class="section-title">
      <text>今日推荐</text>
      <text class="link" @click="load">刷新</text>
    </view>
    <ji-empty v-if="!loading && services.length === 0" title="还没有上架服务" desc="完成讲师审核后发布第一项技能服务。" />
    <ji-service-card v-for="item in services" :key="item.id" :service="item" @click="openDetail" />
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import JiEmpty from "@/components/ji-empty.vue";
import JiServiceCard from "@/components/ji-service-card.vue";
import JiTabBar from "@/components/ji-tab-bar.vue";
import { searchServices } from "@/api/service";
import { useConfigStore } from "@/store/config";
import { useUserStore } from "@/store/user";
import type { ServiceItem } from "@/types/domain";

const user = useUserStore();
const config = useConfigStore();
const services = ref<ServiceItem[]>([]);
const loading = ref(false);
const categories = computed(() => config.categories.slice(0, 8));

onShow(load);

async function load() {
  loading.value = true;
  try {
    await config.loadCategories();
    services.value = await searchServices({ campusId: user.campusId });
  } finally {
    loading.value = false;
  }
}

function goSearch() {
  uni.navigateTo({ url: "/pages/service/search" });
}

function goCategory(categoryId: number) {
  uni.navigateTo({ url: `/pages/service/search?categoryId=${categoryId}` });
}

function openDetail(service: ServiceItem) {
  uni.navigateTo({ url: `/pages/service/detail?id=${service.id}` });
}

async function toggleRole() {
  if (user.currentRole === 2) {
    user.setLocalRole(1);
    return;
  }
  if (!user.isLogin) {
    uni.navigateTo({ url: "/pages/login/index" });
    return;
  }
  user.setLocalRole(2);
  uni.reLaunch({ url: "/pages/tabbar/seller-desk/index" });
}
</script>

<style scoped>
.topline,
.search {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.role {
  border-radius: 999rpx;
  padding: 12rpx 22rpx;
  color: #fff;
  font-size: 22rpx;
  background: rgba(255, 255, 255, 0.18);
}

.headline {
  display: block;
  width: 520rpx;
  margin-top: 54rpx;
  font-size: 46rpx;
  font-weight: 900;
  line-height: 1.25;
}

.search {
  height: 88rpx;
  margin-top: 40rpx;
  border-radius: 999rpx;
  padding: 0 28rpx;
  background: rgba(255, 255, 255, 0.92);
}

.search-icon {
  color: #1f4fd8;
  font-weight: 900;
}

.muted-text {
  flex: 1;
  margin-left: 18rpx;
  color: #8a95aa;
  font-size: 24rpx;
}

.link {
  color: #1f4fd8;
  font-size: 24rpx;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
}

.category {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  height: 150rpx;
  gap: 12rpx;
  color: #26344d;
  font-size: 23rpx;
  font-weight: 700;
}

.cat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56rpx;
  height: 56rpx;
  border-radius: 20rpx;
  color: #fff;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}
</style>
