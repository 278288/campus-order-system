<script setup>
import { ref } from 'vue'
import { orderApi } from '@/api'
import { useToast } from '@/utils/toast'
import { formatDateTime, money } from '@/utils/format'

const toast = useToast()

const orderNo = ref('')
const pickupCode = ref('')
const verifying = ref(false)

/** 最近一次核验结果，用大面板展示成功或失败 */
const result = ref(null)
/** 本地核验历史，只保留最近 10 条 */
const history = ref([])

async function verify() {
  const no = orderNo.value.trim()
  const code = pickupCode.value.trim()
  if (!no) {
    toast.warn('请输入订单号')
    return
  }
  if (!code) {
    toast.warn('请输入取餐码')
    return
  }

  verifying.value = true
  result.value = null
  try {
    const order = await orderApi.verifyPickup(no, code)
    result.value = {
      ok: true,
      message: '核验成功，请将餐品交给顾客',
      order
    }
    pushHistory(no, code, true, '核验成功')
    toast.success('核验成功，取餐完成')
    // 成功后清空输入，方便连续核销下一单
    orderNo.value = ''
    pickupCode.value = ''
  } catch (error) {
    // 失败原因（订单状态不符、取餐码不匹配等）直接展示在结果面板上
    const message = error?.message || '核验失败，请核对订单号与取餐码'
    result.value = { ok: false, message, order: null }
    pushHistory(no, code, false, message)
    toast.error(message)
  } finally {
    verifying.value = false
  }
}

function pushHistory(no, code, ok, message) {
  history.value.unshift({
    key: `${no}-${Date.now()}`,
    orderNo: no,
    pickupCode: code,
    ok,
    message,
    time: formatDateTime(new Date().toISOString())
  })
  if (history.value.length > 10) history.value = history.value.slice(0, 10)
}

function reset() {
  orderNo.value = ''
  pickupCode.value = ''
  result.value = null
}

function clearHistory() {
  history.value = []
}

/** 点击历史记录可以快速回填，便于重复核验 */
function refill(item) {
  orderNo.value = item.orderNo
  pickupCode.value = item.pickupCode
}
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>核码取餐</h2>
      <span class="sub">顾客出示取餐码后在此核销，核销成功的订单状态变为已完成</span>
    </div>

    <div class="grid grid-2">
      <div class="card">
        <div class="card-title">核验信息</div>
        <form @submit.prevent="verify">
          <div class="form-item">
            <label class="required">订单号</label>
            <input v-model.trim="orderNo" type="text" class="mono" placeholder="请输入订单号" />
          </div>
          <div class="form-item">
            <label class="required">取餐码</label>
            <input v-model.trim="pickupCode" type="text" placeholder="请输入顾客出示的取餐码" />
          </div>
          <div class="row">
            <button class="btn btn-success" type="submit" :disabled="verifying">
              {{ verifying ? '核验中…' : '核 验' }}
            </button>
            <button class="btn btn-plain" type="button" :disabled="verifying" @click="reset">清空</button>
          </div>
        </form>

        <!-- 核验结果面板：成功绿色、失败红色 -->
        <div v-if="result" class="result-panel" :class="result.ok ? 'ok' : 'fail'">
          <div class="result-title">{{ result.ok ? '✅ 核验成功' : '❌ 核验失败' }}</div>
          <div class="result-message">{{ result.message }}</div>
          <div v-if="result.order" class="result-detail">
            <div>订单号：<span class="mono">{{ result.order.orderNo }}</span></div>
            <div>顾客：{{ result.order.userRealName || '-' }}</div>
            <div>窗口：{{ result.order.windowName }}</div>
            <div>金额：<span class="money">{{ money(result.order.totalAmount) }}</span></div>
            <div>状态：{{ result.order.statusName }}</div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="row-between">
          <div class="card-title no-border">最近核验记录（最多 10 条）</div>
          <button
            v-if="history.length"
            class="btn btn-plain btn-sm"
            type="button"
            @click="clearHistory"
          >
            清空记录
          </button>
        </div>

        <div v-if="history.length === 0" class="empty">还没有核验记录。</div>

        <ul v-else class="history">
          <li v-for="item in history" :key="item.key" :class="{ ok: item.ok, fail: !item.ok }">
            <div class="row-between">
              <span class="mono">{{ item.orderNo }}</span>
              <span class="tag" :class="item.ok ? 'tag-3' : 'tag-5'">
                {{ item.ok ? '成功' : '失败' }}
              </span>
            </div>
            <div class="muted history-line">
              取餐码 {{ item.pickupCode }} · {{ item.time }}
            </div>
            <div class="muted history-line">{{ item.message }}</div>
            <button class="btn btn-plain btn-sm" type="button" @click="refill(item)">回填</button>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.no-border {
  margin-bottom: 6px;
  padding-bottom: 0;
  border-bottom: none;
}

.result-panel {
  margin-top: 18px;
  padding: 18px;
  text-align: center;
  border: 1px solid;
  border-radius: var(--radius);
}

.result-panel.ok {
  color: #1f7a45;
  background: #e6f6ec;
  border-color: #b6e3c7;
}

.result-panel.fail {
  color: #a13225;
  background: #fdeceb;
  border-color: #f6c6c1;
}

.result-title {
  font-size: 22px;
  font-weight: 700;
}

.result-message {
  margin-top: 6px;
  font-size: 14px;
}

.result-detail {
  margin-top: 12px;
  padding-top: 12px;
  text-align: left;
  font-size: 13px;
  color: var(--text);
  border-top: 1px dashed currentColor;
}

.history {
  margin: 0;
  padding: 0;
  list-style: none;
}

.history li {
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
}

.history li:last-child {
  border-bottom: none;
}

.history-line {
  margin: 4px 0;
  font-size: 12.5px;
}
</style>
