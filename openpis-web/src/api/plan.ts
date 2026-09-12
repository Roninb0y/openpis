import request from './request'
import type { PageResult } from './material'

export interface ScheduleItem {
  programId: number
  dayOfWeek?: number | null
  startTime: string
  endTime: string
}

export interface PlanListItem {
  id: number
  name: string
  planType: 'daily' | 'weekly'
  startDate: string
  endDate: string
  status: number
  itemCount: number
  createdAt: string
  updatedAt: string
}

export interface ScheduleItemDetail extends ScheduleItem {
  programName: string
  playType: string
}

export interface PlanDetail {
  id: number
  name: string
  planType: 'daily' | 'weekly'
  startDate: string
  endDate: string
  status: number
  items: ScheduleItemDetail[]
  createdAt: string
  updatedAt: string
}

export const getPlanList = (params: {
  page?: number
  size?: number
  status?: number
  keyword?: string
}) => {
  return request.get('/plans', { params })
}

export const createPlan = (data: {
  name: string
  planType: string
  startDate: string
  endDate: string
  schedule: { items: ScheduleItem[] }
}) => {
  return request.post('/plans', data)
}

export const getPlanDetail = (id: number) => {
  return request.get(`/plans/${id}`)
}

export const updatePlan = (
  id: number,
  data: {
    name: string
    planType: string
    startDate: string
    endDate: string
    schedule: { items: ScheduleItem[] }
  }
) => {
  return request.put(`/plans/${id}`, data)
}

export const deletePlan = (id: number) => {
  return request.delete(`/plans/${id}`)
}

export const publishPlan = (id: number) => {
  return request.post(`/plans/${id}/publish`)
}

export const unpublishPlan = (id: number) => {
  return request.post(`/plans/${id}/unpublish`)
}

export type { PageResult }
