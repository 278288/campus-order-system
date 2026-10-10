<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { orderApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import { useToast } from '@/utils/toast'
import { formatDateTime, money, ORDER_STATUS, ORDER_STATUS_OPTIONS } from '@/utils/format'

const router = useRouter()
const toast = useToast()

const orders = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const status = ref('')

/** 取消订单弹窗状态 */
const cancelTarget = ref(null)
const cancelReason = ref('')
const canceling = ref(false)

/** 评价弹窗状态 */
const rateTarget = ref(null)
const rateForm = ref({ rating: 5, comment: '' })
const rating = ref(false)

async function loadOrders(silent = false) {
  if (!silent) loading.value = true
  try {
    const res = await orderApi.page({
      scope: 'mine',
      status: status.value === '' ? undefined : status.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    orders.value = res?.records || []
    total.value = res?.total || 0
  } catch {
    // 静默刷新失败不打断用户，也不清空已有列表
    if (!silent) {
      orders.value = []
      total.value = 0
    }
  } finally {
    if (!silent) loading.value = false
  }
}

function changeStatus(value) {
  status.value = value
  pageNum.value = 1
  loadOrders()
}

function goDetail(orderNo) {
  router.push(`/orders/${orderNo}`)
}

function openCancel(order) {
  cancelTarget.value = order
  cancelReason.value = ''
}

function closeCancel() {
  cancelTarget.value = null
  cancelReason.value = ''
}

async function confirmCancel() {
  if (!cancelTarget.value) return
  canceling.value = true
  try {
    await orderApi.cancel(cancelTarget.value.orderNo, cancelReason.value.trim() || undefined)
    toast.success('订单已取消')
    closeCancel()
    loadOrders()
  } catch {
    // 失败提示由拦截器处理（例如状态已变化不允许取消）
  } finally {
    canceling.value = false
  }
}

function openRate(order) {
  rateTarget.value = order
  rateForm.value = { rating: 5, comment: '' }
}

function closeRate() {
  rateTarget.value = null
}

async function confirmRate() {
  if (!rateTarget.value) return
  if (!rateForm.value.rating) {
    toast.warn('请选择评分')
    return
  }
  rating.value = true
  try {
    await orderApi.rate(rateTarget.value.orderNo, {
      rating: rateForm.value.rating,
      comment: rateForm.value.comment.trim()
    })
    toast.success('评价成功，感谢你的反馈')
    closeRate()
    loadOrders()
  } catch {
    // 失败提示由拦截器处理
  } finally {
    rating.value = false
  }
}

const canCancel = (order) => order.status === ORDER_STATUS.PENDING
const canRate = (order) => order.status === ORDER_STATUS.FINISHED && order.rating == null

const starText = computed(() => ['很差', '较差', '一般', '满意', '非常满意'][rateForm.value.rating - 1] || '')

/**
 * 自动刷新定时器：每 20 秒刷新一次订单状态，
 * 页面隐藏（切到其他标签页）时跳过请求，避免无意义的轮询。
 */
let timer = null

onMounted(() => {
  loadOrders()
  timer = setInterval(() => {
    if (document.visibilityState === 'visible') loadOrders(true)
  }, 20000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  timer = null
})
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>我的订单</h2>
      <span class="sub">列表每 20 秒自动刷新一次</span>
    </div>

    <div class="card filter-card">
      <div class="row">
        <span class="muted">订单状态</span>
        <button
          class="btn btn-sm"
          :class="status === '' ? '' : 'btn-plain'"
          type="button"
          @click="changeStatus('')"
        >
          全部
        </button>
        <button
          v-for="item in ORDER_STATUS_OPTIONS"
          :key="item.value"
          class="btn btn-sm"
          :class="status === item.value ? '' : 'btn-plain'"
          type="button"
          @click="changeStatus(item.value)"
        >
          {{ item.label }}
        </button>
        <button class="btn btn-ghost btn-sm" type="button" :disabled="loading" @click="loadOrders()">
          刷新
        </button>
      </div>
    </div>

    <div v-if="loading" class="empty">正在加载订单…</div>

    <div v-else-if="orders.length === 0" class="empty">
      暂无订单，去<router-link to="/dishes">选餐</router-link>下单吧。
    </div>

    <template v-else>
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>订单号</th>
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
              <td>{{ order.windowName }}</td>
              <td class="money">{{ money(order.totalAmount) }}</td>
              <td>
                <OrderStatusTag :status="order.status" />
                <span v-if="order.rating != null" class="muted rating-hint">已评 {{ order.rating }} 星</span>
              </td>
              <td>{{ formatDateTime(order.createTime) }}</td>
              <td class="text-right">
                <div class="row actions">
                  <button class="btn btn-plain btn-sm" type="button" @click="goDetail(order.orderNo)">
                    查看详情
                  </button>
                  <button
                    v-if="canCancel(order)"
                    class="btn btn-danger btn-sm"
                    type="button"
                    @click="openCancel(order)"
                  >
                    取消订单
                  </button>
                  <button v-if="canRate(order)" class="btn btn-sm" type="button" @click="openRate(order)">
                    评价
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar v-model:page-num="pageNum" v-model:page-size="pageSize" :total="total" />
    </template>

    <!-- 取消订单：必须填写原因时走弹窗而非浏览器 prompt -->
    <BaseModal title="取消订单" :visible="cancelTarget !== null" @close="closeCancel">
      <p>确认取消订单 <span class="mono">{{ cancelTarget?.orderNo }}</span> 吗？</p>
      <div class="form-item">
        <label>取消原因（选填）</label>
        <textarea v-model="cancelReason" maxlength="200" placeholder="例如：临时有事，不需要了" />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeCancel">再想想</button>
        <button class="btn btn-danger" type="button" :disabled="canceling" @click="confirmCancel">
          {{ canceling ? '提交中…' : '确认取消' }}
        </button>
      </template>
    </BaseModal>

    <!-- 评价：1-5 星 + 文字评论 -->
    <BaseModal title="订单评价" :visible="rateTarget !== null" @close="closeRate">
      <p>订单 <span class="mono">{{ rateTarget?.orderNo }}</span>（{{ rateTarget?.windowName }}）</p>
      <div class="form-item">
        <label>评分</label>
        <div class="row stars">
          <button
            v-for="n in 5"
            :key="n"
            class="star"
            :class="{ active: n <= rateForm.rating }"
            type="button"
            @click="rateForm.rating = n"
          >
            ★
          </button>
          <span class="muted">{{ starText }}</span>
        </div>
      </div>
      <div class="form-item">
        <label>评价内容（选填）</label>
        <textarea v-model="rateForm.comment" maxlength="200" placeholder="说说这次的用餐体验" />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="closeRate">取消</button>
        <button class="btn" type="button" :disabled="rating" @click="confirmRate">
          {{ rating ? '提交中…' : '提交评价' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}

.actions {
  justify-content: flex-end;
  gap: 6px;
}

.rating-hint {
  margin-left: 6px;
  font-size: 12px;
}

.stars {
  gap: 4px;
}

.star {
  padding: 0 2px;
  font-size: 22px;
  line-height: 1;
  color: #d8dfe8;
  background: none;
  border: none;
  cursor: pointer;
}

.star.active {
  color: #f0a500;
}
</style>
