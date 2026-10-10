<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { dishApi, windowApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { money } from '@/utils/format'

const auth = useAuthStore()
const toast = useToast()

const CATEGORY_OPTIONS = ['主食', '荤菜', '素菜', '汤品', '饮品', '其他']

const dishes = ref([])
const loading = ref(true)
const saving = ref(false)
/** 商家未绑定窗口时后端返回 403，这里单独提示而不是展示空列表 */
const loadError = ref('')

/** 菜品新增 / 编辑弹窗 */
const formVisible = ref(false)
const editingId = ref(null)
const form = reactive({
  dishName: '',
  category: '主食',
  price: '',
  stock: '',
  description: '',
  status: 1
})

/** 库存调整弹窗 */
const stockTarget = ref(null)
const stockValue = ref('')

/** 营业状态卡片 */
const myWindow = ref(null)
const windowLoading = ref(false)

const isEditing = computed(() => editingId.value !== null)
const title = computed(() => (isEditing.value ? '编辑菜品' : '新增菜品'))

async function loadDishes() {
  loading.value = true
  loadError.value = ''
  try {
    const list = await dishApi.mine()
    dishes.value = Array.isArray(list) ? list : []
  } catch (error) {
    dishes.value = []
    // 403 通常意味着商家账号还没有绑定窗口
    if (error?.code === 403) {
      loadError.value = error.message || '当前商家账号未绑定任何食堂窗口，请联系管理员'
    } else {
      loadError.value = '菜品列表加载失败，请稍后重试'
    }
  } finally {
    loading.value = false
  }
}

/** 通过公共窗口列表找到当前商家绑定的窗口，用于营业状态开关 */
async function loadMyWindow() {
  windowLoading.value = true
  const windowId = auth.user?.windowId
  try {
    if (!windowId) {
      myWindow.value = null
      return
    }
    const list = await windowApi.list({})
    const all = Array.isArray(list) ? list : []
    let hit = all.find((item) => item.id === windowId) || null
    if (!hit) {
      // 窗口已停用时会从公共列表中消失，这里再按 id 单独查询一次
      try {
        hit = await windowApi.detail(windowId)
      } catch {
        hit = null
      }
    }
    myWindow.value = hit
  } catch {
    // 窗口信息只是辅助展示，失败时给出提示但不阻塞菜品维护
    myWindow.value = null
    toast.warn('窗口信息加载失败，暂时无法查看营业状态')
  } finally {
    windowLoading.value = false
  }
}

function resetForm() {
  editingId.value = null
  form.dishName = ''
  form.category = '主食'
  form.price = ''
  form.stock = ''
  form.description = ''
  form.status = 1
}

function openCreate() {
  resetForm()
  formVisible.value = true
}

function openEdit(dish) {
  editingId.value = dish.id
  form.dishName = dish.dishName || ''
  form.category = dish.category || '主食'
  form.price = dish.price ?? ''
  form.stock = dish.stock ?? ''
  form.description = dish.description || ''
  form.status = dish.status ?? 1
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  resetForm()
}

async function saveDish() {
  if (!form.dishName.trim()) {
    toast.warn('请填写菜品名称')
    return
  }
  const price = Number(form.price)
  const stock = Number(form.stock)
  if (!Number.isFinite(price) || price <= 0) {
    toast.warn('请填写大于 0 的单价')
    return
  }
  if (!Number.isInteger(stock) || stock < 0) {
    toast.warn('库存必须是不小于 0 的整数')
    return
  }
  saving.value = true
  const payload = {
    dishName: form.dishName.trim(),
    category: form.category,
    price,
    stock,
    description: form.description.trim(),
    status: form.status
  }
  try {
    if (isEditing.value) {
      await dishApi.update(editingId.value, payload)
      toast.success('菜品已更新')
    } else {
      await dishApi.create(payload)
      toast.success('菜品已新增')
    }
    closeForm()
    loadDishes()
  } catch {
    // 失败提示由拦截器处理
  } finally {
    saving.value = false
  }
}

/** 上架 / 下架：1 在售，0 下架 */
async function toggleStatus(dish) {
  const next = dish.status === 1 ? 0 : 1
  try {
    await dishApi.changeStatus(dish.id, next)
    toast.success(next === 1 ? `「${dish.dishName}」已上架` : `「${dish.dishName}」已下架`)
    loadDishes()
  } catch {
    // 失败提示由拦截器处理
  }
}

function openStock(dish) {
  stockTarget.value = dish
  stockValue.value = String(dish.stock ?? 0)
}

function closeStock() {
  stockTarget.value = null
  stockValue.value = ''
}

async function saveStock() {
  const dish = stockTarget.value
  if (!dish) return
  const stock = Number(stockValue.value)
  if (!Number.isInteger(stock) || stock < 0) {
    toast.warn('库存必须是不小于 0 的整数')
    return
  }
  saving.value = true
  try {
    await dishApi.updateStock(dish.id, stock)
    toast.success(`「${dish.dishName}」库存已调整为 ${stock}`)
    closeStock()
    loadDishes()
  } catch {
    // 失败提示由拦截器处理
  } finally {
    saving.value = false
  }
}

/** 切换窗口营业状态：1 营业中，0 已打烊 */
async function toggleBusiness() {
  const target = myWindow.value
  if (!target) return
  const next = target.businessStatus === 1 ? 0 : 1
  windowLoading.value = true
  try {
    await windowApi.changeBusinessStatus(target.id, next)
    target.businessStatus = next
    toast.success(next === 1 ? '窗口已开始营业' : '窗口已打烊')
  } catch {
    // 失败提示由拦截器处理
  } finally {
    windowLoading.value = false
  }
}

onMounted(() => {
  loadDishes()
  loadMyWindow()
})
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>菜品维护</h2>
      <span class="sub">上架中的菜品才会出现在师生用户的选餐页面</span>
    </div>

    <div class="card window-card">
      <div class="row-between">
        <div>
          <div class="card-title no-border">我的窗口</div>
          <div v-if="windowLoading && !myWindow" class="muted">正在加载窗口信息…</div>
          <div v-else-if="!myWindow" class="muted">
            未找到绑定的窗口，请联系管理员在「账号与角色」中绑定。
          </div>
          <div v-else>
            <div>
              <b>{{ myWindow.windowName }}</b>
              <span class="tag" :class="myWindow.businessStatus === 1 ? 'tag-3' : 'tag-4'">
                {{ myWindow.businessStatus === 1 ? '营业中' : '已打烊' }}
              </span>
              <span v-if="myWindow.status === 0" class="tag tag-5">已停用</span>
            </div>
            <div class="muted">{{ myWindow.location || '未填写位置' }}</div>
          </div>
        </div>
        <button
          v-if="myWindow"
          class="btn"
          :class="myWindow.businessStatus === 1 ? 'btn-plain' : 'btn-success'"
          type="button"
          :disabled="windowLoading || myWindow.status === 0"
          @click="toggleBusiness"
        >
          {{ myWindow.businessStatus === 1 ? '打烊休息' : '开始营业' }}
        </button>
      </div>
    </div>

    <div class="row toolbar">
      <button class="btn" type="button" @click="openCreate">新增菜品</button>
      <button class="btn btn-plain btn-sm" type="button" :disabled="loading" @click="loadDishes">
        刷新
      </button>
      <span class="muted">共 {{ dishes.length }} 个菜品</span>
    </div>

    <div v-if="loadError" class="alert alert-warn">{{ loadError }}</div>

    <div v-if="loading" class="empty">正在加载菜品…</div>

    <div v-else-if="dishes.length === 0 && !loadError" class="empty">
      还没有菜品，点击「新增菜品」创建第一个菜品吧。
    </div>

    <div v-else-if="dishes.length > 0" class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            <th>菜品</th>
            <th>分类</th>
            <th>单价</th>
            <th>库存</th>
            <th>状态</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="dish in dishes" :key="dish.id">
            <td>
              <div>{{ dish.dishName }}</div>
              <div class="muted desc">{{ dish.description || '暂无描述' }}</div>
            </td>
            <td>{{ dish.category || '未分类' }}</td>
            <td class="money">{{ money(dish.price) }}</td>
            <td :class="{ 'low-stock': dish.stock <= 5 }">{{ dish.stock }}</td>
            <td>
              <span class="tag" :class="dish.status === 1 ? 'tag-3' : 'tag-4'">
                {{ dish.status === 1 ? '在售' : '已下架' }}
              </span>
            </td>
            <td class="text-right">
              <div class="row actions">
                <button class="btn btn-plain btn-sm" type="button" @click="openEdit(dish)">编辑</button>
                <button
                  class="btn btn-plain btn-sm"
                  type="button"
                  :class="dish.status === 1 ? 'btn-danger' : 'btn-success'"
                  @click="toggleStatus(dish)"
                >
                  {{ dish.status === 1 ? '下架' : '上架' }}
                </button>
                <button class="btn btn-plain btn-sm" type="button" @click="openStock(dish)">调整库存</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal :title="title" :visible="formVisible" @close="closeForm">
      <div class="form-item">
        <label class="required">菜品名称</label>
        <input v-model.trim="form.dishName" type="text" maxlength="40" placeholder="例如：红烧牛肉面" />
      </div>
      <div class="form-item">
        <label class="required">分类</label>
        <select v-model="form.category">
          <option v-for="item in CATEGORY_OPTIONS" :key="item" :value="item">{{ item }}</option>
        </select>
      </div>
      <div class="form-item">
        <label class="required">单价（元）</label>
        <input v-model="form.price" type="number" min="0.01" step="0.01" placeholder="例如：12.50" />
      </div>
      <div class="form-item">
        <label class="required">库存（份）</label>
        <input v-model="form.stock" type="number" min="0" step="1" placeholder="例如：50" />
      </div>
      <div class="form-item">
        <label>描述</label>
        <textarea v-model="form.description" maxlength="200" placeholder="菜品口味、份量等说明" />
      </div>
      <div class="form-item">
        <label>状态</label>
        <select v-model.number="form.status">
          <option :value="1">在售</option>
          <option :value="0">下架</option>
        </select>
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeForm">取消</button>
        <button class="btn" type="button" :disabled="saving" @click="saveDish">
          {{ saving ? '保存中…' : '保存' }}
        </button>
      </template>
    </BaseModal>

    <BaseModal title="调整库存" :visible="stockTarget !== null" @close="closeStock">
      <p>
        菜品「{{ stockTarget?.dishName }}」当前库存 {{ stockTarget?.stock }} 份。
      </p>
      <div class="form-item">
        <label class="required">新的库存数量</label>
        <input v-model="stockValue" type="number" min="0" step="1" @keyup.enter="saveStock" />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeStock">取消</button>
        <button class="btn" type="button" :disabled="saving" @click="saveStock">
          {{ saving ? '保存中…' : '确认调整' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.window-card {
  margin-bottom: 16px;
}

.no-border {
  margin-bottom: 6px;
  padding-bottom: 0;
  border-bottom: none;
}

.toolbar {
  margin-bottom: 12px;
}

.desc {
  font-size: 12.5px;
}

.low-stock {
  font-weight: 600;
  color: var(--danger);
}

.actions {
  justify-content: flex-end;
  gap: 6px;
}
</style>
