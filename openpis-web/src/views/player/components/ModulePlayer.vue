<template>
  <div class="module-player">
    <!-- 未绑定播表 -->
    <div v-if="!playlist || items.length === 0" class="placeholder">
      <span>{{ moduleName }}（未绑定播表）</span>
    </div>

    <!-- 视频素材 -->
    <video
      v-else-if="currentItem && currentItem.materialType === 'video'"
      :key="itemIndex + '-' + currentItem.url"
      :src="currentItem.url"
      autoplay
      muted
      playsinline
      @ended="nextItem"
      @error="nextItem"
    ></video>

    <!-- 图片素材 -->
    <img
      v-else-if="currentItem && currentItem.materialType === 'image'"
      :key="itemIndex + '-' + currentItem.url"
      :src="currentItem.url"
      @error="nextItem"
    />

    <!-- 字幕素材 -->
    <div v-else-if="currentItem && currentItem.materialType === 'text'" class="text-scroll-wrap">
      <div class="text-scroll" :class="{ animate: textContent }">{{ textContent }}</div>
    </div>

    <!-- 未知素材类型 -->
    <div v-else class="placeholder"><span>暂无内容</span></div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { PlayerModule, PlayerPlaylistItem } from '@/api/player'
import { reportPlayLog } from '@/api/player'
import { formatLocalDateTime } from '@/utils/device'

const props = defineProps<{
  module: PlayerModule
  deviceCode: string
  programId: number
}>()

const itemIndex = ref(0)
const textContent = ref('')
let timer: number | null = null
let pendingLog: { materialId: number; startedAt: string } | null = null

const playlist = computed(() => props.module.playlist)
const items = computed(() => playlist.value?.items || [])
const currentItem = computed<PlayerPlaylistItem | null>(() => items.value[itemIndex.value] || null)
const moduleName = computed(() => props.module.name || props.module.moduleId)

// ========== 播放日志 ==========

const flushLog = () => {
  if (!pendingLog) return
  reportPlayLog([
    {
      deviceCode: props.deviceCode,
      programId: props.programId || null,
      materialId: pendingLog.materialId,
      startedAt: pendingLog.startedAt,
      endedAt: formatLocalDateTime()
    }
  ]).catch(() => {})
  pendingLog = null
}

const markStarted = (item: PlayerPlaylistItem) => {
  flushLog()
  pendingLog = { materialId: item.materialId, startedAt: formatLocalDateTime() }
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
  flushLog()
  itemIndex.value = (itemIndex.value + 1) % items.value.length
}

// 每当当前素材变化时：图片/字幕用定时器推进，视频由 ended 事件推进
watch(
  currentItem,
  async (item) => {
    clearTimer()
    textContent.value = ''
    if (!item) return
    markStarted(item)

    if (item.materialType === 'image') {
      scheduleNext(item.duration > 0 ? item.duration : 10)
    } else if (item.materialType === 'text') {
      try {
        const res = await fetch(item.url)
        textContent.value = await res.text()
      } catch (e) {
        textContent.value = item.title
      }
      scheduleNext(item.duration > 0 ? item.duration : 30)
    }
    // video：由 <video> 的 ended/error 事件推进
  },
  { immediate: true }
)

// 播表内容变化（如换栏目）时重置
watch(
  () => props.module.moduleId,
  () => {
    itemIndex.value = 0
  }
)

onUnmounted(() => {
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
