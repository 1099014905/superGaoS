# 博客系统视频播放功能设计

## 概述

在博客文章中支持上传和播放视频文件。作者可在编辑器中上传视频，在文章详情页渲染为自定义视频播放器。

## 上传流程

### 编辑器 UI

工具栏新增「🎬 上传视频」按钮，与现有「📷 上传图片」并排：

```
[📷 上传图片]  [🎬 上传视频]
```

点击「上传视频」→ 弹出文件选择器，仅显示视频格式（`.mp4`, `.webm`, `.mov`）。

### 上传进度

上传过程中显示详细信息型进度条：

```
┌──────────────────────────────────────────┐
│ 🎬 demo.mp4                             │
│ 45.2 MB / 62.8 MB                   72%  │
│ ╺━━━━━━━━━━━━━━━╺━━━━━━━━━━━━━━━━━━━━╸  │
└──────────────────────────────────────────┘
```

- 显示文件名
- 已上传大小 / 总大小
- 百分比
- 渐变进度条

### 上传成功

上传完成后，在编辑器光标位置插入 `<video>` 标签：

```html
<video src="/api/file/{id}/download" controls preload="metadata" style="max-width:100%"></video>
```

## 播放器

### 渲染方式

文章详情页使用 marked 渲染 Markdown 内容。现有 marked 配置已支持 HTML 标签，`<video>` 标签会保留。

### 播放器样式

使用原生 `<video controls>` 标签，通过 CSS 自定义深色控制栏：

```
┌─────────────────────────────────────────────────┐
│                                                  │
│                  视频画面区域                      │
│                                                  │
│                                                  │
│  ▶ ╺━━━━━━━━━━━━━━━━━╺━━━━━━━━┫ HD  1:23 / 5:45  │
└─────────────────────────────────────────────────┘
```

- 控制栏深色背景
- 显示播放/暂停、进度条、当前时间/总时长
- 视频宽度自适应（`max-width: 100%; height: auto`）
- 浏览器原生支持（无第三方依赖）

## 后端改动

### 上传大小限制

| 文件 | 修改 | 原值 | 新值 |
|------|------|------|------|
| `supergaos-file/.../application.yml` | `spring.servlet.multipart.max-file-size` | 10MB | 500MB |
| `supergaos-file/.../application.yml` | `spring.servlet.multipart.max-request-size` | 10MB | 500MB |
| `frontend/nginx.conf` | 新增 `client_max_body_size` | 无 | 500m |

### MinIO 存储

直接使用现有文件上传 API（`POST /api/file/upload`），视频文件与图片存储在同一 MinIO bucket。无需新增存储接口。

## 前端改动清单

| 文件 | 改动 |
|------|------|
| `frontend/src/views/WriteArticle.vue` | 新增「🎬 上传视频」按钮及对应上传逻辑 |
| `frontend/src/views/WriteArticle.vue` | 新增文件类型过滤（`accept="video/*"）` |
| `frontend/src/views/WriteArticle.vue` | 新增上传进度条 UI（详细信息型） |
| `frontend/src/views/ArticleDetail.vue` | 为 `<video>` 标签添加自定义播放器 CSS |
| `frontend/src/api/index.js` | 新增 `uploadVideo()` API 函数（复用现有上传接口） |

## 文件上传 API

现有 `POST /api/file/upload` 接口可直接用于视频上传：

```
Request:  multipart/form-data { file: <视频文件>, articleId: <可选> }
Response: { code: 200, data: { url: "/api/file/{id}/download", ... } }
```

前端通过 axios 的 `onUploadProgress` 实现上传进度跟踪。

## 边界情况

| 场景 | 处理 |
|------|------|
| 上传超大文件（>500MB） | 后端返回 413 Payload Too Large |
| 上传非视频文件 | 前端通过 `accept="video/*"` 限制，后端不做严格校验 |
| 上传失败 | 进度条消失，显示错误提示 |
| 视频 URL 无法播放 | 浏览器原生 `<video>` 会显示失败状态 |
| 视频播放尺寸 | CSS `max-width: 100%; height: auto` 自适应 |

## 实现顺序

1. 后端配置：调大上传限制（file-service application.yml）
2. Nginx 配置：添加 `client_max_body_size 500m`
3. 前端：编辑器视频上传按钮 + 进度条
4. 前端：播放器样式
5. 前后端联调验证
