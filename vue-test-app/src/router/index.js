/**
 * @description 页面路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { STORAGE_KEYS } from '../constants'

const routes = [
    { path: '/', redirect: '/login' },
    { path: '/index.html', redirect: '/login' },
    { path: '/login', component: () => import('../views/auth/LoginView.vue') },
    { path: '/register', component: () => import('../views/auth/RegisterView.vue') },
    { path: '/dashboard', component: () => import('../views/dashboard/IndexView.vue') },
    { path: '/ppt-runtime', name: 'pptRuntime', component: () => import('../views/dashboard/PptRuntime.vue')},
    { path: '/aiops', component: () => import('../views/aiops/AIOpsView.vue') },
    { path: '/editor/:id', component: () => import('../views/editor/EditorView.vue') },
    { path: '/agent-workbench', component: () => import('../views/agent/AgentWorkbench.vue') }
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
