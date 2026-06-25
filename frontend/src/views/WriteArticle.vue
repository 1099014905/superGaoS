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
        <div class="editor-toolbar">
          <button
            type="button"
            class="btn btn-outline btn-sm"
            :disabled="uploading"
            @click="triggerUpload"
          >📷 上传图片</button>
          <button
            type="button"
            class="btn btn-outline btn-sm"
            :disabled="uploading"
            @click="triggerVideoUpload"
          >🎬 上传视频</button>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display:none"
            @change="handleFileUpload"
          />
          <input
            ref="videoFileInput"
            type="file"
            accept="video/*"
            style="display:none"
            @change="handleVideoUpload"
          />
        </div>
        <div v-if="uploading && uploadProgress > 0" class="upload-progress">
          <div class="upload-progress-info">
            <span class="upload-progress-file">🎬 {{ uploadFileName }}</span>
            <span class="upload-progress-pct">{{ uploadProgress }}%</span>
          </div>
          <div class="upload-progress-detail">
            <span>{{ formatFileSize(uploadFileSize * uploadProgress / 100) }} / {{ formatFileSize(uploadFileSize) }}</span>
          </div>
          <div class="upload-progress-bar">
            <div class="upload-progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
        </div>
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
          <option :value="1">草稿</option>
          <option :value="2">发布</option>
        </select>
      </div>

      <p v-if="uploadError" class="form-error">{{ uploadError }}</p>
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
import { getArticle, createArticle, updateArticle, uploadImage, uploadVideo } from '../api'
import LoadingSpinner from '../components/LoadingSpinner.vue'
import ErrorMessage from '../components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()

const isEditing = computed(() => !!route.params.id)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  status: 1
})

const loading = ref(false)
const loadError = ref('')
const submitting = ref(false)
const submitError = ref('')
const uploadError = ref('')
const uploading = ref(false)
const fileInput = ref(null)
const videoFileInput = ref(null)
const uploadProgress = ref(0)
const uploadFileName = ref('')
const uploadFileSize = ref(0)

function triggerUpload() {
  fileInput.value?.click()
}

function triggerVideoUpload() {
  videoFileInput.value?.click()
}

async function handleFileUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const res = await uploadImage(file)
    const data = res.data || res
    const url = data.url || data
    const markdown = `![${file.name}](${url})`
    const el = document.querySelector('.editor-area')
    if (el) {
      const start = el.selectionStart
      const end = el.selectionEnd
      form.content = form.content.substring(0, start) + markdown + form.content.substring(end)
      el.focus()
      el.selectionStart = el.selectionEnd = start + markdown.length
    } else {
      form.content += '\n' + markdown
    }
  } catch (e) {
    submitError.value = '图片上传失败：' + (e.response?.data?.message || e.message)
  } finally {
    uploading.value = false
    fileInput.value.value = ''
  }
}

async function handleVideoUpload(e) {
  const file = e.target.files?.[0]
  if (!file) return
  uploading.value = true
  uploadProgress.value = 0
  uploadFileName.value = file.name
  uploadFileSize.value = file.size
  try {
    const res = await uploadVideo(file, (progressEvent) => {
      if (progressEvent.total) {
        uploadProgress.value = Math.round((progressEvent.loaded / progressEvent.total) * 100)
      }
    })
    const data = res.data || res
    const url = data.url || data
    const videoTag = `<video src="${url}" controls preload="metadata" playsinline style="max-width:100%"></video>`
    const el = document.querySelector('.editor-area')
    if (el) {
      const start = el.selectionStart
      const end = el.selectionEnd
      form.content = form.content.substring(0, start) + videoTag + form.content.substring(end)
      el.focus()
      el.selectionStart = el.selectionEnd = start + videoTag.length
    } else {
      form.content += '\n' + videoTag
    }
  } catch (e) {
    uploadError.value = '视频上传失败：' + (e.response?.data?.message || e.message)
  } finally {
    uploading.value = false
    uploadProgress.value = 0
    videoFileInput.value.value = ''
  }
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

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
    form.status = article.status || 1
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

.editor-toolbar {
  margin-bottom: 0.5rem;
}

.upload-progress {
  margin-bottom: 0.5rem;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--bg);
}

.upload-progress-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.25rem;
}

.upload-progress-file {
  font-size: 0.875rem;
  font-weight: 600;
}

.upload-progress-pct {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--primary);
}

.upload-progress-detail {
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}

.upload-progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.upload-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), #818cf8);
  border-radius: 8px;
  transition: width 0.3s ease;
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
