<template>
  <div class="write-page">
    <h1 class="page-title">{{ isEditing ? '编辑文章' : '写文章' }}</h1>

    <LoadingSpinner v-if="loading" text="加载文章数据..." />

    <ErrorMessage
      v-else-if="loadError"
      :message="loadError"
      :retry="loadArticle"
    />

    <form v-else class="write-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label class="form-label">标题</label>
        <input
          v-model="form.title"
          type="text"
          class="form-input form-input-lg"
          placeholder="文章标题"
          required
        />
      </div>

      <div class="form-group">
        <label class="form-label">摘要</label>
        <input
          v-model="form.summary"
          type="text"
          class="form-input"
          placeholder="简要描述文章内容（可选）"
        />
      </div>

      <div class="form-group">
        <label class="form-label">内容 (Markdown)</label>
        <textarea
          v-model="form.content"
          class="form-textarea editor-area"
          placeholder="在此撰写 Markdown 内容..."
          required
        ></textarea>
      </div>

      <div class="form-group">
        <label class="form-label">状态</label>
        <select v-model="form.status" class="form-select">
          <option value="draft">草稿</option>
          <option value="published">发布</option>
        </select>
      </div>

      <p v-if="submitError" class="form-error">{{ submitError }}</p>

      <div class="form-actions">
        <button
          type="submit"
          class="btn btn-primary"
          :disabled="submitting"
        >{{ submitting ? '保存中...' : (isEditing ? '更新文章' : '发布文章') }}</button>
        <router-link to="/admin" class="btn btn-ghost">取消</router-link>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle, createArticle, updateArticle } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()

const isEditing = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  status: 'draft'
})

const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')

async function loadArticle() {
  if (!isEditing.value) return
  loading.value = true
  loadError.value = ''
  try {
    const res = await getArticle(route.params.id)
    const article = res.data || res
    form.title = article.title || ''
    form.summary = article.summary || ''
    form.content = article.content || ''
    form.status = article.status || 'draft'
  } catch (e) {
    loadError.value = e.response?.data?.message || e.message || '加载文章失败'
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  submitError.value = ''
  submitting.value = true
  try {
    const payload = {
      title: form.title,
      summary: form.summary,
      content: form.content,
      status: form.status
    }

    if (isEditing.value) {
      await updateArticle(route.params.id, payload)
    } else {
      await createArticle(payload)
    }

    router.push('/admin')
  } catch (e) {
    submitError.value = e.response?.data?.message || e.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped>
.page-title {
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
}

.write-form {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 2rem;
  box-shadow: var(--shadow-sm);
}

.form-input-lg {
  font-size: 1.125rem;
  font-weight: 600;
}

.editor-area {
  min-height: 400px;
  font-family: var(--font-mono);
  font-size: 0.9375rem;
  line-height: 1.7;
}

.form-select {
  max-width: 200px;
}

.form-error {
  color: var(--danger);
  font-size: 0.875rem;
  margin-bottom: 1rem;
}

.form-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}

@media (max-width: 640px) {
  .write-form {
    padding: 1.25rem;
  }

  .editor-area {
    min-height: 300px;
  }
}
</style>
