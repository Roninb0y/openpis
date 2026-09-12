export const FILE_BASE: string =
  (import.meta as any).env?.VITE_FILE_BASE_URL || 'http://localhost:8080/uploads'

/**
 * 把后端返回的相对路径拼接为可访问的 URL
 */
export const getFileUrl = (path?: string | null): string => {
  if (!path) return ''
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  return `${FILE_BASE}/${path.replace(/^\//, '').replace(/\\/g, '/')}`
}

/**
 * 秒数格式化为 mm:ss
 */
export const formatDuration = (seconds?: number | null): string => {
  if (!seconds || seconds <= 0) return '-'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return m > 0 ? `${m}分${s}秒` : `${s}秒`
}
