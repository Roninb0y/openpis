import request from './request'

export interface Material {
  id: number
  title: string
  type: 'image' | 'video' | 'text'
  filePath: string
  thumbnail: string | null
  duration: number | null
  description: string | null
  status: number
  createdAt: string
}

export interface PageResult<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export const uploadMaterial = (file: File, description?: string, duration?: number) => {
  const formData = new FormData()
  formData.append('file', file)
  if (description) formData.append('description', description)
  if (duration && duration > 0) formData.append('duration', String(duration))
  return request.post('/materials/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const getMaterialList = (params: {
  page?: number
  size?: number
  type?: string
  keyword?: string
}) => {
  return request.get('/materials', { params })
}

export const updateMaterial = (id: number, data: { title?: string; description?: string; duration?: number }) => {
  return request.put(`/materials/${id}`, data)
}

export const deleteMaterial = (id: number) => {
  return request.delete(`/materials/${id}`)
}
