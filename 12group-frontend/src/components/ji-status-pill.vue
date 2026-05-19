<template>
  <text class="ji-status-pill" :class="`ji-tone-${tone}`">{{ text }}</text>
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

<style>
.ji-status-pill {
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

.ji-tone-idle {
  color: #718096;
  background: #edf2f7;
}

.ji-tone-warn {
  color: #a35b00;
  background: #fff2d6;
}

.ji-tone-active {
  color: #1f4fd8;
  background: #e8efff;
}

.ji-tone-done {
  color: #0f8f72;
  background: #dff8ef;
}

.ji-tone-danger {
  color: #d6455d;
  background: #ffe8ee;
}
</style>
