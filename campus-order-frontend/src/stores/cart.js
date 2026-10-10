import { defineStore } from 'pinia'

const CART_KEY = 'campus_order_cart'

/**
 * 购物车状态。
 *
 * 业务规则与后端设计保持一致：
 * 1. 购物车保存在客户端，加车不锁库存，下单时由服务端重新校验价格与库存；
 * 2. 一个购物车只包含同一窗口的菜品，更换窗口需要先清空；
 * 3. 预计金额只作参考，不能作为成交金额。
 */
export const useCartStore = defineStore('cart', {
  state: () => ({
    windowId: null,
    windowName: '',
    items: JSON.parse(localStorage.getItem(CART_KEY) || '[]')
  }),
  getters: {
    itemCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: (state) =>
      state.items.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0),
    isEmpty: (state) => state.items.length === 0
  },
  actions: {
    persist() {
      localStorage.setItem(
        CART_KEY,
        JSON.stringify({ windowId: this.windowId, windowName: this.windowName, items: this.items })
      )
    },    /**
     * 加入购物车。返回 { ok, message } 供页面提示。
     */
    add(dish, quantity = 1) {
      if (this.windowId !== null && this.windowId !== dish.windowId) {
        return {
          ok: false,
          needClear: true,
          message: `购物车中已有「${this.windowName}」的菜品，请先清空后再选择该窗口`
        }
      }
      const existing = this.items.find((item) => item.dishId === dish.id)
      const maxStock = dish.stock ?? 0
      if (existing) {
        if (existing.quantity + quantity > maxStock) {
          return { ok: false, message: `库存仅剩 ${maxStock} 份，无法继续增加` }
        }
        existing.quantity += quantity
      } else {
        if (quantity > maxStock) {
          return { ok: false, message: `库存仅剩 ${maxStock} 份` }
        }
        this.items.push({
          dishId: dish.id,
          dishName: dish.dishName,
          price: Number(dish.price),
          quantity,
          windowId: dish.windowId,
          stock: maxStock
        })
      }
      this.windowId = dish.windowId
      this.windowName = dish.windowName || ''
      this.persist()
      return { ok: true, message: '已加入购物车' }
    },
    updateQuantity(dishId, quantity) {
      const item = this.items.find((i) => i.dishId === dishId)
      if (!item) return { ok: false, message: '菜品不在购物车中' }
      if (quantity <= 0) {
        this.remove(dishId)
        return { ok: true, message: '已移除' }
      }
      if (quantity > item.stock) {
        return { ok: false, message: `库存仅剩 ${item.stock} 份` }
      }
      item.quantity = quantity
      this.persist()
      return { ok: true, message: '已更新数量' }
    },
    remove(dishId) {
      this.items = this.items.filter((i) => i.dishId !== dishId)
      if (this.items.length === 0) {
        this.windowId = null
        this.windowName = ''
      }
      this.persist()
    },
    clear() {
      this.items = []
      this.windowId = null
      this.windowName = ''
      this.persist()
    },
    /** 下单成功后清空购物车 */
    clearAfterOrder() {
      this.clear()
    }
  }
})
