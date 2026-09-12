import request from './request'
import type { PageResult } from './material'

export interface ModuleConfig {
  id: string
  name: string
  type: 'video' | 'image' | 'text' | 'flash'
  x: number
  y: number
  w: number
  h: number
  zIndex: number
}

export interface LayoutListItem {
  id: number
  name: string
  resolution: string
  width: number
  height: number
  moduleCount: number
  createdAt: string
}

export interface LayoutDetail {
  id: number
  name: string
  resolution: string
  width: number
  height: number
  modules: ModuleConfig[]
  createdAt: string
}

export const getLayoutList = (params: { page?: number; size?: number; keyword?: string }) => {
  return request.get('/layouts', { params })
}

export const createLayout = (data: {
  name: string
  resolution: string
  width: number
  height: number
  modules: ModuleConfig[]
}) => {
  return request.post('/layouts', data)
}

export const getLayoutDetail = (id: number) => {
  return request.get(`/layouts/${id}`)
}

export const updateLayout = (
  id: number,
  data: {
    name: string
    resolution: string
    width: number
    height: number
    modules: ModuleConfig[]
  }
) => {
  return request.put(`/layouts/${id}`, data)
}

export const deleteLayout = (id: number) => {
  return request.delete(`/layouts/${id}`)
}

export type { PageResult }
