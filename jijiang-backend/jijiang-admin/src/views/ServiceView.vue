<template>
  <section class="content-panel">
    <div class="panel-head">
      <h2>服务管理</h2>
      <div class="toolbar">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="待审核" :value="0" />
          <el-option label="已上架" :value="1" />
          <el-option label="已驳回/下架" :value="2" />
        </el-select>
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="服务、讲师、分类" @keyup.enter="search" />
        <el-button :icon="Search" type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-table :data="pageData.items" v-loading="loading" height="620">
      <el-table-column prop="id" label="服务" width="88" />
      <el-table-column label="封面" width="84">
        <template #default="{ row }">
          <el-image v-if="row.coverUrl" :src="row.coverUrl" fit="cover" class="cover-thumb" />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="210" show-overflow-tooltip />
      <el-table-column prop="sellerName" label="讲师" width="120" show-overflow-tooltip />
      <el-table-column prop="categoryName" label="分类" width="110" />
      <el-table-column label="价格" width="100">
        <template #default="{ row }">{{ formatMoney(row.price) }}</template>
      </el-table-column>
      <el-table-column label="库存" width="100">
        <template #default="{ row }">{{ row.usedStock }}/{{ row.stock }}</template>
      </el-table-column>
      <el-table-column label="状态" width="130">
        <template #default="{ row }">
          <el-tag :type="serviceStatusType(row.status)">{{ serviceStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" :icon="Check" type="success" size="small" @click="openAction(row, 'approve')">通过</el-button>
          <el-button v-if="row.status === 0" :icon="Close" type="danger" plain size="small" @click="openAction(row, 'reject')">驳回</el-button>
          <el-button v-if="row.status === 1" :icon="CircleClose" type="warning" plain size="small" @click="openAction(row, 'offline')">下架</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-input v-model="reason" type="textarea" :rows="4" placeholder="填写审核备注、驳回或下架原因" />
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button :type="dialogType" :loading="submitting" @click="submitAction">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Check, CircleClose, Close, Search } from "@element-plus/icons-vue";
import { getServices, offlineService, reviewService } from "@/api/admin";
import type { PageResult, ServiceRecord } from "@/types/admin";
import { formatDate, formatMoney, serviceStatusText, serviceStatusType } from "@/utils/status";

type ActionType = "approve" | "reject" | "offline";

const loading = ref(false);
const submitting = ref(false);
const keyword = ref("");
const status = ref<number>();
const query = reactive({ page: 1, pageSize: 20 });
const pageData = reactive<PageResult<ServiceRecord>>({ items: [], total: 0, page: 1, pageSize: 20 });
const dialogVisible = ref(false);
const actionType = ref<ActionType>("approve");
const current = ref<ServiceRecord>();
const reason = ref("");
const dialogTitle = computed(() => ({ approve: "通过服务审核", reject: "驳回服务", offline: "下架服务" })[actionType.value]);
const dialogType = computed(() => (actionType.value === "approve" ? "success" : actionType.value === "reject" ? "danger" : "warning"));

async function load() {
  loading.value = true;
  try {
    const data = await getServices({ ...query, status: status.value, keyword: keyword.value });
    Object.assign(pageData, data);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "服务列表加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function openAction(row: ServiceRecord, type: ActionType) {
  current.value = row;
  actionType.value = type;
  reason.value = type === "approve" ? "内容合规，审核通过" : "";
  dialogVisible.value = true;
}

async function submitAction() {
  if (!current.value) return;
  submitting.value = true;
  try {
    if (actionType.value === "offline") {
      await offlineService({ id: current.value.id, reason: reason.value });
    } else {
      await reviewService({ id: current.value.id, passed: actionType.value === "approve", reason: reason.value });
    }
    ElMessage.success("操作已完成");
    dialogVisible.value = false;
    load();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "服务操作失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>
