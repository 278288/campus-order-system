<script setup>
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

/** 不同角色给出不同的返回入口，避免把商家/管理员送到师生页面 */
function homePath() {
  if (auth.isMerchant) return '/merchant/orders'
  if (auth.isAdmin) return '/admin/users'
  return '/dishes'
}

function homeLabel() {
  if (auth.isMerchant) return '去接单与备餐'
  if (auth.isAdmin) return '去账号与角色'
  return '去选餐'
}
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
        <router-link class="btn" :to="homePath()">{{ homeLabel() }}</router-link>
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
