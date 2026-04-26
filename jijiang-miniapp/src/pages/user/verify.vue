<template>
  <view class="page-shell">
    <view class="hero-card">
      <text class="title">学生身份认证</text>
      <text class="desc">认证通过后可下单、发布服务，并建立校园信誉档案。</text>
    </view>
    <view class="surface-card form">
      <view class="field">
        <view class="field-label">学校/校区</view>
        <picker :range="campusNames" @change="pickCampus">
          <view class="field-input">{{ campusNames[campusIndex] }}</view>
        </picker>
      </view>
      <view class="field">
        <view class="field-label">真实姓名</view>
        <input v-model="realName" class="field-input" placeholder="请输入姓名" />
      </view>
      <view class="field">
        <view class="field-label">学号</view>
        <input v-model="studentNo" class="field-input" placeholder="请输入学号" />
      </view>
      <view class="field">
        <view class="field-label">学生证/校园卡</view>
        <view class="upload" @click="chooseImage">{{ certImageUrl ? "已选择认证图片" : "点击选择图片" }}</view>
      </view>
      <button class="primary-btn" :loading="loading" @click="submit">提交认证</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { submitVerify } from "@/api/user";
import { useUserStore } from "@/store/user";
import { toast } from "@/utils/toast";

const user = useUserStore();
const campusNames = ["XX大学", "主校区", "东校区"];
const campusIds = [1, 1, 1];
const campusIndex = ref(0);
const realName = ref("");
const studentNo = ref("");
const certImageUrl = ref("");
const loading = ref(false);

function pickCampus(event: { detail: { value: number } }) {
  campusIndex.value = Number(event.detail.value);
}

async function chooseImage() {
  const res = await uni.chooseImage({ count: 1, sizeType: ["compressed"] });
  certImageUrl.value = res.tempFilePaths?.[0] || "mock://student-card.jpg";
}

async function submit() {
  if (!realName.value || !studentNo.value) {
    toast("请补充姓名和学号");
    return;
  }
  loading.value = true;
  try {
    const result = await submitVerify({
      campusId: campusIds[campusIndex.value],
      certType: 1,
      certImageUrl: certImageUrl.value || "mock://student-card.jpg",
      realName: realName.value,
      studentNo: studentNo.value,
    });
    toast(result.message || "提交成功", "success");
    await user.refreshUserInfo().catch(() => undefined);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.title,
.desc {
  display: block;
}

.title {
  font-size: 42rpx;
  font-weight: 900;
}

.desc {
  width: 560rpx;
  margin-top: 18rpx;
  opacity: 0.82;
  line-height: 1.65;
}

.form {
  margin-top: 24rpx;
  padding: 34rpx;
}

.upload {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 180rpx;
  border: 2rpx dashed #b8c5dc;
  border-radius: 28rpx;
  color: #1f4fd8;
  font-weight: 800;
  background: #f4f8ff;
}
</style>
