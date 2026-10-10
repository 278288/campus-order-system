<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { publicApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToast } from '@/utils/toast'
import { money } from '@/utils/format'

const auth = useAuthStore()
const cart = useCartStore()
const toast = useToast()

const windows = ref([])
const categories = ref([])
const dishes = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const query = reactive({
  windowId: '',
  keyword: '',
  category: ''
})

/** 待确认换窗口的菜品：购物车里已有其他窗口的菜品时使用 */
const pendingDish = ref(null)

const showCartBar = computed(() => cart.itemCount > 0)
const estimated = computed(() => money(cart.totalAmount))

/** 已登录但角色不是师生用户时，浏览接口虽然开放，但下单只有师生用户能做 */
const canOrder = computed(() => !auth.isLoggedIn || auth.isStudent)

async function loadWindows() {
  try {
    const list = await publicApi.windows({})
    windows.value = Array.isArray(list) ? list : []
  } catch {
    windows.value = []
  }
}

async function loadCategories() {
  try {
    const list = await publicApi.categories()
    categories.value = Array.isArray(list) ? list : []
  } catch {
    categories.value = []
  }
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await publicApi.dishes({
      windowId: query.windowId === '' ? undefined : query.windowId,
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    dishes.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    // 错误提示由拦截器统一处理，这里保证页面不会因为异常而空白
    dishes.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 条件变化时回到第一页，避免停留在越界页码上 */
function resetAndLoad() {
  pageNum.value = 1
  loadDishes()
}

function search() {
  resetAndLoad()
}

function resetFilters() {
  query.windowId = ''
  query.keyword = ''
  query.category = ''
  resetAndLoad()
}

/** 不可购买的原因文案：售罄 / 已下架 / 窗口打烊 */
function disabledReason(dish) {
  if (dish.status === 0) return '已下架'
  if (dish.windowBusinessStatus === 0) return '窗口打烊'
  if (!dish.stock || dish.stock <= 0) return '售罄'
  return '暂不可购'
}

function addToCart(dish) {
  if (!auth.isLoggedIn) {
    toast.warn('请先登录后再加入购物车')
    return
  }
  if (!auth.isStudent) {
    toast.warn('只有师生用户可以下单，请使用师生账号')
    return
  }
  const res = cart.add(dish, 1)
  if (res.ok) {
    toast.success(res.message)
    return
  }
  if (res.needClear) {
    // 换窗口：先弹窗确认清空，再由用户决定
    pendingDish.value = dish
    return
  }
  toast.error(res.message)
}

/** 确认清空购物车并加入新窗口的菜品 */
function confirmClearAndAdd() {
  const dish = pendingDish.value
  if (!dish) return
  cart.clear()
  const res = cart.add(dish, 1)
  pendingDish.value = null
  if (res.ok) toast.success(res.message)
  else toast.error(res.message)
}

function cancelClear() {
  pendingDish.value = null
}

onMounted(async () => {
  await Promise.all([loadWindows(), loadCategories()])
  await loadDishes()
})

// 页码变化后重新拉取
watch([pageNum, pageSize], loadDishes)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>今日菜单</h2>
      <span class="sub">加车不锁库存，下单时由服务端重新校验价格与库存</span>
    </div>

    <div v-if="auth.isLoggedIn && !auth.isStudent" class="alert alert-info">
      当前账号角色为「{{ auth.user?.roleName || '非师生用户' }}」，可以浏览菜品，但下单仅限师生用户。
    </div>

    <div class="card filter-card">
      <div class="row">
        <div class="filter-item">
          <label>食堂窗口</label>
          <select v-model="query.windowId" @change="resetAndLoad">
            <option value="">全部窗口</option>
            <option v-for="item in windows" :key="item.id" :value="item.id">
              {{ item.windowName }}（{{ item.businessStatus === 1 ? '营业中' : '已打烊' }}）
            </option>
          </select>
        </div>

        <div class="filter-item">
          <label>分类</label>
          <select v-model="query.category" @change="resetAndLoad">
            <option value="">全部分类</option>
            <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>

        <div class="filter-item filter-grow">
          <label>关键字</label>
          <input
            v-model.trim="query.keyword"
            type="search"
            placeholder="按菜品名称搜索"
            @keyup.enter="search"
          />
        </div>

        <div class="filter-item filter-btns">
          <button class="btn" type="button" @click="search">搜索</button>
          <button class="btn btn-plain" type="button" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="empty">正在加载菜品…</div>

    <div v-else-if="dishes.length === 0" class="empty">
      没有找到符合条件的菜品，试试更换窗口或重置筛选条件。
    </div>

    <div v-else class="grid grid-3 dish-grid">
      <div
        v-for="dish in dishes"
        :key="dish.id"
        class="dish-card"
        :class="{ 'sold-out': !dish.purchasable }"
      >
        <div class="thumb">🍜</div>
        <div class="body">
          <div class="row-between">
            <span class="name">{{ dish.dishName }}</span>
            <span class="tag tag-plain">{{ dish.category || '未分类' }}</span>
          </div>
          <div class="desc">{{ dish.description || '暂无描述' }}</div>
          <div class="meta muted">
            <span>{{ dish.windowName }}</span>
            <span>库存 {{ dish.stock }}</span>
          </div>
          <div class="foot">
            <span class="money">{{ money(dish.price) }}</span>
            <button
              class="btn btn-sm"
              type="button"
              :disabled="!dish.purchasable || !canOrder"
              @click="addToCart(dish)"
            >
              {{ dish.purchasable ? '加入购物车' : disabledReason(dish) }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <PaginationBar
      v-model:page-num="pageNum"
      v-model:page-size="pageSize"
      :total="total"
    />

    <!-- 购物车非空时的底部汇总条 -->
    <div v-if="showCartBar" class="cart-bar">
      <div class="cart-bar-inner">
        <span>共 <b>{{ cart.itemCount }}</b> 件</span>
        <span class="muted">{{ cart.windowName }}</span>
        <span>预计 <b class="money">{{ estimated }}</b></span>
        <router-link class="btn" to="/cart">去购物车</router-link>
      </div>
    </div>

    <BaseModal
      title="购物车中已有其他窗口的菜品"
      :visible="pendingDish !== null"
      @close="cancelClear"
    >
      <p>
        购物车中已有「{{ cart.windowName }}」的菜品，一个订单只能包含同一窗口的菜品。
        是否清空购物车并添加「{{ pendingDish?.dishName }}」？
      </p>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="cancelClear">取消</button>
        <button class="btn btn-danger" type="button" @click="confirmClearAndAdd">清空并添加</button>
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

.dish-grid {
  margin-bottom: 6px;
}

.dish-card .meta {
  display: flex;
  justify-content: space-between;
  font-size: 12.5px;
}

/* 底部汇总条固定在视口底部，避免长列表滚动后找不到购物车入口 */
.cart-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 200;
  background: #fff;
  border-top: 1px solid var(--border);
  box-shadow: 0 -3px 12px rgba(31, 42, 55, 0.1);
}

.cart-bar-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 18px;
}
</style>
