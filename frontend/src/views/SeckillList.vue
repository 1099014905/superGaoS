<template>
  <div class="seckill-page">
    <h1 class="page-title">秒杀活动</h1>

    <LoadingSpinner v-if="loading" text="加载秒杀活动中..." />

    <ErrorMessage v-else-if="error" :message="error" :retry="fetchList" />

    <EmptyState v-else-if="activities.length === 0" title="暂无秒杀活动" description="敬请期待！" />

    <div v-else class="seckill-grid">
      <div v-for="item in activities" :key="item.id" class="card seckill-card">
        <div class="seckill-header">
          <h3>{{ item.title }}</h3>
          <span class="seckill-price">¥{{ item.price }}</span>
        </div>
        <div class="seckill-info">
          <span>剩余库存：{{ item.stock }}</span>
          <span class="seckill-status" :class="'status-' + getStatus(item)">{{ getStatusText(item) }}</span>
        </div>
        <div class="seckill-time">{{ formatTime(item) }}</div>
        <button
          class="btn btn-primary btn-block"
          :disabled="!canGrab(item)"
          @click="handleGrab(item)"
        >{{ grabBtnText(item) }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getSeckillList, grabSeckill } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()
const activities = ref([])
const loading = ref(true)
const error = ref('')

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const res = await getSeckillList()
    const data = res.data || res
    activities.value = data || []
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function getStatus(item) {
  const now = Date.now()
  const start = new Date(item.startTime).getTime()
  const end = new Date(item.endTime).getTime()
  if (now < start) return 0
  if (now > end) return 2
  return 1
}

function getStatusText(item) {
  const s = getStatus(item)
  return s === 0 ? '未开始' : s === 1 ? '进行中' : '已结束'
}

function canGrab(item) {
  return getStatus(item) === 1 && item.stock > 0
}

function grabBtnText(item) {
  const s = getStatus(item)
  if (s === 0) return '即将开始'
  if (s === 2) return '已结束'
  if (item.stock <= 0) return '已售罄'
  return '立即抢购'
}

function formatTime(item) {
  const s = getStatus(item)
  const start = new Date(item.startTime)
  const end = new Date(item.endTime)
  if (s === 0) return '开始时间：' + start.toLocaleString()
  if (s === 2) return '已结束'
  return '结束时间：' + end.toLocaleString()
}

async function handleGrab(item) {
  const token = localStorage.getItem('token')
  if (!token) {
    router.push('/login?redirect=/seckill')
    return
  }
  try {
    const res = await grabSeckill(item.id)
    const data = res.data || res
    alert(data || '抢购成功')
    await fetchList()
  } catch (e) {
    alert(e.response?.data?.message || e.message || '抢购失败')
  }
}

onMounted(fetchList)
</script>

<style scoped>
.seckill-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1rem;
}
.seckill-card { padding: 1.25rem; }
.seckill-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 0.75rem; }
.seckill-price { font-size: 1.5rem; font-weight: 700; color: var(--danger); }
.seckill-info { display: flex; justify-content: space-between; font-size: 0.875rem; margin-bottom: 0.5rem; }
.seckill-status { font-weight: 600; }
.status-0 { color: var(--text-muted); }
.status-1 { color: var(--success); }
.status-2 { color: var(--text-muted); }
.seckill-time { font-size: 0.8125rem; color: var(--text-muted); margin-bottom: 1rem; }
</style>
