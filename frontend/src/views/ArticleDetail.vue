<template>
  <div class="article-detail-page">
    <LoadingSpinner v-if="loading" text="加载文章中..." />

    <ErrorMessage
      v-else-if="error"
      :message="error"
      :retry="fetchArticle"
    />

    <template v-else-if="article">
      <article class="article-full">
        <header class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>
          <div class="article-meta">
            <span class="article-date">{{ formatDate(article.createdAt) }}</span>
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
        </header>

        <div class="article-body markdown-body" v-html="renderedContent"></div>
      </article>

      <section class="comments-section">
        <h2 class="comments-title">评论 ({{ comments.length }})</h2>

        <div class="comment-form-wrapper card">
          <h3 class="comment-form-title">发表评论</h3>
          <form @submit.prevent="submitComment">
            <div class="form-row">
              <div class="form-group form-group-inline">
                <label class="form-label">昵称</label>
                <input
                  v-model="commentForm.nickname"
                  type="text"
                  class="form-input"
                  placeholder="请输入昵称"
                  required
                />
              </div>
              <div class="form-group form-group-inline">
                <label class="form-label">邮箱</label>
                <input
                  v-model="commentForm.email"
                  type="email"
                  class="form-input"
                  placeholder="请输入邮箱"
                  required
                />
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">评论内容</label>
              <textarea
                v-model="commentForm.content"
                class="form-textarea"
                placeholder="写下你的想法..."
                rows="4"
                required
              ></textarea>
            </div>
            <p v-if="commentError" class="form-error">{{ commentError }}</p>
            <button
              type="submit"
              class="btn btn-primary"
              :disabled="commentSubmitting"
            >{{ commentSubmitting ? '提交中...' : '发表评论' }}</button>
          </form>
        </div>

        <div v-if="commentsLoading" class="comments-loading">
          <LoadingSpinner text="加载评论中..." />
        </div>

        <div v-else-if="comments.length === 0" class="comments-empty">
          <p>暂无评论，来抢沙发吧！</p>
        </div>

        <div v-else class="comments-list">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item card"
          >
            <div class="comment-header">
              <span class="comment-author">{{ comment.nickname || '匿名' }}</span>
              <span class="comment-date">{{ formatDate(comment.createdAt) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { marked } from 'marked'
import { getArticle, getComments, addComment } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'

const route = useRoute()

const article = ref(null)
const loading = ref(true)
const error = ref('')

const comments = ref([])
const commentsLoading = ref(false)
const commentSubmitting = ref(false)
const commentError = ref('')

const commentForm = ref({
  nickname: '',
  email: '',
  content: ''
})

const renderedContent = computed(() => {
  if (!article.value || !article.value.content) return ''
  return marked(article.value.content)
})

async function fetchArticle() {
  loading.value = true
  error.value = ''
  try {
    const id = route.params.id
    const res = await getArticle(id)
    article.value = res.data || res
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '加载文章失败'
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  commentsLoading.value = true
  try {
    const id = route.params.id
    const res = await getComments(id)
    const data = res.data || res
    comments.value = data.records || data.list || data || []
  } catch (e) {
    // Comments failing is non-critical; just log
    console.error('Failed to load comments:', e)
  } finally {
    commentsLoading.value = false
  }
}

async function submitComment() {
  commentError.value = ''
  commentSubmitting.value = true
  try {
    const id = route.params.id
    await addComment(id, {
      nickname: commentForm.value.nickname,
      email: commentForm.value.email,
      content: commentForm.value.content
    })
    commentForm.value.content = ''
    await fetchComments()
  } catch (e) {
    commentError.value = e.response?.data?.message || e.message || '评论提交失败'
  } finally {
    commentSubmitting.value = false
  }
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
  fetchArticle()
  fetchComments()
})
</script>

<style scoped>
.article-header {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid var(--border);
}

.article-title {
  font-size: 2rem;
  margin-bottom: 0.75rem;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

/* Markdown body */
.markdown-body {
  line-height: 1.8;
  font-size: 1rem;
  color: var(--text);
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
}

.markdown-body :deep(p) {
  margin-bottom: 1rem;
}

.markdown-body :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 1rem;
  border-radius: var(--radius);
  overflow-x: auto;
  margin-bottom: 1rem;
}

.markdown-body :deep(code) {
  font-family: var(--font-mono);
  font-size: 0.875rem;
  background: #f1f5f9;
  padding: 0.15rem 0.4rem;
  border-radius: var(--radius-sm);
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid var(--primary);
  padding-left: 1rem;
  color: var(--text-secondary);
  margin-bottom: 1rem;
}

.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
  margin: 1rem 0;
}

/* Comments */
.comments-section {
  margin-top: 3rem;
  padding-top: 2rem;
  border-top: 1px solid var(--border);
}

.comments-title {
  margin-bottom: 1.5rem;
}

.comment-form-wrapper {
  margin-bottom: 2rem;
}

.comment-form-title {
  font-size: 1rem;
  margin-bottom: 1rem;
}

.form-row {
  display: flex;
  gap: 1rem;
}

.form-group-inline {
  flex: 1;
}

.form-error {
  color: var(--danger);
  font-size: 0.875rem;
  margin-bottom: 0.75rem;
}

.comments-empty {
  text-align: center;
  padding: 2rem;
  color: var(--text-muted);
}

.comment-item + .comment-item {
  margin-top: 0.75rem;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.comment-author {
  font-weight: 600;
  font-size: 0.9375rem;
}

.comment-date {
  font-size: 0.8125rem;
  color: var(--text-muted);
}

.comment-content {
  font-size: 0.9375rem;
  color: var(--text);
  white-space: pre-wrap;
}

@media (max-width: 640px) {
  .article-title {
    font-size: 1.5rem;
  }

  .form-row {
    flex-direction: column;
    gap: 0;
  }
}
</style>
