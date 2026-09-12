<template>
  <div class="player-screen" ref="screenRef">
    <!-- 有计划：按布局渲染模块 -->
    <div
      v-if="current && current.modules.length > 0"
      class="stage"
      :style="stageStyle"
    >
      <div
        v-for="mod in current.modules"
        :key="currentKey + '-' + mod.moduleId"
        class="module-box"
        :style="moduleBoxStyle(mod)"
      >
        <ModulePlayer
          :module="mod"
          :device-code="deviceCode"
          :program-id="current.programId"
        />
      </div>
    </div>

    <!-- 无计划 -->
    <div v-else class="empty-state">
      <div class="empty-icon">📡</div>
      <div class="empty-text">当前无播放计划</div>
      <div class="empty-sub">设备编码：{{ deviceCode }}｜等待计划下发中…</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getPlayerCurrent } from '@/api/player'
import type { PlayerCurrent, PlayerModule } from '@/api/player'
import { resolveDeviceCode } from '@/utils/device'
import { sendHeartbeat } from '@/api/player'
import ModulePlayer from './components/ModulePlayer.vue'

const route = useRoute()
const screenRef = ref()

const deviceCode = resolveDeviceCode(route.query.code as string)

const current = ref<PlayerCurrent | null>(null)
const currentKey = ref('')
const windowSize = ref({ w: window.innerWidth, h: window.innerHeight })

let pollTimer: number | null = null
let heartbeatTimer: number | null = null
let ws: WebSocket | null = null
let wsRetryTimer: number | null = null

// ========== 布局渲染 ==========

const scale = computed(() => {
  if (!current.value) return 1
  return Math.min(
    windowSize.value.w / current.value.layoutWidth,
    windowSize.value.h / current.value.layoutHeight
  )
})

const stageStyle = computed(() => {
  if (!current.value) return {}
  return {
    width: current.value.layoutWidth * scale.value + 'px',
    height: current.value.layoutHeight * scale.value + 'px'
  }
})

const moduleBoxStyle = (mod: PlayerModule) => ({
  left: (mod.x || 0) * scale.value + 'px',
  top: (mod.y || 0) * scale.value + 'px',
  width: (mod.w || 0) * scale.value + 'px',
  height: (mod.h || 0) * scale.value + 'px',
  zIndex: mod.zIndex || 1
})

// ========== 计划加载 ==========

const fetchCurrent = async () => {
  try {
    const res: any = await getPlayerCurrent(deviceCode)
    const data: PlayerCurrent | null = res.data
    if (!data) {
      current.value = null
      currentKey.value = ''
      return
    }
    const newKey = `${data.planId}-${data.programId}-${data.layoutWidth}x${data.layoutHeight}`
    if (newKey !== currentKey.value) {
      current.value = data
      currentKey.value = newKey
    } else {
      // 同一栏目：仅静默更新数据引用，不打断播放
      current.value = data
    }
  } catch (e) {
    // 网络异常时保留当前播放内容
  }
}

// ========== WebSocket（计划变更推送，轮询作降级） ==========

const connectWebSocket = () => {
  try {
    // 从 API 地址推导 WebSocket 地址：http://localhost:8080/api → ws://localhost:8080
    const apiBase: string =
      (import.meta as any).env?.VITE_API_BASE_URL || '/api'
    const wsBase = apiBase.replace(/^http/, 'ws').replace(/\/api\/?$/, '')
    const proto = location.protocol === 'https:' ? 'wss' : 'ws'
    const base = wsBase.startsWith('ws') ? wsBase : `${proto}://${location.host}`
    ws = new WebSocket(`${base}/ws/player?code=${deviceCode}`)
    ws.onmessage = (event) => {
      if (event.data.includes('PLAN_CHANGED')) {
        fetchCurrent()
      }
    }
    ws.onclose = () => {
      wsRetryTimer = window.setTimeout(connectWebSocket, 5000)
    }
    ws.onerror = () => ws?.close()
  } catch (e) {
    // WebSocket 不可用时依赖轮询
  }
}

const startHeartbeat = () => {
  sendHeartbeat(deviceCode).catch(() => {})
  heartbeatTimer = window.setInterval(() => {
    sendHeartbeat(deviceCode).catch(() => {})
  }, 60000)
}

const onResize = () => {
  windowSize.value = { w: window.innerWidth, h: window.innerHeight }
}

onMounted(() => {
  fetchCurrent()
  pollTimer = window.setInterval(fetchCurrent, 30000)
  startHeartbeat()
  connectWebSocket()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
  if (heartbeatTimer) clearInterval(heartbeatTimer)
  if (wsRetryTimer) clearTimeout(wsRetryTimer)
  window.removeEventListener('resize', onResize)
  ws?.close()
})
</script>

<style scoped>
.player-screen {
  width: 100vw;
  height: 100vh;
  background: #000;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  position: fixed;
  top: 0;
  left: 0;
}

.stage {
  position: relative;
  background: #111;
}

.module-box {
  position: absolute;
  overflow: hidden;
  background: #222;
}

.empty-state {
  text-align: center;
  color: #666;
}

.empty-icon {
  font-size: 64px;
}

.empty-text {
  font-size: 24px;
  color: #999;
  margin-top: 16px;
}

.empty-sub {
  font-size: 14px;
  color: #555;
  margin-top: 8px;
}
</style>
