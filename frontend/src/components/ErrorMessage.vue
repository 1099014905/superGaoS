<template>
  <div class="error-message">
    <div class="error-icon">&#9888;</div>
    <p class="error-text">{{ message }}</p>
    <button v-if="retry" class="btn btn-outline" @click="retry">重试</button>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: '加载失败，请稍后重试'
  },
  retry: {
    type: Function,
    default: null
  }
})
</script>

<style scoped>
.error-message {
  text-align: center;
  padding: 3rem 1rem;
}

.error-icon {
  font-size: 2.5rem;
  color: var(--danger);
  margin-bottom: 0.75rem;
  line-height: 1;
}

.error-text {
  font-size: 0.9375rem;
  color: var(--text-secondary);
  margin-bottom: 1.25rem;
}
</style>
