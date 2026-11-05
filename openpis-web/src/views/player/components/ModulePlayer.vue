<template>
  <div class="module-player">
    <!-- 未绑定播表 -->
    <div v-if="!playlist || items.length === 0" class="placeholder">
      <span>{{ moduleName }}（未绑定播表）</span>
    </div>

    <!-- 视频素材 -->
    <video
      v-else-if="currentItem && currentItem.materialType === 'video'"
      :key="cycle"
      :src="currentItem.url"
      autoplay
      muted
      playsinline
      @ended="nextItem"
      @error="onMediaError"
    ></video>

    <!-- 图片素材 -->
    <img
      v-else-if="currentItem && currentItem.materialType === 'image'"
      :key="cycle"
      :src="currentItem.url"
      @error="onMediaError"
    />

    <!-- 字幕素材 -->
    <div v-else-if="currentItem && currentItem.materialType === 'text'" class="text-scroll-wrap">
      <div :key="cycle" class="text-scroll" :class="{ animate: textContent }">{{ textContent }}</div>
    </div>

    <!-- 未知素材类型 -->
    <div v-else class="placeholder"><span>暂无内容</span></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onUnmounted } from 'vue'
import type { PlayerModule, PlayerPlaylistItem } from '@/api/player'
import { reportPlayLog } from '@/api/player'
import { formatLocalDateTime } from '@/utils/device'

const props = defineProps<{
  module: PlayerModule
  deviceCode: string
  programId: number
}>()

const itemIndex = ref(0)
const cycle = ref(0)
const textContent = ref('')
let timer: number | null = null
let pendingLog: { materialId: number; programId: number; startedAt: string } | null = null

const playlist = computed(() => props.module.playlist)
const items = computed(() => playlist.value?.items || [])
const currentItem = computed<PlayerPlaylistItem | null>(() => items.value[itemIndex.value] || null)
const moduleName = computed(() => props.module.name || props.module.moduleId)

// 轮询会返回新的对象；仅播放内容变化才重置，位置、尺寸和名称变化不打断播放。
const playlistKey = computed(() => JSON.stringify([
  props.module.moduleId,
  props.programId,
  playlist.value?.id,
  items.value.map(item => [item.materialId, item.materialType, item.url, item.duration, item.title])
]))

// ========== 播放日志 ==========

const flushLog = () => {
  if (!pendingLog) return
  reportPlayLog([
    {
      deviceCode: props.deviceCode,
      programId: pendingLog.programId || null,
      materialId: pendingLog.materialId,
      startedAt: pendingLog.startedAt,
      endedAt: formatLocalDateTime()
    }
  ]).catch(() => {})
  pendingLog = null
}

const markStarted = (item: PlayerPlaylistItem) => {
  flushLog()
  pendingLog = {
    materialId: item.materialId,
    programId: props.programId,
    startedAt: formatLocalDateTime()
  }
}

// ========== 播放循环 ==========

const clearTimer = () => {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

const scheduleNext = (seconds: number) => {
  clearTimer()
  if (seconds <= 0) return
  timer = window.setTimeout(() => nextItem(), seconds * 1000)
}

const nextItem = () => {
  if (items.value.length === 0) return
  itemIndex.value = (itemIndex.value + 1) % items.value.length
  // 单素材时索引不变，也必须显式启动下一轮（视频需要重新挂载）。
  startCurrentItem()
}

// 损坏素材延迟一秒再切换，避免单素材或全部损坏时产生紧密请求循环。
const onMediaError = () => scheduleNext(1)

const startCurrentItem = async () => {
  clearTimer()
  flushLog()
  const activeCycle = ++cycle.value
  textContent.value = ''
  const item = currentItem.value
  if (!item) return
  markStarted(item)

  if (item.materialType === 'image') {
    scheduleNext(item.duration > 0 ? item.duration : 10)
  } else if (item.materialType === 'text') {
    let content: string
    try {
      const res = await fetch(item.url)
      if (!res.ok) throw new Error('字幕加载失败')
      content = await res.text()
    } catch (e) {
      content = item.title
    }
    // 播表切换或组件卸载后，旧请求不得覆盖内容或重新创建计时器。
    if (activeCycle !== cycle.value) return
    textContent.value = content
    scheduleNext(item.duration > 0 ? item.duration : 30)
  }
  // video：由 <video> 的 ended/error 事件推进，保留自然播完的行为。
}

watch(
  playlistKey,
  () => {
    itemIndex.value = 0
    startCurrentItem()
  },
  { immediate: true }
)

onUnmounted(() => {
  cycle.value++
  clearTimer()
  flushLog()
})
</script>

<style scoped>
.module-player {
  width: 100%;
  height: 100%;
  overflow: hidden;
  background: #000;
}

.module-player video,
.module-player img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #555;
  font-size: 14px;
  background: #2b2b2b;
}

.text-scroll-wrap {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  overflow: hidden;
  background: #111;
}

.text-scroll {
  white-space: nowrap;
  color: #ffd04b;
  font-size: 22px;
  padding-left: 100%;
}

.text-scroll.animate {
  animation: marquee linear infinite;
  animation-duration: calc(30s);
}

@keyframes marquee {
  0% {
    transform: translateX(0);
  }
  100% {
    transform: translateX(-200%);
  }
}
</style>
