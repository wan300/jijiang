<template>
  <div class="page-stack">
    <el-row :gutter="16">
      <el-col v-for="item in metricItems" :key="item.key" :xs="24" :sm="12" :lg="6">
        <section class="metric-card">
          <div class="metric-icon" :class="item.tone">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <div class="metric-label">{{ item.label }}</div>
            <div class="metric-value">{{ item.value }}</div>
          </div>
        </section>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <section class="content-panel">
          <div class="panel-head">
            <h2>订单状态</h2>
            <el-button :icon="Refresh" text @click="load">刷新</el-button>
          </div>
          <el-table :data="overview?.orderStatus || []" v-loading="loading" height="320">
            <el-table-column label="状态">
              <template #default="{ row }">
                <el-tag :type="orderStatusType(row.status)">{{ orderStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="total" label="数量" width="160" />
          </el-table>
        </section>
      </el-col>
      <el-col :xs="24" :lg="12">
        <section class="content-panel">
          <div class="panel-head">
            <h2>TOP 服务</h2>
          </div>
          <el-table :data="overview?.topServices || []" v-loading="loading" height="320">
            <el-table-column prop="title" label="服务" min-width="180" show-overflow-tooltip />
            <el-table-column prop="sellerName" label="讲师" width="120" show-overflow-tooltip />
            <el-table-column prop="salesCount" label="销量" width="90" />
            <el-table-column prop="scoreAvg" label="评分" width="90" />
          </el-table>
        </section>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { Check, Coin, Goods, Refresh, Tickets, User } from "@element-plus/icons-vue";
import { getDashboardOverview } from "@/api/admin";
import type { DashboardOverview } from "@/types/admin";
import { formatMoney, orderStatusText, orderStatusType } from "@/utils/status";

const loading = ref(false);
const overview = ref<DashboardOverview>();
const metricItems = computed(() => {
  const metrics = overview.value?.metrics || {};
  return [
    { key: "userTotal", label: "用户总数", value: metrics.userTotal || 0, icon: User, tone: "teal" },
    { key: "verifiedUserTotal", label: "已实名用户", value: metrics.verifiedUserTotal || 0, icon: Check, tone: "green" },
    { key: "pendingServiceTotal", label: "待审服务", value: metrics.pendingServiceTotal || 0, icon: Goods, tone: "amber" },
    { key: "orderTotal", label: "订单总数", value: metrics.orderTotal || 0, icon: Tickets, tone: "blue" },
    { key: "pendingVerifyTotal", label: "待审实名", value: metrics.pendingVerifyTotal || 0, icon: User, tone: "amber" },
    { key: "onlineServiceTotal", label: "上架服务", value: metrics.onlineServiceTotal || 0, icon: Goods, tone: "green" },
    { key: "paidGmv", label: "支付 GMV", value: formatMoney(metrics.paidGmv), icon: Coin, tone: "teal" },
    { key: "todayOrderTotal", label: "今日订单", value: metrics.todayOrderTotal || 0, icon: Tickets, tone: "blue" },
  ];
});

async function load() {
  loading.value = true;
  try {
    overview.value = await getDashboardOverview();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : "看板加载失败");
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>
