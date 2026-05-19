<template>
  <view class="page-shell">
    <view class="hero-card">
      <text class="title">发布你的技能服务</text>
      <text class="desc">标题清晰、价格透明、描述具体，更容易获得同学信任。</text>
    </view>
    <view class="surface-card form">
      <view class="field">
        <view class="field-label">服务标题</view>
        <input v-model="form.title" class="field-input" placeholder="如 Python 数据分析 1v1 辅导" />
      </view>
      <view class="field">
        <view class="field-label">分类</view>
        <picker :range="categoryNames" @change="pickCategory">
          <view class="field-input">{{ selectedCategoryName }}</view>
        </picker>
      </view>
      <view class="field">
        <view class="field-label">价格</view>
        <input v-model="form.price" class="field-input" type="digit" placeholder="请输入单次价格" />
      </view>
      <view class="field">
        <view class="field-label">库存/可预约次数</view>
        <input v-model="form.stock" class="field-input" type="number" placeholder="默认 1" />
      </view>
      <view class="field">
        <view class="field-label">封面图片 URL</view>
        <input v-model="form.coverUrl" class="field-input" placeholder="可先填写网络图片地址或留空" />
      </view>
      <view class="field">
        <view class="field-label">服务描述</view>
        <textarea v-model="form.description" class="field-textarea" placeholder="说明适合人群、交付方式、可约时间和注意事项" />
      </view>
      <button class="primary-btn" :loading="loading" @click="submit">提交审核</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { publishService } from "@/api/service";
import { useConfigStore } from "@/store/config";
import { hitRiskService, hitSensitiveContact } from "@/utils/regex";
import { toast } from "@/utils/toast";

const config = useConfigStore();
const categoryIndex = ref(0);
const loading = ref(false);
const form = reactive({
  title: "",
  description: "",
  price: "",
  stock: "1",
  coverUrl: "",
});
const categoryNames = computed(() => config.categories.map((item) => item.name));
const selectedCategoryName = computed(() => categoryNames.value[categoryIndex.value] || "请选择分类");

onLoad(() => config.loadCategories());

function pickCategory(event: { detail: { value: number } }) {
  categoryIndex.value = Number(event.detail.value);
}

async function submit() {
  const text = `${form.title} ${form.description}`;
  if (!form.title || !form.description || !form.price) {
    toast("请填写标题、描述和价格");
    return;
  }
  if (hitSensitiveContact(text) || hitRiskService(text)) {
    toast("内容包含联系方式或高风险词");
    return;
  }
  const category = config.categories[categoryIndex.value];
  if (!category) {
    toast("请选择分类");
    return;
  }
  loading.value = true;
  try {
    const result = await publishService({
      categoryId: category.id,
      title: form.title,
      description: form.description,
      price: Number(form.price),
      priceConfig: JSON.stringify([{ key: "single", name: "单次体验", price: Number(form.price), unit: "次", qty: 1 }]),
      coverUrl: form.coverUrl,
      stock: Number(form.stock || 1),
    });
    toast(result.message || "已提交审核", "success");
    uni.navigateBack();
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
  line-height: 1.6;
}

.form {
  margin-top: 24rpx;
  padding: 34rpx;
}
</style>
