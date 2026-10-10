import { defineStore } from 'pinia'

const TOKEN_KEY = 'campus_order_token'
const USER_KEY = 'campus_order_user'

/** 角色取值与后端 user.role 保持一致 */
export const ROLE = {
  STUDENT: 0,
  MERCHANT: 1,
  ADMIN: 2
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    role: (state) => state.user?.role ?? null,
    isStudent: (state) => state.user?.role === ROLE.STUDENT,
    isMerchant: (state) => state.user?.role === ROLE.MERCHANT,
    isAdmin: (state) => state.user?.role === ROLE.ADMIN,
    displayName: (state) => state.user?.realName || state.user?.username || '未登录'
  },
  actions: {
    setSession(loginResult) {
      this.token = loginResult.token
      this.user = loginResult.user
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    setUser(user) {
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
