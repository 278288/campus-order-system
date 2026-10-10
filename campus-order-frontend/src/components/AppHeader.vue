<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToast } from '@/utils/toast'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const toast = useToast()

const navItems = computed(() => {
  if (!auth.isLoggedIn) {
    return [{ to: '/dishes', label: '选餐' }]
  }
  if (auth.isStudent) {
    return [
      { to: '/dishes', label: '选餐' },
      { to: '/cart', label: '购物车' },
      { to: '/orders', label: '我的订单' }
    ]
  }
  if (auth.isMerchant) {
    return [
      { to: '/merchant/orders', label: '接单与备餐' },
      { to: '/merchant/pickup', label: '核码取餐' },
      { to: '/merchant/dishes', label: '菜品维护' },
      { to: '/merchant/stats', label: '窗口统计' }
    ]
  }
  if (auth.isAdmin) {
    return [
      { to: '/admin/users', label: '账号与角色' },
      { to: '/admin/windows', label: '窗口管理' },
      { to: '/admin/orders', label: '全平台订单' },
      { to: '/admin/stats', label: '平台统计' }
    ]
  }
  return []
})

function logout() {
  auth.clear()
  cart.clear()
  toast.success('已退出登录')
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="app-header">
    <div class="inner">
      <router-link class="brand" to="/dishes">🍚 校园点餐系统</router-link>

      <nav class="nav">
        <router-link v-for="item in navItems" :key="item.to" :to="item.to">
          {{ item.label }}
          <span v-if="item.to === '/cart' && cart.itemCount > 0" class="badge">{{ cart.itemCount }}</span>
        </router-link>
      </nav>

      <div class="user-box">
        <template v-if="auth.isLoggedIn">
          <span>{{ auth.displayName }}</span>
          <span class="tag tag-plain">{{ auth.user.roleName }}</span>
          <router-link to="/profile" class="btn btn-plain btn-sm">个人资料</router-link>
          <button class="btn btn-plain btn-sm" type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login" class="btn btn-sm">登录 / 注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>
