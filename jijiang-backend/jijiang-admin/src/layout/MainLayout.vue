<template>
  <el-container class="admin-shell">
    <el-aside width="232px" class="admin-aside">
      <div class="brand">
        <div class="brand-mark">匠</div>
        <div>
          <div class="brand-title">技匠后台</div>
          <div class="brand-subtitle">运营管理</div>
        </div>
      </div>
      <el-menu :default-active="route.path" router class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据看板</span>
        </el-menu-item>
        <el-menu-item index="/verify">
          <el-icon><UserFilled /></el-icon>
          <span>实名审核</span>
        </el-menu-item>
        <el-menu-item index="/services">
          <el-icon><Collection /></el-icon>
          <span>服务管理</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>订单监控</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="admin-header">
        <div>
          <div class="page-title">{{ title }}</div>
          <div class="page-subtitle">管理审核、服务与交易履约</div>
        </div>
        <div class="admin-user">
          <el-icon><Avatar /></el-icon>
          <span>{{ auth.adminInfo?.displayName || "管理员" }}</span>
          <el-button :icon="SwitchButton" plain @click="logout">退出</el-button>
        </div>
      </el-header>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import { Avatar, Collection, DataAnalysis, SwitchButton, Tickets, UserFilled } from "@element-plus/icons-vue";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const title = computed(() => String(route.meta.title || "管理后台"));

function logout() {
  auth.logout();
  router.replace("/login");
}
</script>
