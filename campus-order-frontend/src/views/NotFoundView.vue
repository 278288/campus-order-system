<script setup>
import { onMounted, ref } from 'vue'
import { menuApi } from '@/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

/** 默认入口；有菜单树时改用后端返回的第一个菜单，避免按角色硬编码跳转目标 */
const homePath = ref('/dishes')
const homeLabel = ref('去选餐')

/**
 * 从菜单树中取第一个 menuType === 2 且有 path 的节点作为 404 页的返回入口，
 * 这样不同角色的落点完全由后端菜单配置决定。
 */
function firstMenuPath(nodes) {
  if (!Array.isArray(nodes)) return null
  for (const node of nodes) {
    if (!node) continue
    if (node.menuType === 2 && node.path) {
      return { path: node.path, label: `去${node.menuName || '首页'}` }
    }
    const hit = firstMenuPath(node.children)
    if (hit) return hit
  }
  return null
}

onMounted(async () => {
  if (!auth.isLoggedIn) return
  try {
    const tree = await menuApi.myMenus()
    const hit = firstMenuPath(tree)
    if (hit) {
      homePath.value = hit.path
      homeLabel.value = hit.label
    }
  } catch {
    // 菜单加载失败时保留默认入口（401 已由拦截器处理）
  }
})
</script>

<template>
  <div class="page">
    <div class="card not-found-card">
      <div class="code">404</div>
      <div class="card-title">页面不存在</div>
      <p class="muted">
        你访问的页面可能已被移除，或者地址输入有误。请检查链接后重试。
      </p>
      <div class="row actions">
        <router-link class="btn" :to="homePath">{{ homeLabel }}</router-link>
        <router-link class="btn btn-plain" to="/dishes">去选餐</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found-card {
  max-width: 520px;
  margin: 60px auto;
  text-align: center;
}

.code {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand);
}

.not-found-card .card-title {
  margin-top: 12px;
  border-bottom: none;
  padding-bottom: 0;
}

.actions {
  justify-content: center;
  margin-top: 16px;
}
</style>
