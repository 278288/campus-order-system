<script setup>
import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 基础弹窗：点击遮罩或按 Esc 关闭。
 * 重要业务流程（取消订单、拒单、评价、核码取餐、调整库存）统一使用本组件，
 * 不使用浏览器原生 prompt / confirm。
 */
const props = defineProps({
  title: { type: String, default: '' },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}

/** 只有点击遮罩本身才关闭，点击弹窗内部不应关闭 */
function onMaskClick(event) {
  if (event.target === event.currentTarget) close()
}

function onKeydown(event) {
  if (event.key === 'Escape' && props.visible) close()
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div v-if="visible" class="modal-mask" @click="onMaskClick">
    <div class="modal" role="dialog" aria-modal="true">
      <div class="modal-header">
        <span>{{ title }}</span>
        <button class="modal-close" type="button" aria-label="关闭" @click="close">×</button>
      </div>
      <div class="modal-body">
        <slot />
      </div>
      <div class="modal-footer">
        <slot name="footer">
          <button class="btn btn-plain" type="button" @click="close">关闭</button>
        </slot>
      </div>
    </div>
  </div>
</template>
