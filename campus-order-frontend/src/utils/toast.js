import { reactive } from 'vue'

/**
 * 轻量全局提示，避免为课程项目引入额外的 UI 组件库。
 * 由 App.vue 渲染 toasts 列表。
 */
const state = reactive({ toasts: [] })

let seed = 0

function push(type, message, duration = 2600) {
  const id = ++seed
  state.toasts.push({ id, type, message })
  window.setTimeout(() => {
    const index = state.toasts.findIndex((t) => t.id === id)
    if (index >= 0) state.toasts.splice(index, 1)
  }, duration)
}

export function useToast() {
  return {
    state,
    success: (message) => push('success', message),
    error: (message) => push('error', message),
    warn: (message) => push('warn', message),
    info: (message) => push('info', message)
  }
}
