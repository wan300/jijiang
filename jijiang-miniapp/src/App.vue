<script setup lang="ts">
import { onLaunch } from "@dcloudio/uni-app";
import { useUserStore } from "@/store/user";

onLaunch(async () => {
  const user = useUserStore();
  if (user.token) {
    await user.refreshUserInfo().catch(() => user.clearSession());
  }
});
</script>
<style>
page {
  min-height: 100%;
  background: #f5f7fb;
  color: #152033;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", Arial, sans-serif;
}

view,
text,
button,
input,
textarea {
  box-sizing: border-box;
}

button {
  margin: 0;
  padding: 0;
  border: 0;
  line-height: 1.4;
  background: transparent;
}

button::after {
  border: 0;
}

.page-shell {
  min-height: 100vh;
  padding: 28rpx;
  background:
    radial-gradient(circle at 10% 2%, rgba(38, 197, 169, 0.18), transparent 34%),
    radial-gradient(circle at 90% 0%, rgba(67, 97, 238, 0.2), transparent 30%),
    linear-gradient(180deg, #eef5ff 0%, #f7f8fc 42%, #f8fafc 100%);
}

.page-with-tab {
  padding-bottom: 150rpx;
}

.hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 36rpx;
  padding: 36rpx;
  color: #fff;
  background: linear-gradient(135deg, #101c4c 0%, #1f4fd8 58%, #25c5a9 100%);
  box-shadow: 0 28rpx 70rpx rgba(31, 79, 216, 0.24);
}

.glass-card,
.surface-card {
  border: 1rpx solid rgba(255, 255, 255, 0.72);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18rpx 48rpx rgba(35, 50, 91, 0.08);
}

.section-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 34rpx 4rpx 20rpx;
  color: #152033;
  font-size: 34rpx;
  font-weight: 800;
}

.muted {
  color: #76839a;
}

.primary-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 92rpx;
  border-radius: 999rpx;
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  background: linear-gradient(135deg, #1f4fd8, #25c5a9);
  box-shadow: 0 16rpx 34rpx rgba(31, 79, 216, 0.22);
}

.ghost-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 82rpx;
  border: 1rpx solid rgba(31, 79, 216, 0.18);
  border-radius: 999rpx;
  color: #1f4fd8;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.72);
}

.danger-text {
  color: #f05a71;
}

.field {
  margin-bottom: 22rpx;
}

.field-label {
  margin-bottom: 12rpx;
  color: #647089;
  font-size: 24rpx;
  font-weight: 700;
}

.field-input,
.field-textarea {
  width: 100%;
  border-radius: 24rpx;
  padding: 24rpx;
  color: #152033;
  background: rgba(255, 255, 255, 0.92);
}

.field-textarea {
  min-height: 190rpx;
}
</style>
