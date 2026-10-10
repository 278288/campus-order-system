<script setup>
import { ORDER_STATUS_TEXT } from '@/utils/format'

/** 订单状态标签，颜色由 global.css 中的 .tag-0 ~ .tag-5 提供 */
defineProps({
  status: { type: Number, default: 0 }
})
</script>

<template>
  <span class="tag" :class="`tag-${status}`">{{ ORDER_STATUS_TEXT[status] || '未知状态' }}</span>
</template>
