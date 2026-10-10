<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const toast = useToast()

/** 当前页签：login 登录 / register 注册 */
const tab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

/** 课程演示账号，方便答辩时快速切换角色 */
const demoAccounts = [
  { username: 'admin', label: '管理员' },
  { username: 'canteen1', label: '一食堂商家' },
  { username: 'canteen2', label: '二食堂商家' },
  { username: 'student1', label: '学生一' },
  { username: 'student2', label: '学生二' }
]

/** 填演示账号：切到登录页签并把账号密码写入表单 */
function fillDemo(username) {
  tab.value = 'login'
  loginForm.username = username
  loginForm.password = '123456'
  toast.info(`已填入演示账号 ${username}，密码 123456`)
}

/** 登录成功后按角色进入对应首页，未指定 redirect 时师生用户去选餐页 */
function afterLogin() {
  const redirect = route.query.redirect
  if (redirect) {
    router.push(String(redirect))
    return
  }
  router.push('/dishes')
}

async function submitLogin() {
  if (!loginForm.username || !loginForm.password) {
    toast.warn('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })
    auth.setSession(res)
    cart.restore?.()
    toast.success(`欢迎回来，${res.user?.realName || res.user?.username}`)
    afterLogin()
  } catch {
    // 失败提示已由 axios 响应拦截器统一弹出，这里只需结束 loading
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  const form = registerForm
  if (!form.username || !form.password || !form.confirmPassword) {
    toast.warn('请填写账号与密码')
    return
  }
  if (form.password.length < 6) {
    toast.warn('密码长度至少 6 位')
    return
  }
  // 两次密码一致性只能在前端校验，后端不会收到确认密码
  if (form.password !== form.confirmPassword) {
    toast.error('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const res = await authApi.register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      phone: form.phone.trim(),
      email: form.email.trim()
    })
    // 后端注册接口直接返回登录结果，注册后即为登录态
    auth.setSession(res)
    cart.restore?.()
    toast.success('注册成功，已自动登录')
    afterLogin()
  } catch {
    // 同上：错误提示由拦截器负责
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1>🍚 校园点餐系统</h1>
      <p class="sub">在线选餐 · 窗口备餐 · 核码取餐</p>

      <div class="tabs">
        <button type="button" :class="{ active: tab === 'login' }" @click="tab = 'login'">登录</button>
        <button type="button" :class="{ active: tab === 'register' }" @click="tab = 'register'">
          注册
        </button>
      </div>

      <form v-if="tab === 'login'" @submit.prevent="submitLogin">
        <div class="form-item">
          <label class="required">账号</label>
          <input v-model.trim="loginForm.username" type="text" placeholder="请输入账号" autocomplete="username" />
        </div>
        <div class="form-item">
          <label class="required">密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>
        <button class="btn btn-block" type="submit" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>

      <form v-else @submit.prevent="submitRegister">
        <div class="form-item">
          <label class="required">账号</label>
          <input v-model.trim="registerForm.username" type="text" placeholder="4-20 位字母或数字" />
        </div>
        <div class="form-item">
          <label class="required">密码</label>
          <input v-model="registerForm.password" type="password" placeholder="至少 6 位" />
        </div>
        <div class="form-item">
          <label class="required">确认密码</label>
          <input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" />
        </div>
        <div class="form-item">
          <label>真实姓名</label>
          <input v-model.trim="registerForm.realName" type="text" placeholder="选填" />
        </div>
        <div class="form-item">
          <label>手机号</label>
          <input v-model.trim="registerForm.phone" type="text" placeholder="选填" />
        </div>
        <div class="form-item">
          <label>邮箱</label>
          <input v-model.trim="registerForm.email" type="text" placeholder="选填" />
        </div>
        <button class="btn btn-block" type="submit" :disabled="loading">
          {{ loading ? '注册中…' : '注 册' }}
        </button>
      </form>

      <div class="alert alert-info demo-box">
        <div>演示账号（密码统一为 123456），点击即可填入：</div>
        <div class="row demo-row">
          <button
            v-for="item in demoAccounts"
            :key="item.username"
            class="btn btn-ghost btn-sm"
            type="button"
            @click="fillDemo(item.username)"
          >
            {{ item.username }}（{{ item.label }}）
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.demo-box {
  margin: 18px 0 0;
}

.demo-row {
  margin-top: 8px;
}
</style>
