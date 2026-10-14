import { defineStore } from 'pinia'

const TOKEN_KEY = 'campus_order_token'
const USER_KEY = 'campus_order_user'
const ROLES_KEY = 'campus_order_roles'
const PERMS_KEY = 'campus_order_permissions'

/** 角色取值与后端 user.role 保持一致 */
export const ROLE = {
  STUDENT: 0,
  MERCHANT: 1,
  ADMIN: 2
}

/** 角色编码（后端 user.roleCode），与 Spring Security 的权限标识一致 */
export const ROLE_CODE = {
  STUDENT: 'ROLE_STUDENT',
  MERCHANT: 'ROLE_MERCHANT',
  ADMIN: 'ROLE_ADMIN'
}

/** 管理员通配权限：持有它即视为拥有一切权限 */
const ALL_PERMISSION = '*:*'

/** localStorage 读取 JSON，解析失败时兜底为空数组，避免脏数据导致白屏 */
function readArray(key) {
  try {
    const raw = JSON.parse(localStorage.getItem(key) || '[]')
    return Array.isArray(raw) ? raw : []
  } catch {
    return []
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    // 权限与角色随会话一起持久化，刷新页面后权限不丢
    roles: readArray(ROLES_KEY),
    permissions: readArray(PERMS_KEY)
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    role: (state) => state.user?.role ?? null,
    roleCode: (state) => state.user?.roleCode ?? '',
    isStudent: (state) => state.user?.role === ROLE.STUDENT,
    isMerchant: (state) => state.user?.role === ROLE.MERCHANT,
    isAdmin: (state) => state.user?.role === ROLE.ADMIN,
    displayName: (state) => state.user?.realName || state.user?.username || '未登录',
    /**
     * 权限判断：管理员通配符 *:* 直接放行；
     * 否则精确匹配，同时支持「模块:*」这种粗粒度通配。
     */
    hasPermission: (state) => (perm) => {
      if (!perm) return false
      const list = state.permissions || []
      if (list.includes(ALL_PERMISSION)) return true
      if (list.includes(perm)) return true
      const moduleName = String(perm).split(':')[0]
      return list.includes(`${moduleName}:*`)
    },
    /** 只要命中任意一个权限即返回 true */
    hasAnyPermission: (state) => (...perms) => {
      const list = (perms || []).flat().filter(Boolean)
      if (list.length === 0) return false
      return list.some((perm) => {
        const owned = state.permissions || []
        if (owned.includes(ALL_PERMISSION) || owned.includes(perm)) return true
        return owned.includes(`${String(perm).split(':')[0]}:*`)
      })
    },
    hasRoleCode: (state) => (code) => (state.roles || []).includes(code) || state.user?.roleCode === code
  },
  actions: {
    setSession(loginResult) {
      this.token = loginResult.token
      this.user = loginResult.user
      localStorage.setItem(TOKEN_KEY, this.token)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      // 登录响应同时带回 roles / permissions，一并落库
      this.setAuthorities({
        roles: loginResult.roles ?? loginResult.user?.roles,
        permissions: loginResult.permissions ?? loginResult.user?.permissions
      })
    },
    setUser(user) {
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    /** 保存当前账号的角色与权限标识（登录响应或 /api/users/me 的返回） */
    setAuthorities({ roles, permissions } = {}) {
      if (Array.isArray(roles)) {
        this.roles = roles
        localStorage.setItem(ROLES_KEY, JSON.stringify(this.roles))
      }
      if (Array.isArray(permissions)) {
        this.permissions = permissions
        localStorage.setItem(PERMS_KEY, JSON.stringify(this.permissions))
      }
    },
    clear() {
      this.token = ''
      this.user = null
      this.roles = []
      this.permissions = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      localStorage.removeItem(ROLES_KEY)
      localStorage.removeItem(PERMS_KEY)
    }
  }
})
