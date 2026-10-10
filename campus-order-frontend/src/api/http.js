import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'

/**
 * Axios 实例：统一携带访问令牌，统一解包后端 Result 结构。
 *
 * 后端统一返回：{ code, message, data, timestamp }
 * code = 200 视为成功，此时直接把 data 交给调用方；
 * 其余情况抛出携带业务码与提示信息的 Error，由页面决定如何展示。
 */
const http = axios.create({
  baseURL: '/',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      const error = new Error(body.message || '请求失败')
      error.code = body.code
      error.business = true
      return Promise.reject(error)
    }
    return body
  },
  (error) => {
    const auth = useAuthStore()
    const toast = useToast()
    const status = error.response?.status
    const body = error.response?.data
    const message = body?.message || error.message || '网络异常，请稍后重试'

    if (status === 401 || (body && body.code === 401)) {
      auth.clear()
      toast.warn('登录已过期，请重新登录')
      if (location.hash !== '#/login') {
        location.hash = '#/login'
      }
    } else if (status === 403 || (body && body.code === 403)) {
      toast.error(message || '没有操作权限')
    } else if (!error.response) {
      toast.error('无法连接后端服务，请确认后端已在 8088 端口启动')
    } else {
      toast.error(message)
    }
    const wrapped = new Error(message)
    wrapped.code = body?.code ?? status
    return Promise.reject(wrapped)
  }
)

export default http
