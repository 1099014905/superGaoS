import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor: attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: handle 401
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// ---- Auth ----
export function login(username, password) {
  return api.post('/user/login', { username, password })
}

// ---- Articles ----
export function getArticles(page = 1, size = 10) {
  return api.get('/blog/articles', { params: { page, size } })
}

export function getArticle(id) {
  return api.get(`/blog/articles/${id}`)
}

export function createArticle(data) {
  return api.post('/blog/articles', data)
}

export function updateArticle(id, data) {
  return api.put(`/blog/articles/${id}`, data)
}

export function deleteArticle(id) {
  return api.delete(`/blog/articles/${id}`)
}

// ---- Comments ----
export function getComments(articleId) {
  return api.get('/comment/articles/' + articleId)
}

export function addComment(articleId, data) {
  return api.post('/comment/articles/' + articleId, data)
}

// ---- File upload ----
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return api.post('/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export default api
