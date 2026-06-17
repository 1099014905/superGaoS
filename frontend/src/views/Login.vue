<template>
  <div class="login-page">
    <div class="login-card card">
      <h1 class="login-title">管理员登录</h1>

      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input
            v-model="form.username"
            type="text"
            class="form-input"
            placeholder="请输入用户名"
            required
            autocomplete="username"
          />
        </div>

        <div class="form-group">
          <label class="form-label">密码</label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="请输入密码"
            required
            autocomplete="current-password"
          />
        </div>

        <p v-if="errorMsg" class="login-error">{{ errorMsg }}</p>

        <button
          type="submit"
          class="btn btn-primary btn-block"
          :disabled="submitting"
        >{{ submitting ? '登录中...' : '登录' }}</button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  password: ''
})

const errorMsg = ref('')
const submitting = ref(false)

async function handleLogin() {
  errorMsg.value = ''
  submitting.value = true
  try {
    const res = await login(form.username, form.password)
    // Extract token — gateway wraps response or token is directly in res
    const data = res.data || res
    const token = data.token || data.accessToken || data.access_token
    if (!token) {
      throw new Error('登录响应中未找到 token')
    }
    localStorage.setItem('token', token)
    const redirect = route.query.redirect || '/admin'
    router.push(redirect)
  } catch (e) {
    errorMsg.value = e.response?.data?.message || e.message || '登录失败，请检查用户名和密码'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 140px);
}

.login-card {
  width: 100%;
  max-width: 400px;
}

.login-title {
  text-align: center;
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
}

.btn-block {
  width: 100%;
}

.login-error {
  color: var(--danger);
  font-size: 0.875rem;
  margin-bottom: 1rem;
  text-align: center;
}
</style>
