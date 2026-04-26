<template>
  <view class="page-shell">
    <view class="searchbar surface-card">
      <input v-model="keyword" placeholder="搜索技能服务" confirm-type="search" @confirm="load" />
      <button @click="load">搜索</button>
    </view>
    <scroll-view class="category-scroll" scroll-x>
      <text class="chip" :class="{ active: !categoryId }" @click="pickCategory(undefined)">全部</text>
      <text
        v-for="item in config.categories"
        :key="item.id"
        class="chip"
        :class="{ active: categoryId === item.id }"
        @click="pickCategory(item.id)"
      >
        {{ item.name }}
      </text>
    </scroll-view>

    <ji-empty v-if="!loading && list.length === 0" title="没有找到服务" desc="换个关键词或分类试试。" />
    <ji-service-card v-for="item in list" :key="item.id" :service="item" @click="open" />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import JiEmpty from "@/components/ji-empty.vue";
import JiServiceCard from "@/components/ji-service-card.vue";
import { searchServices } from "@/api/service";
import { useConfigStore } from "@/store/config";
import { useUserStore } from "@/store/user";
import type { ServiceItem } from "@/types/domain";

const user = useUserStore();
const config = useConfigStore();
const keyword = ref("");
const categoryId = ref<number | undefined>();
const list = ref<ServiceItem[]>([]);
const loading = ref(false);

onLoad(async (query) => {
  keyword.value = String(query?.keyword || "");
  categoryId.value = query?.categoryId ? Number(query.categoryId) : undefined;
  await config.loadCategories();
  await load();
});

async function load() {
  loading.value = true;
  try {
    list.value = await searchServices({ campusId: user.campusId, keyword: keyword.value, categoryId: categoryId.value });
  } finally {
    loading.value = false;
  }
}

function pickCategory(id?: number) {
  categoryId.value = id;
  load();
}

function open(service: ServiceItem) {
  uni.navigateTo({ url: `/pages/service/detail?id=${service.id}` });
}
</script>

<style scoped>
.searchbar {
  display: flex;
  align-items: center;
  height: 96rpx;
  padding: 0 18rpx 0 30rpx;
}

.searchbar input {
  flex: 1;
}

.searchbar button {
  width: 136rpx;
  height: 68rpx;
  border-radius: 999rpx;
  color: #fff;
  font-size: 26rpx;
  background: #1f4fd8;
}

.category-scroll {
  white-space: nowrap;
  margin: 24rpx 0;
}

.chip {
  display: inline-flex;
  margin-right: 14rpx;
  border-radius: 999rpx;
  padding: 16rpx 24rpx;
  color: #6e7a91;
  font-size: 24rpx;
  background: #fff;
}

.chip.active {
  color: #fff;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}
</style>
