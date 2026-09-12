import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref(localStorage.getItem('username') || '')
  const role = ref(localStorage.getItem('role') || '')

  const setUserInfo = (info: { token: string; username: string; role: string }) => {
    token.value = info.token
    username.value = info.username
    role.value = info.role
    localStorage.setItem('token', info.token)
    localStorage.setItem('username', info.username)
    localStorage.setItem('role', info.role)
  }

  const logout = () => {
    token.value = ''
    username.value = ''
    role.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
  }

  const isLoggedIn = () => !!token.value

  return { token, username, role, setUserInfo, logout, isLoggedIn }
})
