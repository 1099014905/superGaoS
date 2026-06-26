<template>
  <div class="orders-page">
    <h1 class="page-title">我的秒杀订单</h1>

    <LoadingSpinner v-if="loading" text="加载订单中..." />

    <ErrorMessage v-else-if="error" :message="error" :retry="fetchOrders" />

    <EmptyState v-else-if="orders.length === 0" title="暂无订单" description="快去秒杀吧！" />

    <div v-else class="orders-list">
      <div v-for="order in orders" :key="order.id" class="card order-card">
        <div class="order-header">
          <span class="order-id">订单 #{{ order.id }}</span>
          <span class="order-status">{{ order.status === 0 ? '待支付' : order.status === 1 ? '已支付' : '已取消' }}</span>
        </div>
        <div class="order-body">
          <span>活动ID：{{ order.activityId }}</span>
          <span>金额：¥{{ order.amount }}</span>
          <span>时间：{{ formatDate(order.createTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSeckillOrders } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import EmptyState from '../components/EmptyState.vue'

const orders = ref([])
const loading = ref(true)
const error = ref('')

async function fetchOrders() {
  loading.value = true
  error.value = ''
  try {
    const res = await getSeckillOrders()
    const data = res.data || res
    orders.value = data || []
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString()
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-card { padding: 1rem; margin-bottom: 0.75rem; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 0.5rem; }
.order-id { font-weight: 600; }
.order-status { font-weight: 600; color: var(--primary); }
.order-body { display: flex; gap: 1.5rem; font-size: 0.875rem; color: var(--text-secondary); }
</style>
