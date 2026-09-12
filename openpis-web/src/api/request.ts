import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: (import.meta as any).env?.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 30000
})

const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('role')
  if (!location.pathname.startsWith('/login')) {
    location.href = '/login'
  }
}

// 请求拦截器：自动携带 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非 JSON 统一返回体（如文件流）直接返回
    if (res === null || typeof res !== 'object' || res.code === undefined) {
      return res
    }
    if (res.code !== 200) {
      if (res.code === 401) {
        ElMessage.error(res.message || '登录已过期')
        clearAuth()
      } else {
        ElMessage.error(res.message || '请求失败')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      clearAuth()
    } else {
      const message = error.response?.data?.message || '网络错误，请检查后端服务'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default request
