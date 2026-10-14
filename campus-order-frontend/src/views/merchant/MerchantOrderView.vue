<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { orderApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { formatDateTime, money, newRequestNo, ORDER_STATUS } from '@/utils/format'

const auth = useAuthStore()
const toast = useToast()

/** 页签：待接单 / 备餐中 / 待取餐 / 全部（'' 代表全部） */
const tabs = [
  { value: ORDER_STATUS.PENDING, label: '待接单' },
  { value: ORDER_STATUS.PREPARING, label: '备餐中' },
  { value: ORDER_STATUS.READY, label: '待取餐' },
  { value: '', label: '全部' }
]

const activeTab = ref(ORDER_STATUS.PENDING)
const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
/** 单行操作进行中，防止重复点击 */
const actingOrderNo = ref('')

/** 拒单弹窗 */
const rejectTarget = ref(null)
const rejectReason = ref('')

/** 核码取餐弹窗 */
const pickupTarget = ref(null)
const pickupCode = ref('')

async function loadOrders(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await orderApi.page({
      scope: 'window',
      status: activeTab.value === '' ? undefined : activeTab.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orders.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    // 静默刷新失败时保留原有列表，避免界面闪烁
    if (!silent) {
      orders.value = []
      total.value = 0
    }
  } finally {
    if (!silent) loading.value = false
  }
}

function switchTab(value) {
  activeTab.value = value
  pageNum.value = 1
  loadOrders()
}

/**
 * 订单状态流转。每次操作都生成新的 requestNo：
 * 同一动作只点一次，用户重新操作属于新的请求意图。
 */
async function changeStatus(order, targetStatus, extra = {}) {
  actingOrderNo.value = order.orderNo
  try {
    await orderApi.updateStatus(order.orderNo, {
      targetStatus,
      requestNo: newRequestNo('status'),
      ...extra
    })
    return true
  } catch {
    // 失败提示由拦截器处理（例如状态已变化，不允许当前流转）
    return false
  } finally {
    actingOrderNo.value = ''
  }
}

async function acceptOrder(order) {
  const ok = await changeStatus(order, ORDER_STATUS.PREPARING)
  if (ok) {
    toast.success(`订单 ${order.orderNo} 已接单`)
    loadOrders(true)
  }
}

function openReject(order) {
  rejectTarget.value = order
  rejectReason.value = ''
}

function closeReject() {
  rejectTarget.value = null
  rejectReason.value = ''
}

async function confirmReject() {
  const order = rejectTarget.value
  if (!order) return
  if (!rejectReason.value.trim()) {
    toast.warn('请填写拒单原因')
    return
  }
  const ok = await changeStatus(order, ORDER_STATUS.REJECTED, { reason: rejectReason.value.trim() })
  if (ok) {
    toast.success(`订单 ${order.orderNo} 已拒单`)
    closeReject()
    loadOrders(true)
  }
}

async function readyOrder(order) {
  const ok = await changeStatus(order, ORDER_STATUS.READY)
  if (ok) {
    toast.success(`订单 ${order.orderNo} 已备餐完成`)
    loadOrders(true)
  }
}

function openPickup(order) {
  pickupTarget.value = order
  pickupCode.value = ''
}

function closePickup() {
  pickupTarget.value = null
  pickupCode.value = ''
}

async function confirmPickup() {
  const order = pickupTarget.value
  if (!order) return
  const code = pickupCode.value.trim()
  if (!code) {
    toast.warn('请输入顾客出示的取餐码')
    return
  }
  // 核码取餐同样带上新生成的请求号，保证一次核销对应一次请求
  const ok = await changeStatus(order, ORDER_STATUS.FINISHED, {
    pickupCode: code
  })
  if (ok) {
    toast.success(`订单 ${order.orderNo} 核码成功，已完成取餐`)
    closePickup()
    loadOrders(true)
  }
}

/** 每 15 秒自动刷新接单列表；页面不可见时跳过请求 */
let timer = null

onMounted(() => {
  loadOrders()
  timer = setInterval(() => {
    if (document.visibilityState === 'visible') loadOrders(true)
  }, 15000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  timer = null
})

watch([pageNum, pageSize], () => loadOrders())
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>接单与备餐</h2>
      <span class="sub">列表每 15 秒自动刷新一次</span>
    </div>

    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="String(tab.value)"
        type="button"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="row toolbar">
      <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="loadOrders()">
        刷新
      </button>
      <span class="muted">共 {{ total }} 笔订单</span>
    </div>

    <div v-if="loading" class="empty">正在加载订单…</div>

    <div v-else-if="orders.length === 0" class="empty">
      当前分类下暂无订单，稍后会自动刷新。
    </div>

    <template v-else>
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>订单号</th>
              <th>用户</th>
              <th>菜品</th>
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
              <td class="items-cell">
                <span v-for="item in order.items || []" :key="item.dishId" class="item-chip">
                  {{ item.dishName }} ×{{ item.quantity }}
                </span>
                <span v-if="!(order.items || []).length" class="muted">-</span>
              </td>
              <td class="money">{{ money(order.totalAmount) }}</td>
              <td><OrderStatusTag :status="order.status" /></td>
              <td>{{ formatDateTime(order.createTime) }}</td>
              <td class="text-right">
                <div class="row actions">
                  <!-- 接单 / 备餐完成需要 order:accept，拒单需要 order:reject -->
                  <template v-if="order.status === ORDER_STATUS.PENDING">
                    <button
                      v-if="auth.hasPermission('order:accept')"
                      class="btn btn-success btn-sm"
                      type="button"
                      :disabled="actingOrderNo === order.orderNo"
                      @click="acceptOrder(order)"
                    >
                      接单
                    </button>
                    <button
                      v-if="auth.hasPermission('order:reject')"
                      class="btn btn-danger btn-sm"
                      type="button"
                      :disabled="actingOrderNo === order.orderNo"
                      @click="openReject(order)"
                    >
                      拒单
                    </button>
                  </template>
                  <button
                    v-else-if="order.status === ORDER_STATUS.PREPARING && auth.hasPermission('order:accept')"
                    class="btn btn-sm"
                    type="button"
                    :disabled="actingOrderNo === order.orderNo"
                    @click="readyOrder(order)"
                  >
                    备餐完成
                  </button>
                  <button
                    v-else-if="order.status === ORDER_STATUS.READY && auth.hasPermission('order:pickup')"
                    class="btn btn-success btn-sm"
                    type="button"
                    :disabled="actingOrderNo === order.orderNo"
                    @click="openPickup(order)"
                  >
                    核码取餐
                  </button>
                  <router-link
                    v-else-if="order.status !== ORDER_STATUS.PENDING"
                    class="btn btn-plain btn-sm"
                    :to="`/orders/${order.orderNo}`"
                  >
                    查看详情
                  </router-link>
                  <span
                    v-if="
                      (order.status === ORDER_STATUS.PENDING &&
                        !auth.hasAnyPermission('order:accept', 'order:reject')) ||
                      (order.status === ORDER_STATUS.PREPARING && !auth.hasPermission('order:accept')) ||
                      (order.status === ORDER_STATUS.READY && !auth.hasPermission('order:pickup'))
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

      <PaginationBar v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </template>

    <BaseModal title="拒单" :visible="rejectTarget !== null" @close="closeReject">
      <p>确认拒绝订单 <span class="mono">{{ rejectTarget?.orderNo }}</span> 吗？拒单后顾客会看到拒绝原因。</p>
      <div class="form-item">
        <label class="required">拒单原因</label>
        <textarea
          v-model="rejectReason"
          maxlength="200"
          placeholder="例如：该菜品已售完，无法备餐"
        />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeReject">取消</button>
        <button
          class="btn btn-danger"
          type="button"
          :disabled="actingOrderNo === rejectTarget?.orderNo"
          @click="confirmReject"
        >
          确认拒单
        </button>
      </template>
    </BaseModal>

    <BaseModal title="核码取餐" :visible="pickupTarget !== null" @close="closePickup">
      <p>
        订单 <span class="mono">{{ pickupTarget?.orderNo }}</span>，
        顾客 {{ pickupTarget?.userRealName || '-' }}，
        应收金额 <span class="money">{{ money(pickupTarget?.totalAmount) }}</span>
      </p>
      <div class="form-item">
        <label class="required">取餐码</label>
        <input v-model.trim="pickupCode" type="text" placeholder="请输入顾客出示的取餐码" @keyup.enter="confirmPickup" />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closePickup">取消</button>
        <button
          class="btn btn-success"
          type="button"
          :disabled="actingOrderNo === pickupTarget?.orderNo"
          @click="confirmPickup"
        >
          确认核销
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

.items-cell {
  max-width: 260px;
  font-size: 12.5px;
}

.item-chip {
  display: inline-block;
  margin: 2px 4px 2px 0;
  padding: 1px 6px;
  color: var(--text-muted);
  background: #f2f5f9;
  border-radius: 4px;
}
</style>
