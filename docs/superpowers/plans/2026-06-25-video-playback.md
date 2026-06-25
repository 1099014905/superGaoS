# Video Playback Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add video upload and playback support to the blog article editor and detail page.

**Architecture:** Reuse existing file-service upload API (`POST /api/file/upload`) and MinIO storage. Frontend adds a video upload button with progress bar in WriteArticle.vue and renders `<video>` with custom CSS in ArticleDetail.vue.

**Tech Stack:** Spring Boot 3.2.5, MinIO, Vue 3, marked, Axios, Nginx

## Global Constraints

- max-file-size: 500MB, max-request-size: 500MB
- Nginx client_max_body_size 500m
- Use existing file-service upload endpoint (no new backend API)
- Frontend upload via Axios with onUploadProgress
- Video formats: .mp4, .webm, .mov (frontend accept="video/*")
- Markdown editor inserts `<video src="..." controls preload="metadata" style="max-width:100%"></video>`
- No third-party video player library — use native `<video>` with CSS styling

---

### Task 1: Increase file upload limits (backend + nginx)

**Files:**
- Modify: `supergaos-file/src/main/resources/application.yml:16-18`
- Modify: `frontend/nginx.conf:11-17`

**Interfaces:**
- Consumes: N/A
- Produces: File service accepts up to 500MB uploads, nginx proxies up to 500MB

- [ ] **Step 1: Increase file-service upload limit**

In `supergaos-file/src/main/resources/application.yml`, change the `spring.servlet.multipart` block from:

```yaml
  servlet:
    multipart:
      max-file-size: 10MB
```

to:

```yaml
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB
```

- [ ] **Step 2: Add Nginx upload size limit**

In `frontend/nginx.conf`, add `client_max_body_size 500m;` inside the `location /api/` block:

```nginx
    location /api/ {
        client_max_body_size 500m;
        proxy_pass http://gateway-service:9090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
```

- [ ] **Step 3: Verify compilation**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS && mvn clean compile -DskipTests -B`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add supergaos-file/src/main/resources/application.yml frontend/nginx.conf
git commit -m "feat: increase upload limits to 500MB for video support"
```

---

### Task 2: Add uploadVideo() API function

**Files:**
- Modify: `frontend/src/api/index.js:70-77`

**Interfaces:**
- Consumes: `POST /api/file/upload` (existing backend endpoint)
- Produces: `uploadVideo(file, onProgress)` function returning `Promise<{url}>`

- [ ] **Step 1: Add uploadVideo function**

In `frontend/src/api/index.js`, add after the `uploadImage` function (after line 77):

```javascript
export function uploadVideo(file, onProgress) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  })
}
```

- [ ] **Step 2: Verify frontend build**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS/frontend && npm run build 2>&1 | tail -5`
Expected: built successfully

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/index.js
git commit -m "feat: add uploadVideo API function with progress callback"
```

---

### Task 3: Add video upload button and progress bar to editor

**Files:**
- Modify: `frontend/src/views/WriteArticle.vue`

**Interfaces:**
- Consumes: `uploadVideo(file, onProgress)` from Task 2
- Produces: Users can upload videos from editor with progress feedback

- [ ] **Step 1: Update imports to include uploadVideo**

In `frontend/src/views/WriteArticle.vue:85`, add `uploadVideo` to the import:

```javascript
import { getArticle, createArticle, updateArticle, uploadImage, uploadVideo } from '../api'
```

- [ ] **Step 2: Add video upload reactive state**

After `const fileInput = ref(null)` (line 106), add:

```javascript
const videoFileInput = ref(null)
const uploadProgress = ref(0)
const uploadFileName = ref('')
const uploadFileSize = ref(0)
```

- [ ] **Step 3: Add video upload trigger function**

After `function triggerUpload()` (line 108-110), add:

```javascript
function triggerVideoUpload() {
  videoFileInput.value?.click()
}
```

- [ ] **Step 4: Add video upload handler with progress**

After `async function handleFileUpload(e)` (line 112-137), add:

```javascript
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
    const videoTag = `<video src="${url}" controls preload="metadata" style="max-width:100%"></video>`
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
    submitError.value = '视频上传失败：' + (e.response?.data?.message || e.message)
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
```

- [ ] **Step 5: Add video upload button and progress bar to template**

Replace the existing `editor-toolbar` div (lines 37-50) with the new toolbar including both buttons:

```html
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
```

Add before the textarea (after the editor-toolbar div).

- [ ] **Step 6: Add progress bar CSS**

In the `<style scoped>` section, add after `.editor-toolbar` rule:

```css
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
```

- [ ] **Step 7: Verify frontend build**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS/frontend && npm run build 2>&1 | tail -5`
Expected: built successfully

- [ ] **Step 8: Commit**

```bash
git add frontend/src/views/WriteArticle.vue
git commit -m "feat: add video upload button with progress bar to article editor"
```

---

### Task 4: Add video player CSS to article detail page

**Files:**
- Modify: `frontend/src/views/ArticleDetail.vue`

**Interfaces:**
- Consumes: Article content with `<video>` tags (from Task 3)
- Produces: Video player with custom dark control bar

- [ ] **Step 1: Add video player styles**

In `<style scoped>` section of `ArticleDetail.vue`, after the existing `.markdown-body` rules, add:

```css
.markdown-body :deep(video) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius);
  display: block;
  margin: 1rem 0;
  background: #000;
}
```

- [ ] **Step 2: Verify frontend build**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS/frontend && npm run build 2>&1 | tail -5`
Expected: built successfully

- [ ] **Step 3: Commit**

```bash
git add frontend/src/views/ArticleDetail.vue
git commit -m "feat: add video player styles to article detail page"
```

---

### Task 5: Full verification

**No file changes** — this is a manual verification task.

- [ ] **Step 1: Backend build verification**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS && mvn clean package -DskipTests -B`
Expected: BUILD SUCCESS for all 7 modules

- [ ] **Step 2: Frontend build verification**

Run: `cd C:/Users/Administrator/IdeaProjects/superGaoS/frontend && npm run build`
Expected: built successfully

- [ ] **Step 3: Push to remote**

```bash
git push origin main
```

- [ ] **Step 4: Deploy and test on server**

```bash
# on server:
git pull
docker-compose up -d --build

# verify:
# 1. Login → Create article
# 2. Click "🎬 上传视频" → select a video
# 3. Verify progress bar appears and updates
# 4. Verify <video> tag is inserted into content
# 5. Save article → open article detail page
# 6. Verify video player renders and playback works
```
