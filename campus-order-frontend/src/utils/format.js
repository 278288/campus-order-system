/**
 * 前端展示用格式化与常量工具。
 */

/** 订单状态：取值与后端 OrderStatus 完全一致 */
export const ORDER_STATUS = {
  PENDING: 0,
  PREPARING: 1,
  READY: 2,
  FINISHED: 3,
  CANCELED: 4,
  REJECTED: 5
}

export const ORDER_STATUS_TEXT = {
  0: '待接单',
  1: '备餐中',
  2: '待取餐',
  3: '已完成',
  4: '已取消',
  5: '已拒绝'
}

export const ORDER_STATUS_OPTIONS = [
  { value: 0, label: '待接单' },
  { value: 1, label: '备餐中' },
  { value: 2, label: '待取餐' },
  { value: 3, label: '已完成' },
  { value: 4, label: '已取消' },
  { value: 5, label: '已拒绝' }
]

export const ROLE_TEXT = {
  0: '师生用户',
  1: '食堂商家',
  2: '管理员'
}

export const ROLE_OPTIONS = [
  { value: 0, label: '师生用户' },
  { value: 1, label: '食堂商家' },
  { value: 2, label: '管理员' }
]

export function money(value) {
  const num = Number(value ?? 0)
  return `¥${num.toFixed(2)}`
}

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

/**
 * 生成客户端唯一请求号，用于下单与取消的幂等控制。
 * 失败重试时必须复用同一个请求号，因此页面需要把请求号保存在订单提交上下文中。
 */
export function newRequestNo(prefix = 'req') {
  const random = Math.random().toString(36).slice(2, 10)
  return `${prefix}-${Date.now()}-${random}`
}

export function todayString(offsetDays = 0) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}
