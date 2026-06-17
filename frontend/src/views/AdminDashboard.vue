<template>
  <div class="admin-page">
    <div class="admin-header">
      <h1 class="admin-title">文章管理</h1>
      <router-link to="/admin/write" class="btn btn-primary">写新文章</router-link>
    </div>

    <LoadingSpinner v-if="loading" text="加载文章列表中..." />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      :retry="fetchArticles"
    />

    <EmptyState
      v-else-if="articles.length === 0"
      icon="&#128221;"
      title="还没有文章"
      description="点击「写新文章」开始创作吧！"
    >
      <router-link to="/admin/write" class="btn btn-primary">写新文章</router-link>
    </EmptyState>

    <template v-else>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
            <tr>
              <th>标题</th>
              <th>状态</th>
              <th>评论</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="article in articles" :key="article.id">
              <td class="table-title">
                <router-link :to="`/article/${article.id}`" target="_blank">{{ article.title }}</router-link>
              </td>
              <td>
                <span
                  class="badge"
                  :class="article.status === 'published' ? 'badge-success' : 'badge-warning'"
                >{{ article.status === 'published' ? '已发布' : '草稿' }}</span>
              </td>
              <td class="table-count">{{ article.commentCount || 0 }}</td>
              <td class="table-date">{{ formatDate(article.createdAt) }}</td>
              <td class="table-actions">
                <router-link
                  :to="`/admin/write/${article.id}`"
                  class="btn btn-outline btn-sm"
                >编辑</router-link>
                <button
                  class="btn btn-ghost btn-sm"
                  :disabled="deletingId === article.id"
                  @click="handleDelete(article)"
                >{{ deletingId === article.id ? '...' : '删除' }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="totalPages > 1" class="pagination">
        <button
          :disabled="currentPage <= 1"
          @click="goToPage(currentPage - 1)"
        >上一页</button>
        <button
          v-for="page in visiblePages"
          :key="page"
          :class="{ active: page === currentPage }"
          @click="goToPage(page)"
        >{{ page }}</button>
        <button
          :disabled="currentPage >= totalPages"
          @click="goToPage(currentPage + 1)"
        >下一页</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getArticles, deleteArticle } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import EmptyState from '../components/EmptyState.vue'

const articles = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const loading = ref(true)
const error = ref('')
const deletingId = ref(null)

async function fetchArticles() {
  loading.value = true
  error.value = ''
  try {
    const res = await getArticles(currentPage.value, 10)
    const data = res.data || res
    articles.value = data.records || data.list || []
    const total = data.total || 0
    totalPages.value = data.pages || Math.ceil(total / 10) || 1
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载文章列表失败'
  } finally {
    loading.value = false
  }
}

const visiblePages = computed(() => {
  const pages = []
  const maxVisible = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  let end = start + maxVisible - 1
  if (end > totalPages.value) {
    end = totalPages.value
    start = Math.max(1, end - maxVisible + 1)
  }
  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchArticles()
}

async function handleDelete(article) {
  if (!confirm(`确定删除文章「${article.title}」吗？此操作不可撤销。`)) return
  deletingId.value = article.id
  try {
    await deleteArticle(article.id)
    if (articles.value.length === 1 && currentPage.value > 1) {
      currentPage.value--
    }
    await fetchArticles()
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '删除失败'
    alert(msg)
  } finally {
    deletingId.value = null
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.admin-title {
  font-size: 1.5rem;
}

.table-wrapper {
  overflow-x: auto;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9375rem;
}

.admin-table th {
  text-align: left;
  padding: 0.75rem 1rem;
  font-weight: 600;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: var(--bg);
  border-bottom: 1px solid var(--border);
}

.admin-table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--border);
  color: var(--text);
}

.admin-table tr:last-child td {
  border-bottom: none;
}

.table-title {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-title a {
  color: var(--text);
  font-weight: 500;
}

.table-title a:hover {
  color: var(--primary);
}

.table-count {
  text-align: center;
}

.table-date {
  white-space: nowrap;
  color: var(--text-muted);
  font-size: 0.875rem;
}

.table-actions {
  display: flex;
  gap: 0.5rem;
  white-space: nowrap;
}
</style>
