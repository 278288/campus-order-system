<script setup>
import { onMounted, reactive, ref } from 'vue'
import { windowApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const toast = useToast()

const windows = ref([])
const loading = ref(true)
const saving = ref(false)

/** 新增 / 编辑窗口弹窗 */
const formVisible = ref(false)
const editingId = ref(null)
const form = reactive({ windowName: '', location: '', businessStatus: 1 })

const isEditing = ref(false)

async function loadWindows() {
  loading.value = true
  try {
    // status 传 null 表示查询全部记录（含已停用窗口）
    const list = await windowApi.list({ status: null })
    windows.value = Array.isArray(list) ? list : []
  } catch {
    windows.value = []
  } finally {
    loading.value = false
  }
}

function resetForm() {
  editingId.value = null
  isEditing.value = false
  form.windowName = ''
  form.location = ''
  form.businessStatus = 1
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(item) {
  editingId.value = item.id
  isEditing.value = true
  form.windowName = item.windowName || ''
  form.location = item.location || ''
  form.businessStatus = item.businessStatus ?? 1
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  resetForm()
}

async function saveWindow() {
  if (!form.windowName.trim()) {
    toast.warn('请填写窗口名称')
    return
  }
  saving.value = true
  const payload = {
    windowName: form.windowName.trim(),
    location: form.location.trim(),
    businessStatus: form.businessStatus
  }
  try {
    if (isEditing.value) {
      await windowApi.update(editingId.value, payload)
      toast.success('窗口信息已更新')
    } else {
      await windowApi.create(payload)
      toast.success('窗口已新增')
    }
    closeForm()
    loadWindows()
  } catch {
    // 失败提示由拦截器处理
  } finally {
    saving.value = false
  }
}

/** 营业 / 打烊切换：只影响顾客能否下单，不影响历史数据 */
async function toggleBusiness(item) {
  const next = item.businessStatus === 1 ? 0 : 1
  try {
    await windowApi.changeBusinessStatus(item.id, next)
    toast.success(next === 1 ? `「${item.windowName}」已开始营业` : `「${item.windowName}」已打烊`)
    loadWindows()
  } catch {
    // 失败提示由拦截器处理
  }
}

/**
 * 停用 / 启用窗口。
 * 停用只保留历史数据关联，不做物理删除，因此这里用一次简单确认提示风险。
 */
async function toggleStatus(item) {
  const next = item.status === 1 ? 0 : 1
  const tip =
    next === 0
      ? `确认停用窗口「${item.windowName}」吗？\n\n停用只保留历史数据关联，不做物理删除；停用后该窗口的商家无法备餐，顾客也不能再下单。`
      : `确认重新启用窗口「${item.windowName}」吗？`
  if (!window.confirm(tip)) return
  try {
    await windowApi.changeStatus(item.id, next)
    toast.success(next === 1 ? '窗口已启用' : '窗口已停用')
    loadWindows()
  } catch {
    // 失败提示由拦截器处理
  }
}

onMounted(loadWindows)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>窗口管理</h2>
      <span class="sub">共 {{ windows.length }} 个窗口</span>
    </div>

    <div class="row toolbar">
      <!-- 新增窗口需要 window:add 权限 -->
      <button v-if="auth.hasPermission('window:add')" class="btn" type="button" @click="openCreate">
        新增窗口
      </button>
      <button class="btn btn-plain btn-sm" type="button" :disabled="loading" @click="loadWindows">
        刷新
      </button>
    </div>

    <div v-if="loading" class="empty">正在加载窗口列表…</div>

    <div v-else-if="windows.length === 0" class="empty">
      还没有食堂窗口，点击「新增窗口」创建第一个窗口吧。
    </div>

    <div v-else class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>窗口名称</th>
            <th>位置</th>
            <th>营业状态</th>
            <th>记录状态</th>
            <th>创建时间</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in windows" :key="item.id">
            <td>{{ item.windowName }}</td>
            <td>{{ item.location || '-' }}</td>
            <td>
              <span class="tag" :class="item.businessStatus === 1 ? 'tag-3' : 'tag-4'">
                {{ item.businessStatus === 1 ? '营业中' : '已打烊' }}
              </span>
            </td>
            <td>
              <span class="tag" :class="item.status === 1 ? 'tag-ok' : 'tag-danger'">
                {{ item.status === 1 ? '启用' : '已停用' }}
              </span>
            </td>
            <td>{{ formatDateTime(item.createTime) }}</td>
            <td class="text-right">
              <div class="row actions">
                <!-- 按钮按权限标识控制显隐：编辑 window:edit、营业 window:business、启停 window:status -->
                <button
                  v-if="auth.hasPermission('window:edit')"
                  class="btn btn-plain btn-sm"
                  type="button"
                  @click="openEdit(item)"
                >
                  编辑
                </button>
                <button
                  v-if="auth.hasPermission('window:business')"
                  class="btn btn-plain btn-sm"
                  type="button"
                  @click="toggleBusiness(item)"
                >
                  {{ item.businessStatus === 1 ? '打烊' : '营业' }}
                </button>
                <button
                  v-if="auth.hasPermission('window:status')"
                  class="btn btn-sm"
                  :class="item.status === 1 ? 'btn-danger' : 'btn-success'"
                  type="button"
                  @click="toggleStatus(item)"
                >
                  {{ item.status === 1 ? '停用' : '启用' }}
                </button>
                <span
                  v-if="
                    !auth.hasAnyPermission('window:edit', 'window:business', 'window:status')
                  "
                  class="muted"
                >
                  -
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal :title="isEditing ? '编辑窗口' : '新增窗口'" :visible="formVisible" @close="closeForm">
      <div class="form-item">
        <label class="required">窗口名称</label>
        <input v-model.trim="form.windowName" type="text" maxlength="40" placeholder="例如：一食堂 · 川湘风味" />
      </div>
      <div class="form-item">
        <label>位置</label>
        <input v-model.trim="form.location" type="text" maxlength="60" placeholder="例如：一食堂二楼 12 号窗口" />
      </div>
      <div class="form-item">
        <label class="required">营业状态</label>
        <select v-model.number="form.businessStatus">
          <option :value="1">营业中</option>
          <option :value="0">已打烊</option>
        </select>
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeForm">取消</button>
        <button class="btn" type="button" :disabled="saving" @click="saveWindow">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.toolbar {
  margin-bottom: 12px;
}

.actions {
  justify-content: flex-end;
  gap: 6px;
}
</style>
