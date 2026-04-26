<template>
  <text class="pill" :class="`tone-${tone}`">{{ text }}</text>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    text: string;
    tone?: "idle" | "warn" | "active" | "done" | "danger";
  }>(),
  { tone: "active" },
);
</script>

<style scoped>
.pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 112rpx;
  height: 44rpx;
  border-radius: 999rpx;
  padding: 0 18rpx;
  font-size: 22rpx;
  font-weight: 800;
}

.tone-idle {
  color: #718096;
  background: #edf2f7;
}

.tone-warn {
  color: #a35b00;
  background: #fff2d6;
}

.tone-active {
  color: #1f4fd8;
  background: #e8efff;
}

.tone-done {
  color: #0f8f72;
  background: #dff8ef;
}

.tone-danger {
  color: #d6455d;
  background: #ffe8ee;
}
</style>
