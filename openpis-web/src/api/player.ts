import request from './request'
import type { PageResult } from './material'

// ========== 播放端 ==========

export interface PlayerPlaylistItem {
  materialId: number
  title: string
  materialType: string
  url: string
  duration: number
}

export interface PlayerPlaylist {
  id: number
  name: string
  type: string
  items: PlayerPlaylistItem[]
}

export interface PlayerModule {
  moduleId: string
  name: string
  type: string
  x: number
  y: number
  w: number
  h: number
  zIndex: number
  playlist: PlayerPlaylist | null
}

export interface PlayerCurrent {
  planId: number
  planName: string
  programId: number
  programName: string
  playType: string
  layoutWidth: number
  layoutHeight: number
  modules: PlayerModule[]
}

export const getPlayerCurrent = (code: string, at?: string) => {
  return request.get('/player/current', { params: { code, at } })
}

export const reportPlayLog = (
  logs: Array<{
    deviceCode: string
    programId: number | null
    materialId: number
    startedAt: string
    endedAt: string
  }>
) => {
  return request.post('/player/log', logs)
}

export const sendHeartbeat = (code: string, name?: string) => {
  return request.post('/device/heartbeat', { code, name })
}

// ========== 报表 ==========

export const getReportDaily = (days = 7) => {
  return request.get('/report/daily', { params: { days } })
}

export const getReportProgram = () => {
  return request.get('/report/program')
}

export const getReportMaterial = (limit = 10) => {
  return request.get('/report/material', { params: { limit } })
}

export type { PageResult }
