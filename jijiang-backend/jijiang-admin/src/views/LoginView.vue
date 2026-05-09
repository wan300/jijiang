<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-copy">
        <div class="brand-row">
          <div class="brand-mark">匠</div>
          <div>
            <h1>技匠管理后台</h1>
            <p>运营审核、服务管理与订单监控</p>
          </div>
        </div>
        <div class="login-note">
          <span>默认开发账号</span>
          <strong>admin / Admin@123456</strong>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" label-position="top" @submit.prevent>
        <el-form-item label="管理员账号" prop="username">
          <el-input v-model.trim="form.username" :prefix-icon="User" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            :prefix-icon="Lock"
            autocomplete="current-password"
            show-password
            type="password"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-button :icon="Right" type="primary" class="login-button" :loading="loading" @click="submit">登录</el-button>
      </el-form>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, type FormInstance, type FormRules } from "element-plus";
import { Lock, Right, User } from "@element-plus/icons-vue";
import { BizError } from "@/api/request";
import { useAuthStore } from "@/store/auth";

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const formRef = ref<FormInstance>();
const loading = ref(false);
const form = reactive({ username: "admin", password: "Admin@123456" });
const rules: FormRules = {
  username: [{ required: true, message: "请输入管理员账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入密码", trigger: "blur" }],
};

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;
  loading.value = true;
  try {
    await auth.login(form.username, form.password);
    ElMessage.success("登录成功");
    router.replace(String(route.query.redirect || "/dashboard"));
  } catch (error) {
    ElMessage.error(error instanceof BizError ? error.message : "登录失败");
  } finally {
    loading.value = false;
  }
}
</script>
