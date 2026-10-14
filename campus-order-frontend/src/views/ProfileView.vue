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

/** user.roleCode：后端新增的角色编码，例如 ROLE_STUDENT */
const roleCode = computed(() => profile.value?.roleCode || auth.roleCode || '')

/** 权限标识列表，管理员持有的通配符 *:* 原样展示 */
const permissionList = computed(() => auth.permissions || [])

async function loadProfile() {
  loading.value = true
  try {
    // /api/users/me 现在返回 { user, roles, permissions }，user 才是用户对象
    const res = await authApi.me()
    const user = res?.user || null
    profile.value = user
    // 服务端数据作为唯一事实来源，同时同步到本地缓存与权限
    if (user) auth.setUser(user)
    auth.setAuthorities({ roles: res?.roles, permissions: res?.permissions })
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
    const res = await authApi.updateMe({
      realName: form.realName.trim(),
      email: form.email.trim()
    })
    // 兼容两种返回：{ user, roles, permissions } 或直接返回 UserVO
    const user = res?.user || res
    if (user) {
      profile.value = user
      auth.setUser(user)
    }
    auth.setAuthorities({ roles: res?.roles, permissions: res?.permissions })
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

    <template v-else>
      <div class="grid grid-2">
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
              <span v-if="roleCode" class="tag tag-plain code-tag mono">{{ roleCode }}</span>
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

      <!-- 角色编码与权限标识：让实验4 的权限数据在界面上可见 -->
      <div class="card perm-card">
        <div class="row-between">
          <div class="card-title no-border">角色与权限</div>
          <span class="muted">共 {{ permissionList.length }} 项权限标识</span>
        </div>

        <div class="perm-line">
          <span class="muted">角色编码</span>
          <span class="tag tag-2 mono">{{ roleCode || '未返回（请重新登录）' }}</span>
        </div>

        <div class="perm-line">
          <span class="muted">角色列表</span>
          <span v-if="(auth.roles || []).length === 0" class="muted">未返回角色</span>
          <span v-else class="tag-list">
            <span v-for="code in auth.roles" :key="code" class="tag tag-plain mono">{{ code }}</span>
          </span>
        </div>

        <div class="perm-line">
          <span class="muted">权限标识</span>
          <span v-if="permissionList.length === 0" class="muted">
            暂无权限数据，请重新登录以获取最新权限
          </span>
          <span v-else class="tag-list">
            <span
              v-for="perm in permissionList"
              :key="perm"
              class="tag mono"
              :class="perm === '*:*' ? 'tag-2' : 'tag-1'"
            >
              {{ perm }}
            </span>
          </span>
        </div>

        <div v-if="permissionList.includes('*:*')" class="alert alert-info perm-tip">
          当前账号为管理员，持有通配符权限 <span class="mono">*:*</span>，拥有全部操作权限。
        </div>
      </div>
    </template>
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

.code-tag {
  margin-left: 6px;
}

.perm-card {
  margin-top: 16px;
}

.no-border {
  margin-bottom: 6px;
  padding-bottom: 0;
  border-bottom: none;
}

.perm-line {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  font-size: 13.5px;
}

.perm-line > .muted:first-child {
  flex: 0 0 72px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.perm-tip {
  margin: 12px 0 0;
}
</style>
