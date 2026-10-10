import { createRouter, createWebHashHistory } from 'vue-router'
import { useAuthStore, ROLE } from '@/stores/auth'
import { useToast } from '@/utils/toast'

/**
 * 路由表。meta.roles 用于前端导航层面的角色隔离；
 * 真正的权限控制始终由后端拦截器与 Service 校验，前端守卫只提升体验。
 */
const routes = [
  { path: '/', redirect: '/dishes' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录 / 注册' }
  },
  {
    path: '/dishes',
    name: 'dishes',
    component: () => import('@/views/DishBrowseView.vue'),
    meta: { public: true, title: '选餐' }
  },
  {
    path: '/cart',
    name: 'cart',
    component: () => import('@/views/CartView.vue'),
    meta: { title: '购物车', roles: [ROLE.STUDENT] }
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('@/views/OrderListView.vue'),
    meta: { title: '我的订单', roles: [ROLE.STUDENT] }
  },
  {
    path: '/orders/:orderNo',
    name: 'order-detail',
    component: () => import('@/views/OrderDetailView.vue'),
    meta: { title: '订单详情' }
  },
  {
    path: '/merchant/orders',
    name: 'merchant-orders',
    component: () => import('@/views/merchant/MerchantOrderView.vue'),
    meta: { title: '接单与备餐', roles: [ROLE.MERCHANT] }
  },
  {
    path: '/merchant/dishes',
    name: 'merchant-dishes',
    component: () => import('@/views/merchant/MerchantDishView.vue'),
    meta: { title: '菜品维护', roles: [ROLE.MERCHANT] }
  },
  {
    path: '/merchant/pickup',
    name: 'merchant-pickup',
    component: () => import('@/views/merchant/MerchantPickupView.vue'),
    meta: { title: '核码取餐', roles: [ROLE.MERCHANT] }
  },
  {
    path: '/merchant/stats',
    name: 'merchant-stats',
    component: () => import('@/views/merchant/MerchantStatsView.vue'),
    meta: { title: '窗口统计', roles: [ROLE.MERCHANT] }
  },
  {
    path: '/admin/users',
    name: 'admin-users',
    component: () => import('@/views/admin/AdminUserView.vue'),
    meta: { title: '账号与角色', roles: [ROLE.ADMIN] }
  },
  {
    path: '/admin/windows',
    name: 'admin-windows',
    component: () => import('@/views/admin/AdminWindowView.vue'),
    meta: { title: '窗口管理', roles: [ROLE.ADMIN] }
  },
  {
    path: '/admin/orders',
    name: 'admin-orders',
    component: () => import('@/views/admin/AdminOrderView.vue'),
    meta: { title: '全平台订单', roles: [ROLE.ADMIN] }
  },
  {
    path: '/admin/stats',
    name: 'admin-stats',
    component: () => import('@/views/admin/AdminStatsView.vue'),
    meta: { title: '平台统计', roles: [ROLE.ADMIN] }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('@/views/ProfileView.vue'),
    meta: { title: '个人资料' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const toast = useToast()

  if (to.meta.public) {
    return true
  }
  if (!auth.isLoggedIn) {
    toast.warn('请先登录后再访问该页面')
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (Array.isArray(to.meta.roles) && to.meta.roles.length > 0 && !to.meta.roles.includes(auth.role)) {
    toast.error('当前角色没有访问该页面的权限')
    return { name: 'dishes' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园点餐系统` : '校园点餐系统'
})

export default router
