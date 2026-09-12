const DEVICE_CODE_KEY = 'openpis_device_code'

/**
 * 播放端设备编码：优先取 URL 参数，其次本地存储，最后生成 UUID
 */
export const resolveDeviceCode = (fromUrl?: string | null): string => {
  if (fromUrl) return fromUrl
  let code = localStorage.getItem(DEVICE_CODE_KEY)
  if (!code) {
    code = 'DEV-' + Math.random().toString(36).slice(2, 10).toUpperCase()
    localStorage.setItem(DEVICE_CODE_KEY, code)
  }
  return code
}

/**
 * 本地时间格式化 yyyy-MM-dd HH:mm:ss（供播放日志上报）
 */
export const formatLocalDateTime = (d: Date = new Date()): string => {
  const pad = (n: number) => String(n).padStart(2, '0')
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}
