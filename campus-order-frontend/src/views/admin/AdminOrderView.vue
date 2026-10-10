<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi, windowApi } from '@/api'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { formatDateTime, money, ORDER_STATUS_OPTIONS } from '@/utils/format'

const router = useRouter()

const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const windows = ref([])

const query = reactive({ windowId: '', status: '', orderNo: '' })

async function loadWindows() {
  try {
    const list = await windowApi.list({ status: null })
    windows.value = Array.isArray(list) ? list : []
  } catch {
    windows.value = []
  }
}

async function loadOrders() {
  loading.value = true
  try {
    const res = await orderApi.page({
      scope: 'all',
      windowId: query.windowId === '' ? undefined : query.windowId,
      status: query.status === '' ? undefined : query.status,
      orderNo: query.orderNo.trim() || undefined,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orders.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    orders.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  pageNum.value = 1
  loadOrders()
}

function resetFilters() {
  query.windowId = ''
  query.status = ''
  query.orderNo = ''
  search()
}

function goDetail(orderNo) {
  router.push(`/orders/${orderNo}`)
}

onMounted(() => {
  loadWindows()
  loadOrders()
})

watch([pageNum, pageSize], loadOrders)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>全平台订单</h2>
      <span class="sub">共 {{ total }} 笔订单（只读，用于运营排查）</span>
    </div>

    <div class="card filter-card">
      <div class="row">
        <div class="filter-item">
          <label>食堂窗口</label>
          <select v-model="query.windowId" @change="search">
            <option value="">全部窗口</option>
            <option v-for="item in windows" :key="item.id" :value="item.id">
              {{ item.windowName }}
            </option>
          </select>
        </div>
        <div class="filter-item">
          <label>订单状态</label>
          <select v-model="query.status" @change="search">
            <option value="">全部状态</option>
            <option v-for="item in ORDER_STATUS_OPTIONS" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </div>
        <div class="filter-item filter-grow">
          <label>订单号</label>
          <input
            v-model.trim="query.orderNo"
            type="search"
            class="mono"
            placeholder="按订单号精确查询"
            @keyup.enter="search"
          />
        </div>
        <div class="filter-item filter-btns">
          <button class="btn" type="button" @click="search">搜索</button>
          <button class="btn btn-plain" type="button" @click="resetFilters">重置</button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="empty">正在加载订单…</div>

    <div v-else-if="orders.length === 0" class="empty">没有符合条件的订单。</div>

    <template v-else>
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>窗口</th>
              <th>金额</th>
              <th>状态</th>
              <th>下单时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orders" :key="order.orderNo">
              <td class="mono">{{ order.orderNo }}</td>
              <td>{{ order.userRealName || '-' }}</td>
              <td>{{ order.windowName }}</td>
              <td class="money">{{ money(order.totalAmount) }}</td>
              <td><OrderStatusTag :status="order.status" /></td>
              <td>{{ formatDateTime(order.createTime) }}</td>
              <td class="text-right">
                <button class="btn btn-plain btn-sm" type="button" @click="goDetail(order.orderNo)">
                  查看详情
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </template>
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
  flex: 2 1 220px;
}

.filter-btns {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex: 0 0 auto;
}
</style>
