<template>
  <section class="content-panel">
    <div class="panel-head">
      <h2>实名待审核</h2>
      <div class="toolbar">
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="姓名、学号、昵称、学校" @keyup.enter="search" />
        <el-button :icon="Search" type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-table :data="pageData.items" v-loading="loading" height="620">
      <el-table-column prop="id" label="记录" width="88" />
      <el-table-column prop="nickname" label="用户" min-width="120" show-overflow-tooltip />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="campusName" label="学校" min-width="130" show-overflow-tooltip />
      <el-table-column label="OCR 置信度" width="130">
        <template #default="{ row }">{{ row.ocrConfidence ?? "-" }}</template>
      </el-table-column>
      <el-table-column label="证件图片" width="110">
        <template #default="{ row }">
          <el-link v-if="row.certImageUrl" type="primary" :href="row.certImageUrl" target="_blank">查看</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button :icon="Check" type="success" size="small" @click="openReview(row, true)">通过</el-button>
          <el-button :icon="Close" type="danger" size="small" plain @click="openReview(row, false)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-row">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="pageData.total"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="reviewPassed ? '通过实名审核' : '驳回实名审核'" width="460px">
      <el-input v-model="reason" type="textarea" :rows="4" placeholder="填写审核备注或驳回原因" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="reviewPassed ? 'success' : 'danger'" :loading="submitting" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Check, Close, Search } from "@element-plus/icons-vue";
import { getPendingVerifies, reviewVerify } from "@/api/admin";
import type { PageResult, VerifyRecord } from "@/types/admin";
import { formatDate } from "@/utils/status";

const loading = ref(false);
const submitting = ref(false);
const keyword = ref("");
const query = reactive({ page: 1, pageSize: 20 });
const pageData = reactive<PageResult<VerifyRecord>>({ items: [], total: 0, page: 1, pageSize: 20 });
const dialogVisible = ref(false);
const reviewPassed = ref(true);
const reason = ref("");
const current = ref<VerifyRecord>();

async function load() {
  loading.value = true;
  try {
    const data = await getPendingVerifies({ ...query, keyword: keyword.value });
    Object.assign(pageData, data);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "实名审核列表加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function openReview(row: VerifyRecord, passed: boolean) {
  current.value = row;
  reviewPassed.value = passed;
  reason.value = passed ? "材料真实，审核通过" : "";
  dialogVisible.value = true;
}

async function submitReview() {
  if (!current.value) return;
  submitting.value = true;
  try {
    await reviewVerify({ id: current.value.id, passed: reviewPassed.value, reason: reason.value });
    ElMessage.success("审核已提交");
    dialogVisible.value = false;
    load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "审核失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>
