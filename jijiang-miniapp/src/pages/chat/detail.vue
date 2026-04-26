<template>
  <view class="page-shell chat">
    <view class="tips">平台内沟通会留存为仲裁凭证，请勿交换微信、电话等联系方式。</view>
    <scroll-view class="messages" scroll-y>
      <view
        v-for="item in messages"
        :key="item.id"
        class="bubble"
        :class="{ mine: item.senderId === user.userInfo?.id }"
      >
        <text>{{ item.content }}</text>
      </view>
      <ji-empty v-if="messages.length === 0" title="还没有消息" desc="主动说明时间和学习目标吧。" />
    </scroll-view>
    <view class="inputbar">
      <input v-model="content" placeholder="输入站内信" />
      <button @click="send">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import JiEmpty from "@/components/ji-empty.vue";
import { listMessages, sendMessage } from "@/api/message";
import { useUserStore } from "@/store/user";
import type { MessageItem } from "@/types/domain";
import { hitSensitiveContact } from "@/utils/regex";
import { toast } from "@/utils/toast";

const user = useUserStore();
const orderId = ref(0);
const messages = ref<MessageItem[]>([]);
const content = ref("");

onLoad(async (query) => {
  orderId.value = Number(query?.orderId || 0);
  await refresh();
});

async function refresh() {
  if (orderId.value) messages.value = await listMessages(orderId.value);
}

async function send() {
  const text = content.value.trim();
  if (!text) return;
  if (hitSensitiveContact(text)) {
    toast("请勿交换联系方式");
    return;
  }
  await sendMessage(orderId.value, text);
  content.value = "";
  await refresh();
}
</script>

<style scoped>
.chat {
  padding-bottom: 130rpx;
}

.tips {
  margin-bottom: 20rpx;
  border-radius: 24rpx;
  padding: 20rpx;
  color: #946200;
  font-size: 23rpx;
  background: #fff4dc;
}

.messages {
  height: calc(100vh - 260rpx);
}

.bubble {
  max-width: 78%;
  margin: 16rpx 0;
  border-radius: 28rpx 28rpx 28rpx 8rpx;
  padding: 22rpx 26rpx;
  color: #253149;
  background: #fff;
}

.bubble.mine {
  margin-left: auto;
  border-radius: 28rpx 28rpx 8rpx 28rpx;
  color: #fff;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
}

.inputbar {
  position: fixed;
  right: 24rpx;
  bottom: 24rpx;
  left: 24rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  border-radius: 34rpx;
  padding: 16rpx;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 16rpx 40rpx rgba(28, 43, 76, 0.16);
}

.inputbar input {
  flex: 1;
  height: 72rpx;
  padding: 0 20rpx;
}

.inputbar button {
  width: 128rpx;
  height: 72rpx;
  border-radius: 999rpx;
  color: #fff;
  background: #1f4fd8;
}
</style>
