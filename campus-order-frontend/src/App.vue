<script setup>
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { useToast } from '@/utils/toast'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'

const toast = useToast()
const auth = useAuthStore()
const cart = useCartStore()

onMounted(async () => {
  // 购物车保存在浏览器本地，刷新后恢复
  cart.restore?.()
  // 已有令牌时用后端数据刷新用户信息，确保角色与状态是最新的
  if (auth.token) {
    try {
      const { authApi } = await import('@/api')
      const user = await authApi.me()
      auth.setUser(user)
    } catch {
      // 401 已由 axios 拦截器处理：清理会话并跳转登录页
    }
  }
})
</script>

<template>
  <AppHeader />
  <router-view />

  <div class="toast-container">
    <div v-for="item in toast.state.toasts" :key="item.id" class="toast" :class="`toast-${item.type}`">
      {{ item.message }}
    </div>
  </div>
</template>
