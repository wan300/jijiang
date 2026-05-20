<template>
  <section class="content-panel">
    <div class="panel-head">
      <h2>订单监控</h2>
      <div class="toolbar">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 150px">
          <el-option v-for="item in orderStatuses" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="keyword" :prefix-icon="Search" clearable placeholder="订单号、买家、讲师、服务" @keyup.enter="search" />
        <el-button :icon="Search" type="primary" @click="search">查询</el-button>
      </div>
    </div>
    <el-table :data="pageData.items" v-loading="loading" height="620" @row-dblclick="openDetail">
      <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
      <el-table-column prop="serviceTitle" label="服务" min-width="200" show-overflow-tooltip />
      <el-table-column prop="buyerName" label="买家" width="120" show-overflow-tooltip />
      <el-table-column prop="sellerName" label="讲师" width="120" show-overflow-tooltip />
      <el-table-column label="金额" width="110">
        <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button :icon="View" size="small" @click="openDetail(row)">详情</el-button>
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

    <el-drawer v-model="drawerVisible" title="订单详情" size="720px">
      <div v-if="detail" class="detail-stack">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="orderStatusType(detail.order.status)">{{ orderStatusText(detail.order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="服务">{{ detail.order.serviceTitle || "-" }}</el-descriptions-item>
          <el-descriptions-item label="金额">{{ formatMoney(detail.order.amount) }}</el-descriptions-item>
          <el-descriptions-item label="买家">{{ detail.order.buyerName || detail.order.buyerId }}</el-descriptions-item>
          <el-descriptions-item label="讲师">{{ detail.order.sellerName || detail.order.sellerId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDate(detail.order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ formatDate(detail.order.payTime) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.order.remark || "-" }}</el-descriptions-item>
          <el-descriptions-item label="交付说明" :span="2">{{ detail.order.deliverText || "-" }}</el-descriptions-item>
        </el-descriptions>
        <el-tabs>
          <el-tab-pane label="支付记录">
            <el-table :data="detail.payments" height="220">
              <el-table-column prop="outTradeNo" label="商户单号" min-width="180" />
              <el-table-column prop="transactionId" label="交易号" min-width="160" />
              <el-table-column label="金额" width="100">
                <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="payChannel" label="渠道" width="90" />
              <el-table-column label="时间" width="180">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="状态日志">
            <el-table :data="detail.logs" height="220">
              <el-table-column label="流转" width="190">
                <template #default="{ row }">{{ orderStatusText(row.fromStatus) }} -> {{ orderStatusText(row.toStatus) }}</template>
              </el-table-column>
              <el-table-column prop="operatorId" label="操作人" width="100" />
              <el-table-column prop="remark" label="备注" min-width="180" />
              <el-table-column label="时间" width="180">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane label="站内信">
            <el-table :data="detail.messages" height="220">
              <el-table-column prop="senderId" label="发送人" width="100" />
              <el-table-column prop="receiverId" label="接收人" width="100" />
              <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
              <el-table-column label="时间" width="180">
                <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { Search, View } from "@element-plus/icons-vue";
import { getOrderDetail, getOrders } from "@/api/admin";
import type { OrderDetailResult, OrderRecord, PageResult } from "@/types/admin";
import { formatDate, formatMoney, orderStatusText, orderStatusType } from "@/utils/status";

const orderStatuses = [
  { value: 10, label: "待支付" },
  { value: 20, label: "待接单" },
  { value: 30, label: "进行中" },
  { value: 40, label: "待确认" },
  { value: 50, label: "已完成" },
  { value: 60, label: "已关闭" },
  { value: 80, label: "已退款" },
];
const loading = ref(false);
const keyword = ref("");
const status = ref<number>();
const query = reactive({ page: 1, pageSize: 20 });
const pageData = reactive<PageResult<OrderRecord>>({ items: [], total: 0, page: 1, pageSize: 20 });
const drawerVisible = ref(false);
const detail = ref<OrderDetailResult>();

async function load() {
  loading.value = true;
  try {
    const data = await getOrders({ ...query, status: status.value, keyword: keyword.value });
    Object.assign(pageData, data);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "订单列表加载失败");
  } finally {
    loading.value = false;
  }
}

function search() {
  query.page = 1;
  load();
}

async function openDetail(row: OrderRecord) {
  drawerVisible.value = true;
  try {
    detail.value = await getOrderDetail(row.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "订单详情加载失败");
  }
}

onMounted(load);
</script>
