<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { authApi, windowApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { formatDateTime, ROLE_OPTIONS, ROLE_TEXT } from '@/utils/format'

const auth = useAuthStore()
const toast = useToast()

const users = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const windows = ref([])

const query = reactive({ keyword: '', role: '' })

/** 角色维护弹窗 */
const target = ref(null)
const form = reactive({ role: 0, windowId: 0, status: 1 })
const saving = ref(false)

async function loadWindows() {
  try {
    const list = await windowApi.list({})
    windows.value = Array.isArray(list) ? list : []
  } catch {
    windows.value = []
  }
}

async function loadUsers() {
  loading.value = true
  try {
    const res = await authApi.pageUsers({
      keyword: query.keyword || undefined,
      role: query.role === '' ? undefined : query.role,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    users.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    users.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  loadUsers()
}

function resetFilters() {
  query.keyword = ''
  query.role = ''
  search()
}

function openMaintain(user) {
  target.value = user
  form.role = user.role ?? 0
  form.windowId = user.windowId ?? 0
  form.status = user.status ?? 1
}

function closeMaintain() {
  target.value = null
}

async function saveMaintain() {
  if (!target.value) return
  if (form.role === 1 && !form.windowId) {
    toast.warn('商家角色必须绑定一个食堂窗口')
    return
  }
  saving.value = true
  try {
    // 后端要求 windowId 必填：商家传具体窗口，其他角色统一传 0
    await authApi.assignRole(target.value.id, {
      role: form.role,
      windowId: form.role === 1 ? form.windowId : 0,
      status: form.status
    })
    toast.success(`账号 ${target.value.username} 的角色信息已更新`)
    closeMaintain()
    loadUsers()
  } catch {
    // 失败提示由拦截器处理（例如不能停用最后一个管理员）
  } finally {
    saving.value = false
  }
}

/** 弹出窗口下拉中展示绑定的窗口名称 */
function windowName(id) {
  const hit = windows.value.find((item) => item.id === id)
  return hit ? hit.windowName : id ? `#${id}` : '-'
}

onMounted(() => {
  loadWindows()
  loadUsers()
})

watch([pageNum, pageSize], loadUsers)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>账号与角色</h2>
      <span class="sub">共 {{ total }} 个账号</span>
    </div>

    <div class="alert alert-warn">
      维护账号时请遵守以下规则：商家角色必须绑定食堂窗口；不能停用系统中最后一个可用管理员；
      不能停用当前登录的管理员账号。违规操作会被后端拒绝并给出提示。
    </div>

    <div class="card filter-card">
      <div class="row">
        <div class="filter-item filter-grow">
          <label>关键字</label>
          <input
            v-model.trim="query.keyword"
            type="search"
            placeholder="按账号 / 姓名 / 手机号搜索"
            @keyup.enter="search"
          />
        </div>
        <div class="filter-item">
          <label>角色</label>
          <select v-model="query.role" @change="search">
            <option value="">全部角色</option>
            <option v-for="item in ROLE_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </div>
        <div class="filter-item filter-btns">
          <button class="btn" type="button" @click="search">搜索</button>
          <button class="btn btn-plain" type="button" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="empty">正在加载账号列表…</div>

    <div v-else-if="users.length === 0" class="empty">没有符合条件的账号。</div>

    <template v-else>
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>账号</th>
              <th>姓名</th>
              <th>手机号</th>
              <th>角色</th>
              <th>绑定窗口</th>
              <th>状态</th>
              <th>最近登录</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td class="mono">{{ user.username }}</td>
              <td>{{ user.realName || '-' }}</td>
              <td>{{ user.phone || '-' }}</td>
              <td>{{ ROLE_TEXT[user.role] || user.roleName || '-' }}</td>
              <td>{{ user.role === 1 ? windowName(user.windowId) : '-' }}</td>
              <td>
                <span class="tag" :class="user.status === 1 ? 'tag-3' : 'tag-5'">
                  {{ user.status === 1 ? '正常' : '已停用' }}
                </span>
              </td>
              <td>{{ formatDateTime(user.lastLoginTime) }}</td>
              <td class="text-right">
                <button class="btn btn-plain btn-sm" type="button" @click="openMaintain(user)">
                  维护
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </template>

    <BaseModal title="维护账号角色" :visible="target !== null" @close="closeMaintain">
      <div class="alert alert-info">
        账号 <span class="mono">{{ target?.username }}</span>
        （{{ target?.realName || '未填写姓名' }}）
      </div>
      <div class="form-item">
        <label class="required">角色</label>
        <select v-model.number="form.role">
          <option v-for="item in ROLE_OPTIONS" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </div>
      <div class="form-item">
        <label :class="{ required: form.role === 1 }">绑定窗口</label>
        <select v-model.number="form.windowId" :disabled="form.role !== 1">
          <!-- 只有商家角色需要绑定窗口，其他角色提交时统一发送 windowId = 0 -->
          <option :value="0">{{ form.role === 1 ? '请选择食堂窗口' : '该角色无需绑定窗口' }}</option>
          <option v-for="item in windows" :key="item.id" :value="item.id">
            {{ item.windowName }}（{{ item.businessStatus === 1 ? '营业中' : '已打烊' }}）
          </option>
        </select>
      </div>
      <div class="form-item">
        <label class="required">账号状态</label>
        <select v-model.number="form.status">
          <option :value="1">正常</option>
          <option :value="0">停用</option>
        </select>
      </div>
      <div v-if="target?.id === auth.user?.id" class="alert alert-warn">
        当前操作的正是你自己的账号，停用后将立即无法登录。
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeMaintain">取消</button>
        <button class="btn" type="button" :disabled="saving" @click="saveMaintain">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.filter-item {
  flex: 1 1 180px;
  min-width: 160px;
}

.filter-item > label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: var(--text-muted);
}

.filter-grow {
  flex: 2 1 240px;
}

.filter-btns {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}
</style>
