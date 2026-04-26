<template>
  <view class="page-shell page-with-tab">
    <view class="surface-card discover-head">
      <text class="kicker">DISCOVER</text>
      <text class="title">按目标找到最合适的同学</text>
      <input v-model="keyword" class="field-input" placeholder="搜索服务、技能或讲师" confirm-type="search" @confirm="search" />
    </view>

    <view class="section-title"><text>热门方向</text></view>
    <view class="chips">
      <text v-for="item in config.categories" :key="item.id" class="chip" @click="openCategory(item.id)">{{ item.name }}</text>
    </view>

    <view class="section-title"><text>校园服务灵感</text></view>
    <view class="ideas">
      <view v-for="item in ideas" :key="item.title" class="idea surface-card">
        <text class="idea-title">{{ item.title }}</text>
        <text class="idea-desc">{{ item.desc }}</text>
      </view>
    </view>
    <ji-tab-bar />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import JiTabBar from "@/components/ji-tab-bar.vue";
import { useConfigStore } from "@/store/config";

const config = useConfigStore();
const keyword = ref("");
const ideas = [
  { title: "期末冲刺", desc: "高数、线代、专业课重点梳理" },
  { title: "作品集打磨", desc: "设计、摄影、视频剪辑协作" },
  { title: "求职加速", desc: "简历、面试、项目复盘" },
  { title: "兴趣技能", desc: "吉他、舞蹈、运动陪练" },
];

onShow(() => config.loadCategories());

function search() {
  uni.navigateTo({ url: `/pages/service/search?keyword=${encodeURIComponent(keyword.value)}` });
}

function openCategory(categoryId: number) {
  uni.navigateTo({ url: `/pages/service/search?categoryId=${categoryId}` });
}
</script>

<style scoped>
.discover-head {
  padding: 34rpx;
}

.kicker {
  color: #25a891;
  font-size: 22rpx;
  font-weight: 900;
  letter-spacing: 4rpx;
}

.title {
  display: block;
  margin: 18rpx 0 28rpx;
  color: #152033;
  font-size: 42rpx;
  font-weight: 900;
  line-height: 1.28;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}

.chip {
  border-radius: 999rpx;
  padding: 18rpx 26rpx;
  color: #1f4fd8;
  font-size: 25rpx;
  font-weight: 800;
  background: #e9f0ff;
}

.ideas {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
}

.idea {
  min-height: 190rpx;
  padding: 28rpx;
}

.idea-title {
  display: block;
  color: #152033;
  font-size: 30rpx;
  font-weight: 900;
}

.idea-desc {
  display: block;
  margin-top: 18rpx;
  color: #748198;
  font-size: 24rpx;
  line-height: 1.55;
}
</style>
