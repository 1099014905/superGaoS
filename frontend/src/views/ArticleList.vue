<template>
  <div class="article-list-page">
    <h1 class="page-title">文章列表</h1>

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
      description="博主还在努力创作中，敬请期待！"
    />

    <template v-else>
      <div class="article-grid">
        <article
          v-for="article in articles"
          :key="article.id"
          class="card article-card"
          @click="goToDetail(article.id)"
        >
          <h2 class="article-title">{{ article.title }}</h2>
          <p class="article-summary">{{ article.summary || '暂无摘要' }}</p>
          <div class="article-meta">
            <span class="article-date">{{ formatDate(article.createTime) }}</span>
            <span v-if="article.commentCount != null" class="article-comments">
              &#128172; {{ article.commentCount }} 条评论
            </span>
            <span v-if="article.categories && article.categories.length" class="article-categories">
              <span
                v-for="cat in article.categories"
                :key="cat.id"
                class="tag"
              >{{ cat.name }}</span>
            </span>
          </div>
          <div v-if="article.tags && article.tags.length" class="article-tags">
            <span
              v-for="tag in article.tags"
              :key="tag.id"
              class="tag"
            >#{{ tag.name }}</span>
          </div>
        </article>
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
import { useRouter } from 'vue-router'
import { getArticles } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'
import EmptyState from '../components/EmptyState.vue'

const router = useRouter()

const articles = ref([])
const currentPage = ref(1)
const totalPages = ref(1)
const totalCount = ref(0)
const loading = ref(true)
const error = ref('')

async function fetchArticles() {
  loading.value = true
  error.value = ''
  try {
    const res = await getArticles(currentPage.value, 10)
    // Handle different response shapes from the gateway
    const data = res.data || res
    articles.value = data.articles || data.records || data.list || []
    totalCount.value = data.total || 0
    totalPages.value = data.pages || Math.ceil(totalCount.value / 10) || 1
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
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function goToDetail(id) {
  router.push({ name: 'ArticleDetail', params: { id } })
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

onMounted(() => {
  fetchArticles()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 1.5rem;
  font-size: 1.75rem;
}

.article-card {
  cursor: pointer;
}

.article-title {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
  transition: color 0.2s ease;
}

.article-card:hover .article-title {
  color: var(--primary);
}

.article-summary {
  color: var(--text-secondary);
  font-size: 0.9375rem;
  margin-bottom: 0.75rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.article-comments {
  color: var(--text-secondary);
}

.article-categories {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.article-tags {
  margin-top: 0.75rem;
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}
</style>
