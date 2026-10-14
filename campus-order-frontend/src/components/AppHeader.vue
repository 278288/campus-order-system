<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { authApi, menuApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToast } from '@/utils/toast'

const router = useRouter()
const auth = useAuthStore()
const cart = useCartStore()
const toast = useToast()

/** 导航项改为由后端菜单树驱动，前端不再按角色硬编码 */
const menus = ref([])

/**
 * 把菜单树拍平成可点击项：
 * 只取 menuType === 2（菜单）且带 path 的节点，目录（menuType 1）只用于分组、不产生链接。
 */
function flattenMenus(nodes, acc = []) {
  if (!Array.isArray(nodes)) return acc
  nodes.forEach((node) => {
    if (!node) return
    if (node.menuType === 2 && node.path) {
      acc.push({ path: node.path, label: node.menuName || node.path })
    }
    if (Array.isArray(node.children) && node.children.length > 0) {
      flattenMenus(node.children, acc)
    }
  })
  return acc
}

async function loadMenus() {
  if (!auth.token) {
    menus.value = []
    return
  }
  try {
    const tree = await menuApi.myMenus()
    menus.value = flattenMenus(tree)
  } catch {
    // 菜单加载失败不影响页面渲染（401 已由拦截器清理会话）
    menus.value = []
  }
}

/**
 * 退出登录：先调用后端 /api/auth/logout 让令牌立即失效，
 * 无论成功与否都要清空本地会话并回到登录页，避免用户卡在已登录态。
 */
async function logout() {
  try {
    await authApi.logout()
  } catch {
    // 忽略退出接口失败（例如令牌已失效），继续清理本地会话
  }
  auth.clear()
  cart.clear()
  menus.value = []
  toast.success('已退出登录')
  router.push({ name: 'login' })
}

// 登录、退出或切换账号时重新拉取菜单树
watch(
  () => auth.token,
  () => {
    loadMenus()
  }
)

// 页面刷新时本地已有令牌，挂载后立即拉取一次菜单
onMounted(loadMenus)
</script>

<template>
  <header class="app-header">
    <div class="inner">
      <router-link class="brand" to="/dishes">🍚 校园点餐系统</router-link>

      <nav class="nav">
        <router-link v-for="item in menus" :key="item.path" :to="item.path">
          {{ item.label }}
          <span v-if="item.path === '/cart' && cart.itemCount > 0" class="badge">{{ cart.itemCount }}</span>
        </router-link>
      </nav>

      <div class="user-box">
        <template v-if="auth.isLoggedIn">
          <span>{{ auth.displayName }}</span>
          <span class="tag tag-plain">{{ auth.user.roleName || auth.roleCode }}</span>
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
