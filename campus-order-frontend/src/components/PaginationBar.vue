<script setup>
import { computed } from 'vue'

/**
 * 通用分页条。
 * 使用 v-model:pageNum / v-model:pageSize，父组件只需把值透传给接口即可。
 */
const props = defineProps({
  total: { type: Number, default: 0 },
  pageNum: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 }
})

const emit = defineEmits(['update:pageNum', 'update:pageSize'])

/** 总页数至少为 1，避免空数据时出现 0 页 */
const pageCount = computed(() => Math.max(1, Math.ceil((props.total || 0) / (props.pageSize || 10))))
const canPrev = computed(() => props.pageNum > 1)
const canNext = computed(() => props.pageNum < pageCount.value)

function goPrev() {
  if (canPrev.value) emit('update:pageNum', props.pageNum - 1)
}

function goNext() {
  if (canNext.value) emit('update:pageNum', props.pageNum + 1)
}

/** 改变每页条数时回到第一页，否则可能出现页码越界 */
function changeSize(event) {
  emit('update:pageSize', Number(event.target.value))
  emit('update:pageNum', 1)
}
</script>

<template>
  <div class="pagination">
    <span>共 {{ total }} 条</span>
    <span>第 {{ pageNum }} / {{ pageCount }} 页</span>
    <button class="btn btn-plain btn-sm" type="button" :disabled="!canPrev" @click="goPrev">
      上一页
    </button>
    <button class="btn btn-plain btn-sm" type="button" :disabled="!canNext" @click="goNext">
      下一页
    </button>
    <select class="page-size" :value="pageSize" @change="changeSize">
      <option :value="10">10 条/页</option>
      <option :value="20">20 条/页</option>
      <option :value="50">50 条/页</option>
    </select>
  </div>
</template>

<style scoped>
.page-size {
  width: auto;
  min-width: 96px;
  padding: 5px 8px;
  font-size: 13px;
}
</style>
