<template>
  <div id="blog-app">
    <header class="site-header">
      <div class="container">
        <router-link to="/" class="logo">SuperGaoS Blog</router-link>
        <nav>
          <router-link to="/">首页</router-link>
          <router-link to="/seckill">秒杀</router-link>
          <router-link to="/login" v-if="!isLoggedIn">登录</router-link>
          <template v-else>
            <router-link to="/admin">管理</router-link>
            <a href="#" @click.prevent="logout">退出</a>
          </template>
        </nav>
      </div>
    </header>
    <main class="container">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const token = ref(localStorage.getItem('token') || '')
const isLoggedIn = computed(() => !!token.value)

function logout() {
  localStorage.removeItem('token')
  token.value = ''
  router.push('/')
}
</script>

<style scoped>
#blog-app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.site-header {
  background: #ffffff;
  border-bottom: 1px solid var(--border);
  box-shadow: var(--shadow-sm);
  position: sticky;
  top: 0;
  z-index: 100;
}

.site-header .container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 60px;
}

.logo {
  font-size: 1.25rem;
  font-weight: 800;
  color: var(--text) !important;
  letter-spacing: -0.5px;
}

.logo:hover {
  color: var(--primary) !important;
}

nav {
  display: flex;
  align-items: center;
  gap: 1.5rem;
}

nav a {
  font-size: 0.9375rem;
  font-weight: 500;
  color: var(--text-secondary) !important;
  transition: color 0.2s ease;
}

nav a:hover,
nav a.router-link-active {
  color: var(--primary) !important;
}

main.container {
  flex: 1;
  padding-top: 2rem;
  padding-bottom: 3rem;
}
</style>
