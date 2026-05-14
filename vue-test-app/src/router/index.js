/**
 * @description 页面路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { STORAGE_KEYS } from '../constants'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/login', component: () => import('../views/auth/LoginView.vue') },
    { path: '/register', component: () => import('../views/auth/RegisterView.vue') },
    { path: '/dashboard', component: () => import('../views/dashboard/IndexView.vue') },
    { path: '/aiops', component: () => import('../views/aiops/AIOpsView.vue') },
    { path: '/editor/:id', component: () => import('../views/EditorView.vue') }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
    const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)
    if (to.path !== '/login' && to.path !== '/register' && !token) {
        next('/login')
    } else {
        next()
    }
})

export default router