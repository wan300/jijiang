import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/store/auth";
import LoginView from "@/views/LoginView.vue";
import MainLayout from "@/layout/MainLayout.vue";
import DashboardView from "@/views/DashboardView.vue";
import VerifyView from "@/views/VerifyView.vue";
import ServiceView from "@/views/ServiceView.vue";
import OrderView from "@/views/OrderView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/login", component: LoginView, meta: { public: true } },
    {
      path: "/",
      component: MainLayout,
      redirect: "/dashboard",
      children: [
        { path: "dashboard", component: DashboardView, meta: { title: "数据看板" } },
        { path: "verify", component: VerifyView, meta: { title: "实名审核" } },
        { path: "services", component: ServiceView, meta: { title: "服务管理" } },
        { path: "orders", component: OrderView, meta: { title: "订单监控" } },
      ],
    },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isLogin) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }
  if (to.path === "/login" && auth.isLogin) {
    return "/dashboard";
  }
});

export default router;
