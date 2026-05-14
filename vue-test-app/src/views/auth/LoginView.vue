<template>
  <div class="auth-bg">
    <el-card class="auth-card" shadow="hover">
      <div class="logo">DocAI 智能平台</div>
      <h2 class="title">欢迎回来</h2>
      <el-form :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large" prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="submit-btn" size="large" @click="handleLogin" :loading="loading">登录</el-button>
        <div class="footer-text">
          <router-link to="/register">没有账号？免费注册</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userApi } from '../../api/user'
import { STORAGE_KEYS } from '../../constants'

const router = useRouter()
const loading = ref(false)
const form = ref({ username: '', password: '' })

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    return ElMessage.warning('请填写完整的账号和密码')
  }
  loading.value = true
  try {
    const res = await userApi.login(form.value)

    // 保存 Token
    localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, res.data.accessToken)
    localStorage.setItem('userId', res.data.user.id)

    ElMessage.success('登录成功！')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-bg { height: 100vh; display: flex; justify-content: center; align-items: center; background: #f0f2f5; }
.auth-card { width: 400px; padding: 20px 30px; border-radius: 12px; }
.logo { text-align: center; font-size: 24px; font-weight: bold; color: #409EFF; margin-bottom: 10px; }
.title { text-align: center; font-size: 18px; color: #333; margin-bottom: 25px; }
.submit-btn { width: 100%; margin-top: 10px; font-weight: bold;}
.footer-text { text-align: center; margin-top: 15px; font-size: 14px; }
.footer-text a { color: #409EFF; text-decoration: none; }
</style>