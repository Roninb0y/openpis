import request from './request'

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  role: string
}

export const login = (data: LoginRequest) => {
  return request.post('/auth/login', data)
}
