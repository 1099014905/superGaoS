import { createRouter, createWebHistory } from 'vue-router'

import ArticleList from '../views/ArticleList.vue'
import ArticleDetail from '../views/ArticleDetail.vue'
import Login from '../views/Login.vue'
import AdminDashboard from '../views/AdminDashboard.vue'
import WriteArticle from '../views/WriteArticle.vue'
import SeckillList from '../views/SeckillList.vue'
import SeckillOrders from '../views/SeckillOrders.vue'

const routes = [
  {
    path: '/',
    name: 'ArticleList',
    component: ArticleList,
    meta: { title: '首页' }
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: ArticleDetail,
    meta: { title: '文章详情' }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { title: '登录' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: AdminDashboard,
    meta: { title: '管理后台', requiresAuth: true }
  },
  {
    path: '/admin/write',
    name: 'WriteArticle',
    component: WriteArticle,
    meta: { title: '写文章', requiresAuth: true }
  },
  {
    path: '/admin/write/:id',
    name: 'EditArticle',
    component: WriteArticle,
    meta: { title: '编辑文章', requiresAuth: true }
  },
  { path: '/seckill', name: 'SeckillList', component: SeckillList },
  { path: '/seckill/orders', name: 'SeckillOrders', component: SeckillOrders, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  if (to.meta.requiresAuth) {
    if (!token) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
  }

  if (to.name === 'Login' && token) {
    next({ name: 'AdminDashboard' })
    return
  }

  next()
})

export default router
