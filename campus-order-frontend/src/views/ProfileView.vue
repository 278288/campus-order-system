<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { authApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { ROLE_TEXT } from '@/utils/format'

const auth = useAuthStore()
const toast = useToast()

const loading = ref(true)
const saving = ref(false)

const profile = ref(null)
const form = reactive({ realName: '', email: '' })

const roleText = computed(() => {
  const role = profile.value?.role
  return role != null ? ROLE_TEXT[role] || profile.value?.roleName || '未知角色' : '-'
})

async function loadProfile() {
  loading.value = true
  try {
    const user = await authApi.me()
    profile.value = user
    // 服务端数据作为唯一事实来源，同时同步到本地缓存
    auth.setUser(user)
    form.realName = user?.realName || ''
    form.email = user?.email || ''
  } catch {
    // 失败时退回本地缓存的用户信息，页面依然可用
    profile.value = auth.user
    form.realName = auth.user?.realName || ''
    form.email = auth.user?.email || ''
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const user = await authApi.updateMe({
      realName: form.realName.trim(),
      email: form.email.trim()
    })
    profile.value = user
    auth.setUser(user)
    toast.success('资料已更新')
  } catch {
    // 失败提示由拦截器处理
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>个人资料</h2>
      <span class="sub">账号与角色信息由管理员维护，个人只能修改姓名与邮箱</span>
    </div>

    <div v-if="loading" class="empty">正在加载个人资料…</div>

    <div v-else class="grid grid-2">
      <div class="card">
        <div class="card-title">账号信息</div>
        <div class="info-row">
          <span class="muted">登录账号</span>
          <span class="mono">{{ profile?.username || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="muted">角色</span>
          <span>
            <span class="tag tag-plain">{{ roleText }}</span>
          </span>
        </div>
        <div class="info-row">
          <span class="muted">手机号</span>
          <span>{{ profile?.phone || '-' }}</span>
        </div>
        <div class="info-row">
          <span class="muted">账号状态</span>
          <span>
            <span class="tag" :class="profile?.status === 1 ? 'tag-3' : 'tag-5'">
              {{ profile?.status === 1 ? '正常' : '已停用' }}
            </span>
          </span>
        </div>
        <div v-if="profile?.role === 1" class="info-row">
          <span class="muted">绑定窗口</span>
          <span>{{ profile?.windowName || '未绑定窗口' }}</span>
        </div>
        <div class="info-row">
          <span class="muted">最近登录</span>
          <span>{{ profile?.lastLoginTime ? String(profile.lastLoginTime).replace('T', ' ').slice(0, 19) : '-' }}</span>
        </div>
        <div class="info-row">
          <span class="muted">注册时间</span>
          <span>{{ profile?.createTime ? String(profile.createTime).replace('T', ' ').slice(0, 19) : '-' }}</span>
        </div>
      </div>

      <div class="card">
        <div class="card-title">可编辑资料</div>
        <form @submit.prevent="save">
          <div class="form-item">
            <label>真实姓名</label>
            <input v-model.trim="form.realName" type="text" maxlength="20" placeholder="请输入真实姓名" />
          </div>
          <div class="form-item">
            <label>邮箱</label>
            <input v-model.trim="form.email" type="text" maxlength="60" placeholder="请输入邮箱" />
          </div>
          <button class="btn" type="submit" :disabled="saving">
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 0;
  font-size: 13.5px;
  border-bottom: 1px dashed var(--border);
}

.info-row:last-child {
  border-bottom: none;
}
</style>
