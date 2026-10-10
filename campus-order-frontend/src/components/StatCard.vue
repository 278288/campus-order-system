<script setup>
/**
 * 统计卡片。商家统计与平台统计两处结构完全一致，抽出来减少重复。
 */
defineProps({
  label: { type: String, default: '' },
  value: { type: [String, Number], default: '-' },
  /** 金额类指标用红色突出显示 */
  money: { type: Boolean, default: false }
})
</script>

<template>
  <div class="stat-card">
    <div class="label">{{ label }}</div>
    <div class="value" :class="{ money }">{{ value }}</div>
  </div>
</template>
