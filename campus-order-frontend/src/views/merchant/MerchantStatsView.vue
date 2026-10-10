<script setup>
import { computed, onMounted, ref } from 'vue'
import { statsApi } from '@/api'
import StatCard from '@/components/StatCard.vue'
import { money, todayString } from '@/utils/format'

const startDate = ref(todayString(-6))
const endDate = ref(todayString())
const stats = ref(null)
const loading = ref(true)

const dailyStats = computed(() => stats.value?.dailyStats || [])
const dishRanking = computed(() => stats.value?.dishRanking || [])

/** 条形图按最大订单量归一化，避免出现超出容器的宽度 */
const maxOrderCount = computed(() =>
  dailyStats.value.reduce((max, item) => Math.max(max, Number(item.orderCount) || 0), 0)
)

function barWidth(item) {
  if (!maxOrderCount.value) return '0%'
  const ratio = ((Number(item.orderCount) || 0) / maxOrderCount.value) * 100
  return `${Math.max(ratio, 2)}%`
}

const canceledOrRejected = computed(() => stats.value?.canceledOrderCount ?? 0)
const averageAmount = computed(() => {
  const value = stats.value?.averageAmount
  return value == null ? money(0) : money(value)
})

async function loadStats() {
  loading.value = true
  try {
    stats.value = await statsApi.orders({
      startDate: startDate.value,
      endDate: endDate.value
    })
  } catch {
    // 商家账号未绑定窗口等错误由拦截器提示，这里保证页面结构仍然完整
    stats.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="page">
    <div class="page-title">
      <h2>窗口统计</h2>
      <span class="sub">
        统计范围：{{ stats?.startDate || startDate }} ~ {{ stats?.endDate || endDate }}
      </span>
    </div>

    <div class="card filter-card">
      <div class="row">
        <div class="filter-item">
          <label>开始日期</label>
          <input v-model="startDate" type="date" />
        </div>
        <div class="filter-item">
          <label>结束日期</label>
          <input v-model="endDate" type="date" />
        </div>
        <div class="filter-item filter-btns">
          <button class="btn" type="button" :disabled="loading" @click="loadStats">
            {{ loading ? '统计中…' : '查询统计' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="loading" class="empty">正在统计数据…</div>

    <div v-else-if="!stats" class="empty">
      暂无统计数据，请确认商家账号已绑定食堂窗口，或调整日期范围后重试。
    </div>

    <template v-else>
      <div class="stat-grid">
        <StatCard label="有效订单数" :value="stats.validOrderCount ?? 0" />
        <StatCard label="已完成订单数" :value="stats.finishedOrderCount ?? 0" />
        <StatCard label="已取消 / 已拒绝" :value="canceledOrRejected" />
        <StatCard label="有效金额" :value="money(stats.validAmount)" money />
        <StatCard label="已完成金额" :value="money(stats.finishedAmount)" money />
        <StatCard label="平均客单价" :value="averageAmount" money />
      </div>

      <div class="card section-card">
        <div class="card-title">每日订单量与金额</div>
        <div v-if="dailyStats.length === 0" class="empty">该时间段内没有订单数据。</div>
        <div v-else>
          <div v-for="item in dailyStats" :key="item.date" class="bar-row">
            <span class="bar-label">{{ item.date }}</span>
            <span class="bar-track">
              <span class="bar-fill" :style="{ width: barWidth(item) }" />
            </span>
            <span class="bar-value">{{ item.orderCount }} 单 / {{ money(item.amount) }}</span>
          </div>
        </div>
      </div>

      <div class="card section-card">
        <div class="card-title">热销菜品</div>
        <div v-if="dishRanking.length === 0" class="empty">该时间段内没有菜品销量数据。</div>
        <div v-else class="table-wrap">
          <table class="data">
            <thead>
              <tr>
                <th>排名</th>
                <th>菜品</th>
                <th>销量（份）</th>
                <th class="text-right">销售额</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in dishRanking" :key="item.dishId">
                <td>{{ index + 1 }}</td>
                <td>{{ item.dishName }}</td>
                <td>{{ item.quantity }}</td>
                <td class="text-right money">{{ money(item.amount) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
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

.filter-btns {
  display: flex;
  align-items: flex-end;
  flex: 0 0 auto;
}

.section-card {
  margin-top: 16px;
}

.bar-row .bar-value {
  width: 160px;
}
</style>
