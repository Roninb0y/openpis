import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/index.vue')
    },
    {
      path: '/player',
      name: 'Player',
      component: () => import('@/views/player/index.vue'),
      meta: { standalone: true }
    },
    {
      path: '/dashboard',
      component: () => import('@/views/dashboard/index.vue'),
      meta: { requiresAuth: true },
      redirect: '/dashboard/home',
      children: [
        {
          path: 'home',
          name: 'Home',
          component: () => import('@/views/dashboard/Home.vue')
        },
        {
          path: 'material',
          name: 'Material',
          component: () => import('@/views/material/index.vue')
        },
        {
          path: 'playlist',
          name: 'Playlist',
          component: () => import('@/views/playlist/index.vue')
        },
        {
          path: 'playlist/create',
          name: 'PlaylistCreate',
          component: () => import('@/views/playlist/PlaylistForm.vue')
        },
        {
          path: 'playlist/edit/:id',
          name: 'PlaylistEdit',
          component: () => import('@/views/playlist/PlaylistForm.vue')
        },
        {
          path: 'layout',
          name: 'Layout',
          component: () => import('@/views/layout/index.vue')
        },
        {
          path: 'layout/create',
          name: 'LayoutCreate',
          component: () => import('@/views/layout/LayoutEditor.vue')
        },
        {
          path: 'layout/edit/:id',
          name: 'LayoutEdit',
          component: () => import('@/views/layout/LayoutEditor.vue')
        },
        {
          path: 'program',
          name: 'Program',
          component: () => import('@/views/program/index.vue')
        },
        {
          path: 'program/create',
          name: 'ProgramCreate',
          component: () => import('@/views/program/ProgramForm.vue')
        },
        {
          path: 'program/edit/:id',
          name: 'ProgramEdit',
          component: () => import('@/views/program/ProgramForm.vue')
        },
        {
          path: 'plan',
          name: 'Plan',
          component: () => import('@/views/plan/index.vue')
        },
        {
          path: 'plan/create',
          name: 'PlanCreate',
          component: () => import('@/views/plan/PlanForm.vue')
        },
        {
          path: 'plan/edit/:id',
          name: 'PlanEdit',
          component: () => import('@/views/plan/PlanForm.vue')
        },
        {
          path: 'report',
          name: 'Report',
          component: () => import('@/views/report/index.vue')
        }
      ]
    },
    {
      path: '/',
      redirect: '/dashboard'
    }
  ]
})

// 路由守卫：播放端和登录页不需要认证
router.beforeEach((to, _from, next) => {
  if (to.meta.standalone) {
    next()
    return
  }
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.isLoggedIn()) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn()) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
