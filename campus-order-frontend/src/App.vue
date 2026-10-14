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
  // 已有令牌时用后端数据刷新用户信息与权限，确保角色、状态、权限都是最新的
  if (auth.token) {
    try {
      const { authApi } = await import('@/api')
      // /api/users/me 现在返回 { user, roles, permissions }，不再是 UserVO 本身
      const res = await authApi.me()
      if (res?.user) {
        auth.setUser(res.user)
      }
      auth.setAuthorities({ roles: res?.roles, permissions: res?.permissions })
    } catch {
      // 401 由 axios 拦截器清理会话并跳转登录页，这里静默处理即可
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
