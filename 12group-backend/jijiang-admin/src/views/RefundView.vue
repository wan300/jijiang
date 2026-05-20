<template>
  <section class="content-panel">
    <div class="panel-head">
      <h2>退款审核</h2>
      <div class="toolbar">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="待审核" :value="0" />
          <el-option label="已退款" :value="1" />
          <el-option label="已驳回" :value="2" />
        </el-select>
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="原因、买家、卖家" @keyup.enter="search" />
        <el-button :icon="Search" type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-table :data="pageData.items" v-loading="loading" height="620" @row-dblclick="openDetail">
      <el-table-column prop="orderId" label="订单ID" width="80" />
      <el-table-column prop="buyerName" label="买家" width="120" show-overflow-tooltip />
      <el-table-column prop="sellerName" label="卖家" width="120" show-overflow-tooltip />
      <el-table-column label="金额" width="100">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="refundStatusType(row.status)">{{ refundStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" width="170">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 0" :icon="Check" size="small" type="success" @click="approve(row)">通过</el-button>
          <el-button v-if="row.status === 0" :icon="Close" size="small" type="danger" @click="reject(row)">驳回</el-button>
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

    <el-dialog v-model="dialogVisible" title="退款审核" width="520px">
      <el-form label-width="90px">
        <el-form-item label="退款ID">{{ form.refundId }}</el-form-item>
        <el-form-item label="操作">
          <el-radio-group v-model="form.passed">
            <el-radio :value="true">通过</el-radio>
            <el-radio :value="false">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="驳回时必填" />
        </el-form-item>
        <el-form-item label="扣保证金">
          <el-input-number v-model="form.deductDeposit" :min="0" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Check, Close, Search } from "@element-plus/icons-vue";
import { getRefunds, reviewRefund } from "@/api/admin";
import type { PageResult, RefundRecord } from "@/types/admin";
import { formatDate, formatMoney, refundStatusText, refundStatusType } from "@/utils/status";

const loading = ref(false);
const keyword = ref("");
const status = ref<number>();
const query = reactive({ page: 1, pageSize: 20 });
const pageData = reactive<PageResult<RefundRecord>>({ items: [], total: 0, page: 1, pageSize: 20 });

const dialogVisible = ref(false);
const submitting = ref(false);
const form = reactive({ refundId: 0, passed: true, reason: "", deductDeposit: 0 });

async function load() {
  loading.value = true;
  try {
    const data = await getRefunds({ ...query, status: status.value, keyword: keyword.value });
    Object.assign(pageData, data);
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

function approve(row: RefundRecord) {
  form.refundId = row.id;
  form.passed = true;
  form.reason = "管理员审核通过";
  form.deductDeposit = 0;
  dialogVisible.value = true;
}

function reject(row: RefundRecord) {
  form.refundId = row.id;
  form.passed = false;
  form.reason = "";
  form.deductDeposit = 0;
  dialogVisible.value = true;
}

async function submit() {
  if (!form.passed && !form.reason.trim()) {
    ElMessage.warning("驳回必须填写原因");
    return;
  }
  submitting.value = true;
  try {
    await reviewRefund({
      refundId: form.refundId,
      passed: form.passed,
      reason: form.reason || undefined,
      deductDeposit: form.deductDeposit > 0 ? form.deductDeposit : undefined,
    });
    ElMessage.success(form.passed ? "退款已通过" : "退款已驳回");
    dialogVisible.value = false;
    load();
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : "操作失败");
  } finally {
    submitting.value = false;
  }
}

onMounted(load);
</script>
