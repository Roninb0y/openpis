import request from './request'
import type { PageResult } from './material'

export interface PlaylistItemRequest {
  materialId: number
  sortOrder: number
  duration?: number | null
}

export interface PlaylistListItem {
  id: number
  name: string
  type: string
  itemCount: number
  createdAt: string
}

export interface PlaylistItemDetail {
  id: number
  materialId: number
  materialTitle: string | null
  materialType: string | null
  materialFilePath: string | null
  materialThumbnail: string | null
  materialDuration: number | null
  sortOrder: number
  duration: number | null
}

export interface PlaylistDetail {
  id: number
  name: string
  type: string
  items: PlaylistItemDetail[]
  createdAt: string
}

export const getPlaylistList = (params: {
  page?: number
  size?: number
  type?: string
  keyword?: string
}) => {
  return request.get('/playlists', { params })
}

export const createPlaylist = (data: { name: string; type: string; items: PlaylistItemRequest[] }) => {
  return request.post('/playlists', data)
}

export const getPlaylistDetail = (id: number) => {
  return request.get(`/playlists/${id}`)
}

export const updatePlaylist = (
  id: number,
  data: { name: string; type: string; items: PlaylistItemRequest[] }
) => {
  return request.put(`/playlists/${id}`, data)
}

export const deletePlaylist = (id: number) => {
  return request.delete(`/playlists/${id}`)
}

export type { PageResult }
