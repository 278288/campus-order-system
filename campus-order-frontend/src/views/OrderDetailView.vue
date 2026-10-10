<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { orderApi } from '@/api'
import BaseModal from '@/components/BaseModal.vue'
import OrderStatusTag from '@/components/OrderStatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'
import { formatDateTime, money, ORDER_STATUS, ORDER_STATUS_TEXT } from '@/utils/format'

const route = useRoute()
const auth = useAuthStore()
const toast = useToast()

const order = ref(null)
const loading = ref(true)
const failed = ref(false)

const cancelVisible = ref(false)
const cancelReason = ref('')
const canceling = ref(false)

const orderNo = computed(() => String(route.params.orderNo || ''))

/** 取餐码在待接单 / 备餐中 / 待取餐三个阶段都需要明显展示 */
const showPickupCode = computed(() =>
  order.value && [ORDER_STATUS.PENDING, ORDER_STATUS.PREPARING, ORDER_STATUS.READY].includes(order.value.status)
)

/** 仅本人未接单的订单可以取消 */
const canCancel = computed(
  () =>
    order.value &&
    order.value.status === ORDER_STATUS.PENDING &&
    auth.user?.id != null &&
    order.value.userId === auth.user.id
)

const items = computed(() => order.value?.items || [])

async function loadDetail() {
  loading.value = true
  failed.value = false
  try {
    order.value = await orderApi.detail(orderNo.value)
  } catch {
    order.value = null
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function confirmCancel() {
  canceling.value = true
  try {
    await orderApi.cancel(orderNo.value, cancelReason.value.trim() || undefined)
    toast.success('订单已取消')
    cancelVisible.value = false
    cancelReason.value = ''
    await loadDetail()
  } catch {
    // 失败提示由拦截器处理
  } finally {
    canceling.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>订单详情</h2>
      <router-link class="btn btn-plain btn-sm" to="/orders">返回订单列表</router-link>
    </div>

    <div v-if="loading" class="empty">正在加载订单详情…</div>

    <div v-else-if="failed || !order" class="card">
      <div class="empty">
        未找到订单 <span class="mono">{{ orderNo }}</span>，可能订单号有误或该订单不属于当前账号。
        <div class="row back-row">
          <router-link class="btn btn-sm" to="/orders">返回我的订单</router-link>
          <router-link class="btn btn-plain btn-sm" to="/dishes">去选餐</router-link>
        </div>
      </div>
    </div>

    <template v-else>
      <div class="card">
        <div class="card-title">订单信息</div>
        <div class="grid grid-2 meta-grid">
          <div class="meta-item">
            <span class="muted">订单号</span>
            <span class="mono">{{ order.orderNo }}</span>
          </div>
          <div class="meta-item">
            <span class="muted">订单状态</span>
            <OrderStatusTag :status="order.status" />
          </div>
          <div class="meta-item">
            <span class="muted">食堂窗口</span>
            <span>{{ order.windowName }}</span>
          </div>
          <div class="meta-item">
            <span class="muted">下单用户</span>
            <span>{{ order.userRealName || '-' }}</span>
          </div>
          <div class="meta-item">
            <span class="muted">订单金额</span>
            <span class="money">{{ money(order.totalAmount) }}</span>
          </div>
          <div class="meta-item">
            <span class="muted">下单时间</span>
            <span>{{ formatDateTime(order.createTime) }}</span>
          </div>
        </div>

        <div v-if="showPickupCode" class="pickup-box">
          <div class="muted">取餐码</div>
          <div class="pickup-code">{{ order.pickupCode }}</div>
          <div class="muted pickup-hint">请到 {{ order.windowName }} 报出取餐码取餐</div>
        </div>

        <div v-if="canCancel" class="row cancel-row">
          <button class="btn btn-danger" type="button" @click="cancelVisible = true">取消订单</button>
        </div>
      </div>

      <div class="card section-card">
        <div class="card-title">菜品明细</div>
        <div v-if="items.length === 0" class="empty">该订单没有菜品明细记录。</div>
        <div v-else class="table-wrap">
          <table class="data">
            <thead>
              <tr>
                <th>菜品</th>
                <th>单价</th>
                <th>数量</th>
                <th class="text-right">小计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="item.dishId">
                <td>{{ item.dishName }}</td>
                <td>{{ money(item.unitPrice) }}</td>
                <td>{{ item.quantity }}</td>
                <td class="text-right money">{{ money(item.amount) }}</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="3" class="text-right muted">订单合计</td>
                <td class="text-right money">{{ money(order.totalAmount) }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
      </div>

      <div class="card section-card">
        <div class="card-title">订单进度</div>
        <ul class="timeline">
          <li class="done">
            <span class="dot" />
            <div>
              <div class="step-title">已下单</div>
              <div class="muted">{{ formatDateTime(order.createTime) }}</div>
            </div>
          </li>
          <li :class="{ done: order.acceptTime }">
            <span class="dot" />
            <div>
              <div class="step-title">商家接单</div>
              <div class="muted">{{ order.acceptTime ? formatDateTime(order.acceptTime) : '等待商家接单' }}</div>
            </div>
          </li>
          <li :class="{ done: order.readyTime }">
            <span class="dot" />
            <div>
              <div class="step-title">备餐完成</div>
              <div class="muted">{{ order.readyTime ? formatDateTime(order.readyTime) : '等待商家备餐' }}</div>
            </div>
          </li>
          <li :class="{ done: order.finishTime }">
            <span class="dot" />
            <div>
              <div class="step-title">取餐完成</div>
              <div class="muted">{{ order.finishTime ? formatDateTime(order.finishTime) : '等待取餐' }}</div>
            </div>
          </li>
          <li v-if="order.status === ORDER_STATUS.CANCELED" class="canceled">
            <span class="dot" />
            <div>
              <div class="step-title">{{ ORDER_STATUS_TEXT[ORDER_STATUS.CANCELED] }}</div>
              <div class="muted">取消原因：{{ order.cancelReason || '未填写' }}</div>
            </div>
          </li>
          <li v-if="order.status === ORDER_STATUS.REJECTED" class="canceled">
            <span class="dot" />
            <div>
              <div class="step-title">{{ ORDER_STATUS_TEXT[ORDER_STATUS.REJECTED] }}</div>
              <div class="muted">拒绝原因：{{ order.rejectReason || '未填写' }}</div>
            </div>
          </li>
        </ul>

        <div v-if="order.rating != null" class="alert alert-info rating-box">
          我的评价：{{ order.rating }} 星 {{ order.ratingComment ? `—— ${order.ratingComment}` : '' }}
        </div>
      </div>
    </template>

    <BaseModal title="取消订单" :visible="cancelVisible" @close="cancelVisible = false">
      <p>确认取消订单 <span class="mono">{{ orderNo }}</span> 吗？</p>
      <div class="form-item">
        <label>取消原因（选填）</label>
        <textarea v-model="cancelReason" maxlength="200" placeholder="例如：临时有事，不需要了" />
      </div>
      <template #footer>
        <button class="btn btn-plain" type="button" @click="cancelVisible = false">再想想</button>
        <button class="btn btn-danger" type="button" :disabled="canceling" @click="confirmCancel">
          {{ canceling ? '提交中…' : '确认取消' }}
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
.meta-grid {
  margin-bottom: 4px;
}

.meta-item {
  display: flex;
  gap: 10px;
  padding: 6px 0;
  font-size: 13.5px;
}

.meta-item .muted {
  min-width: 72px;
}

.back-row {
  justify-content: center;
  margin-top: 12px;
}

.pickup-box {
  margin-top: 14px;
  padding: 16px;
  text-align: center;
  background: var(--brand-soft);
  border: 1px dashed #9fc7e8;
  border-radius: var(--radius);
}

.pickup-code {
  margin: 6px 0;
  font-family: Consolas, 'Courier New', monospace;
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 6px;
  color: var(--brand-dark);
}

.pickup-hint {
  font-size: 12.5px;
}

.cancel-row {
  margin-top: 14px;
}

.section-card {
  margin-top: 16px;
}

table tfoot td {
  padding: 10px 12px;
  border-top: 1px solid var(--border);
  font-weight: 600;
}

.timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline li {
  position: relative;
  display: flex;
  gap: 12px;
  padding: 0 0 16px 4px;
}

.timeline li:last-child {
  padding-bottom: 0;
}

.timeline .dot {
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  margin-top: 5px;
  background: #d8dfe8;
  border-radius: 50%;
}

.timeline li.done .dot {
  background: var(--success);
}

.timeline li.canceled .dot {
  background: var(--danger);
}

.timeline .step-title {
  font-size: 13.5px;
  font-weight: 600;
}

.timeline li .muted {
  font-size: 12.5px;
}

.rating-box {
  margin: 14px 0 0;
}
</style>
