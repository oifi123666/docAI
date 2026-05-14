<template>
  <div class="auth-bg">
    <el-card class="auth-card" shadow="hover">
      <div class="logo">DocAI 智能平台</div>
      <h2 class="title">创建新账号</h2>

      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">

        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="例如: 1@qq.com" size="large" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11位手机号" size="large" />
        </el-form-item>

        <el-form-item label="设置密码" prop="password">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              placeholder="必须包含大小写、数字和特殊字符"
              size="large"
              @input="checkPasswordStrength"
          />

          <!-- 实时校验清单面板 -->
          <div class="password-checklist">
            <div :class="['check-item', strength.length ? 'met' : '']">
              <el-icon><CircleCheck v-if="strength.length" /><CircleClose v-else /></el-icon>
              至少 8 个字符
            </div>
            <div :class="['check-item', strength.upper ? 'met' : '']">
              <el-icon><CircleCheck v-if="strength.upper" /><CircleClose v-else /></el-icon>
              包含大写字母
            </div>
            <div :class="['check-item', strength.lower ? 'met' : '']">
              <el-icon><CircleCheck v-if="strength.lower" /><CircleClose v-else /></el-icon>
              包含小写字母
            </div>
            <div :class="['check-item', strength.mixed ? 'met' : '']">
              <el-icon><CircleCheck v-if="strength.mixed" /><CircleClose v-else /></el-icon>
              包含数字和特殊符号 (@$!%*?&)
            </div>
          </div>
        </el-form-item>

        <el-button
            type="primary"
            class="submit-btn"
            size="large"
            @click="handleRegister"
            :loading="loading"
            :disabled="!isPasswordValid"
        >
          立即注册
        </el-button>

        <div class="footer-text">
          <router-link to="/login">已有账号？返回登录</router-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { REGEX } from '../../constants'
import { userApi } from '../../api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = ref({ username: '', email: '', phone: '', password: '' })

// 密码强度实时状态
const strength = reactive({
  length: false,
  upper: false,
  lower: false,
  mixed: false
})

// 实时检查函数
const checkPasswordStrength = () => {
  const pwd = form.value.password
  strength.length = pwd.length >= 8
  strength.upper = REGEX.HAS_UPPER.test(pwd)
  strength.lower = REGEX.HAS_LOWER.test(pwd)
  strength.mixed = REGEX.HAS_NUMBER.test(pwd) && REGEX.HAS_SPECIAL.test(pwd)
}

// 计算属性：密码是否完全合格
const isPasswordValid = computed(() => {
  return strength.length && strength.upper && strength.lower && strength.mixed
})

const rules = {
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱或手机号', trigger: 'blur' }]
  // 密码校验逻辑现在通过 strength 面板和 isPasswordValid 按钮禁用态来控制，更直观
}

const handleRegister = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userApi.register(form.value)
      ElMessage.success('注册成功，请登录！')
      router.push('/login')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-bg { height: 100vh; display: flex; justify-content: center; align-items: center; background: #f4f7f9; }
.auth-card { width: 420px; padding: 20px 30px; border-radius: 12px; }
.logo { text-align: center; font-size: 24px; font-weight: bold; color: #409EFF; margin-bottom: 10px; }
.title { text-align: center; font-size: 16px; color: #666; margin-bottom: 30px; }

/* 密码清单样式 */
.password-checklist {
  margin-top: 12px;
  background: #f8f9fa;
  padding: 12px;
  border-radius: 8px;
}
.check-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #909399; /* 默认灰色：未达成 */
  margin-bottom: 4px;
  transition: all 0.3s;
}
.check-item.met {
  color: #67C23A; /* 达成后变绿色 */
  font-weight: 500;
}
.check-item .el-icon { font-size: 14px; }

.submit-btn { width: 100%; margin-top: 20px; }
.footer-text { text-align: center; margin-top: 20px; font-size: 14px; }
</style>