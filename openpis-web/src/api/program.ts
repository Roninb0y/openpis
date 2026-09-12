import request from './request'
import type { PageResult } from './material'

export interface BindingRequest {
  moduleId: string
  playlistId: number | null
}

export interface ProgramListItem {
  id: number
  name: string
  layoutId: number
  layoutName: string
  playType: string
  moduleCount: number
  createdAt: string
}

export interface ProgramModuleBinding {
  moduleId: string
  moduleName: string
  moduleType: string
  playlistId: number | null
  playlistName: string | null
  playlistType: string | null
  itemCount: number | null
}

export interface ProgramDetail {
  id: number
  name: string
  layoutId: number
  layoutName: string
  playType: string
  width: number | null
  height: number | null
  modules: ProgramModuleBinding[]
  createdAt: string
}

export const getProgramList = (params: { page?: number; size?: number; keyword?: string }) => {
  return request.get('/programs', { params })
}

export const createProgram = (data: {
  name: string
  layoutId: number
  playType: string
  modulePlaylists: BindingRequest[]
}) => {
  return request.post('/programs', data)
}

export const getProgramDetail = (id: number) => {
  return request.get(`/programs/${id}`)
}

export const updateProgram = (
  id: number,
  data: {
    name: string
    layoutId: number
    playType: string
    modulePlaylists: BindingRequest[]
  }
) => {
  return request.put(`/programs/${id}`, data)
}

export const deleteProgram = (id: number) => {
  return request.delete(`/programs/${id}`)
}

export type { PageResult }
