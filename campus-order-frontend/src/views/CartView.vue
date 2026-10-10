<script setup>
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi, orderApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import { useToast } from '@/utils/toast'
import { money, newRequestNo } from '@/utils/format'

const router = useRouter()
const cart = useCartStore()
const toast = useToast()

const submitting = ref(false)
/** 刷新购物车时的加载标记 */
const refreshing = ref(false)
/** 服务端校验失败（如 409 库存/价格变化）后的提示信息 */
const conflictMessage = ref('')

/**
 * 幂等请求号：同一次下单意图必须复用同一个 requestNo。
 * 只有下单成功、或购物车内容发生变化时才重新生成，
 * 这样用户点击重试时后端能识别为同一次请求而不会重复下单。
 */
let pendingRequestNo = null

function ensureRequestNo() {
  if (!pendingRequestNo) pendingRequestNo = newRequestNo('order')
  return pendingRequestNo
}

function resetRequestNo() {
  pendingRequestNo = null
}

/** 购物车内容指纹，变化即意味着这是一次新的下单意图 */
const cartSignature = computed(() =>
  cart.items.map((i) => `${i.dishId}:${i.quantity}`).join('|')
)

watch(cartSignature, () => {
  resetRequestNo()
  conflictMessage.value = ''
})

const hasShortage = computed(() =>
  cart.items.some((item) => !item.stock || item.quantity > item.stock)
)

function changeQuantity(item, delta) {
  const res = cart.updateQuantity(item.dishId, item.quantity + delta)
  if (res.ok) {
    if (res.message !== '已更新数量') toast.info(res.message)
  } else {
    toast.error(res.message)
  }
}

/** 直接输入数量时做一次校验，非法值回落为原数量 */
function onQuantityInput(item, event) {
  const value = Number(event.target.value)
  if (!Number.isFinite(value) || value <= 0) {
    event.target.value = item.quantity
    return
  }
  const res = cart.updateQuantity(item.dishId, value)
  if (!res.ok) {
    toast.error(res.message)
    event.target.value = item.quantity
  } else if (res.message === '已移除') {
    toast.info(res.message)
  }
}

function removeItem(item) {
  cart.remove(item.dishId)
  toast.info(`已移除「${item.dishName}」`)
}

async function submitOrder() {
  if (cart.isEmpty) {
    toast.warn('购物车为空，请先选择菜品')
    return
  }
  if (hasShortage.value) {
    toast.warn('存在库存不足的菜品，请先调整数量或刷新购物车')
    return
  }
  submitting.value = true
  conflictMessage.value = ''
  const requestNo = ensureRequestNo()
  try {
    const order = await orderApi.create({
      requestNo,
      items: cart.items.map((item) => ({ dishId: item.dishId, quantity: item.quantity }))
    })
    // 下单成功：清除请求号与购物车，再进入订单列表
    resetRequestNo()
    cart.clearAfterOrder()
    toast.success(`下单成功，取餐码 ${order?.pickupCode ?? '—'}`)
    router.push('/orders')
  } catch (error) {
    // 409 表示服务端重新校验后发现价格或库存已变化，需要用户刷新购物车
    if (error?.code === 409) {
      conflictMessage.value = error.message || '菜品价格或库存已变化，请刷新购物车后重新提交'
    }
    // 其余错误提示已由拦截器弹出；请求号保留，用户可直接重试
  } finally {
    submitting.value = false
  }
}

/** 逐个拉取最新菜品信息，刷新购物车中的价格与库存 */
async function refreshCart() {
  if (cart.isEmpty) return
  refreshing.value = true
  try {
    const results = await Promise.all(
      cart.items.map(async (item) => {
        try {
          const detail = await publicApi.dishDetail(item.dishId)
          return { item, detail }
        } catch {
          return null
        }
      })
    )
    let adjusted = 0
    let removed = 0
    results.filter(Boolean).forEach(({ item, detail }) => {
      if (detail.status === 0 || detail.windowBusinessStatus === 0 || (detail.stock ?? 0) <= 0) {
        // 已下架或窗口打烊或售罄，直接从购物车移除
        cart.remove(item.dishId)
        removed += 1
        return
      }
      const stock = Number(detail.stock)
      const nextQuantity = Math.min(item.quantity, stock)
      const priceChanged = Number(detail.price) !== Number(item.price)
      if (nextQuantity !== item.quantity || priceChanged) adjusted += 1
      item.price = Number(detail.price)
      item.stock = stock
      item.dishName = detail.dishName
      item.windowId = detail.windowId
      item.quantity = nextQuantity
    })
    cart.persist()
    conflictMessage.value = ''
    resetRequestNo()
    const parts = []
    if (adjusted > 0) parts.push(`${adjusted} 项价格或数量已更新`)
    if (removed > 0) parts.push(`${removed} 项因下架或售罄已移除`)
    toast.success(parts.length ? `购物车已刷新，${parts.join('，')}` : '购物车已是最新状态')
  } catch {
    toast.error('刷新购物车失败，请稍后重试')
  } finally {
    refreshing.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>购物车</h2>
      <span class="sub">
        <template v-if="cart.windowName">当前窗口：{{ cart.windowName }}</template>
        <template v-else>尚未选择任何菜品</template>
      </span>
    </div>

    <div class="alert alert-warn">
      加车不锁库存，下单时由服务端重新校验价格与库存，预计金额不等于成交金额。
    </div>

    <div v-if="conflictMessage" class="alert alert-danger">
      <div>{{ conflictMessage }}</div>
      <div class="row conflict-row">
        <button class="btn btn-danger btn-sm" type="button" :disabled="refreshing" @click="refreshCart">
          {{ refreshing ? '刷新中…' : '刷新购物车' }}
        </button>
        <span class="muted">刷新后会同步最新价格与库存，确认无误即可重新提交。</span>
      </div>
    </div>

    <div v-if="cart.isEmpty" class="empty">
      购物车还是空的，去<router-link to="/dishes">选餐</router-link>看看吧。
    </div>

    <template v-else>
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              <th>菜品</th>
              <th>单价</th>
              <th>数量</th>
              <th>小计</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in cart.items" :key="item.dishId">
              <td>
                <div>{{ item.dishName }}</div>
                <div class="muted stock-hint">库存 {{ item.stock }}</div>
              </td>
              <td>{{ money(item.price) }}</td>
              <td>
                <div class="qty">
                  <button class="btn btn-plain btn-sm" type="button" @click="changeQuantity(item, -1)">
                    −
                  </button>
                  <input
                    class="qty-input"
                    type="number"
                    min="1"
                    :value="item.quantity"
                    @change="onQuantityInput(item, $event)"
                  />
                  <button
                    class="btn btn-plain btn-sm"
                    type="button"
                    :disabled="item.quantity >= item.stock"
                    @click="changeQuantity(item, 1)"
                  >
                    ＋
                  </button>
                </div>
              </td>
              <td class="money">{{ money(item.price * item.quantity) }}</td>
              <td class="text-right">
                <button class="btn btn-plain btn-sm" type="button" @click="removeItem(item)">移除</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="card summary-row">
        <div class="row-between">
          <div>
            共 <b>{{ cart.itemCount }}</b> 件，预计金额
            <span class="money total-amount">{{ money(cart.totalAmount) }}</span>
          </div>
          <div class="row">
            <button class="btn btn-plain" type="button" :disabled="refreshing" @click="refreshCart">
              {{ refreshing ? '刷新中…' : '刷新价格库存' }}
            </button>
            <button class="btn" type="button" :disabled="submitting" @click="submitOrder">
              {{ submitting ? '提交中…' : '去结算' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.conflict-row {
  margin-top: 8px;
}

.stock-hint {
  font-size: 12px;
}

.qty {
  display: flex;
  align-items: center;
  gap: 6px;
}

.qty-input {
  width: 64px;
  text-align: center;
}

.summary-row {
  margin-top: 16px;
}

.total-amount {
  font-size: 18px;
}
</style>
