<template>
  <view class="dev-switcher surface-card">
    <view class="dev-switcher-title">DEV 账号切换</view>
    <view class="dev-row">
      <text class="dev-label">当前：{{ nickname }}</text>
      <text class="dev-uid">ID: {{ userId }}</text>
    </view>
    <view class="dev-actions">
      <button class="dev-btn" @click="save">保存当前</button>
      <button class="dev-btn ghost" @click="switchTo('test-buyer')">切买家</button>
      <button class="dev-btn ghost" @click="switchTo('test-seller')">切卖家</button>
    </view>
    <view v-if="saved.length" class="dev-saved">
      <view v-for="(item, i) in saved" :key="i" class="dev-saved-row" @click="restore(i)">
        <text>{{ item.code }}</text>
        <text class="dev-uid">ID: {{ item.userId }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();

const nickname = computed(() => user.userInfo?.nickname || "未登录");
const userId = computed(() => user.userInfo?.id || 0);

interface SavedAccount {
  code: string;
  nickname: string;
  userId: number;
  token: string;
  refreshToken: string;
}

const saved = ref<SavedAccount[]>([]);

try {
  const raw = uni.getStorageSync("dev_saved_accounts");
  if (raw) saved.value = JSON.parse(raw);
} catch { /* empty */ }

function save() {
  if (!user.isLogin) return toast("请先登录");
  const code = prompt("给这个账号起个 code 名：")?.trim();
  if (!code) return;
  const exists = saved.value.findIndex((a) => a.code === code);
  const item: SavedAccount = {
    code,
    nickname: user.userInfo?.nickname || "",
    userId: user.userInfo?.id || 0,
    token: user.token,
    refreshToken: user.refreshTokenValue,
  };
  if (exists >= 0) saved.value[exists] = item;
  else saved.value.push(item);
  uni.setStorageSync("dev_saved_accounts", JSON.stringify(saved.value));
  toast(`已保存：${code}`);
}

function restore(index: number) {
  const item = saved.value[index];
  if (!item) return;
  user.applyLogin(item.token, item.refreshToken, {
    ...user.userInfo!,
    id: item.userId,
    nickname: item.nickname,
  });
  toast(`已切换：${item.code}`);
}

async function switchTo(code: string) {
  try {
    await user.login(code);
    toast(`已登录：${code}`);
  } catch { /* error handled by interceptor */ }
}
</script>

<style scoped>
.dev-switcher {
  margin-top: 24rpx;
  padding: 24rpx 28rpx;
  background: #f0f4ff;
  border: 1rpx dashed #7b9eff;
}

.dev-switcher-title {
  font-size: 22rpx;
  color: #586c94;
  margin-bottom: 10rpx;
}

.dev-row {
  display: flex;
  justify-content: space-between;
  font-size: 24rpx;
  color: #2c3e50;
}

.dev-uid {
  color: #8899b4;
}

.dev-actions {
  display: flex;
  gap: 12rpx;
  margin-top: 14rpx;
}

.dev-btn {
  flex: 1;
  border-radius: 8rpx;
  padding: 10rpx 0;
  font-size: 22rpx;
  background: #1f4fd8;
  color: #fff;
}

.dev-btn.ghost {
  background: #fff;
  color: #1f4fd8;
  border: 1rpx solid #1f4fd8;
}

.dev-saved {
  margin-top: 14rpx;
  border-top: 1rpx solid #dce1ea;
  padding-top: 10rpx;
}

.dev-saved-row {
  display: flex;
  justify-content: space-between;
  padding: 8rpx 0;
  font-size: 22rpx;
}
</style>
