import http from './http'

/** 认证与用户接口 */
export const authApi = {
  register: (payload) => http.post('/api/auth/register', payload),
  login: (payload) => http.post('/api/auth/login', payload),
  me: () => http.get('/api/users/me'),
  updateMe: (payload) => http.put('/api/users/me', payload),
  pageUsers: (params) => http.get('/api/users', { params }),
  assignRole: (id, payload) => http.put(`/api/users/${id}/role`, payload)
}

/** 只读公开接口：未登录也可浏览食堂窗口与在售菜品 */
export const publicApi = {
  windows: (params) => http.get('/api/public/windows', { params }),
  windowDetail: (id) => http.get(`/api/public/windows/${id}`),
  dishes: (params) => http.get('/api/public/dishes', { params }),
  dishDetail: (id) => http.get(`/api/public/dishes/${id}`),
  categories: () => http.get('/api/public/dishes/categories')
}

/** 食堂窗口管理接口（需登录，维护类操作仅管理员） */
export const windowApi = {
  list: (params) => http.get('/api/windows', { params }),
  detail: (id) => http.get(`/api/windows/${id}`),
  create: (payload) => http.post('/api/windows', payload),
  update: (id, payload) => http.put(`/api/windows/${id}`, payload),
  changeBusinessStatus: (id, businessStatus) =>
    http.put(`/api/windows/${id}/business-status`, null, { params: { businessStatus } }),
  changeStatus: (id, status) => http.put(`/api/windows/${id}/status`, null, { params: { status } })
}

/** 菜品接口：/api/dishes/mine 与写操作仅商家可调用 */
export const dishApi = {
  mine: (params) => http.get('/api/dishes/mine', { params }),
  create: (payload) => http.post('/api/dishes', payload),
  update: (id, payload) => http.put(`/api/dishes/${id}`, payload),
  changeStatus: (id, status) => http.put(`/api/dishes/${id}/status`, null, { params: { status } }),
  updateStock: (id, stock) => http.put(`/api/dishes/${id}/stock`, null, { params: { stock } })
}

/** 兼容旧调用：浏览类接口统一指向只读公开命名空间 */
export const browseApi = {
  windows: (params) => publicApi.windows(params),
  dishes: (params) => publicApi.dishes(params),
  dishDetail: (id) => publicApi.dishDetail(id),
  categories: () => publicApi.categories()
}

/** 订单接口 */
export const orderApi = {
  create: (payload) => http.post('/api/orders', payload),
  page: (params) => http.get('/api/orders', { params }),
  detail: (orderNo) => http.get(`/api/orders/${orderNo}`),
  cancel: (orderNo, reason) => http.put(`/api/orders/${orderNo}/cancel`, { reason }),
  updateStatus: (orderNo, payload) => http.put(`/api/orders/${orderNo}/status`, payload),
  verifyPickup: (orderNo, pickupCode) =>
    http.put(`/api/orders/${orderNo}/verify-pickup`, null, { params: { pickupCode } }),
  rate: (orderNo, payload) => http.put(`/api/orders/${orderNo}/rating`, payload)
}

/** 统计接口 */
export const statsApi = {
  orders: (params) => http.get('/api/stats/orders', { params })
}
