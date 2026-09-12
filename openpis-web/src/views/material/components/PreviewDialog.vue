<template>
  <el-dialog v-model="visible" title="媒体预览" width="800px">
    <div v-if="material" class="preview-content">
      <el-image
        v-if="material.type === 'image'"
        :src="getFileUrl(material.filePath)"
        fit="contain"
        style="width: 100%; max-height: 500px"
      />
      <video
        v-else-if="material.type === 'video'"
        :src="getFileUrl(material.filePath)"
        controls
        style="width: 100%; max-height: 500px"
      />
      <pre v-else-if="material.type === 'text'" class="text-preview">{{ textContent }}</pre>

      <div class="material-info">
        <p><strong>标题：</strong>{{ material.title }}</p>
        <p><strong>类型：</strong>{{ typeLabel(material.type) }}</p>
        <p v-if="material.type === 'video'">
          <strong>时长：</strong>{{ material.duration ? material.duration + ' 秒' : '未设置' }}
        </p>
        <p v-if="material.description"><strong>描述：</strong>{{ material.description }}</p>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Material } from '@/api/material'
import { getFileUrl } from '@/utils/file'

const props = defineProps<{
  visible: boolean
  material: Material | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const textContent = ref('')

const typeLabel = (t: string) => ({ image: '图片', video: '视频', text: '字幕' })[t] || t

watch(
  () => props.material,
  async (val) => {
    if (val && val.type === 'text') {
      textContent.value = '读取中...'
      try {
        const res = await fetch(getFileUrl(val.filePath))
        textContent.value = await res.text()
      } catch (e) {
        textContent.value = '读取文本失败'
      }
    }
  }
)
</script>

<style scoped>
.preview-content {
  text-align: center;
}

.text-preview {
  text-align: left;
  max-height: 400px;
  overflow: auto;
  white-space: pre-wrap;
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
}

.material-info {
  text-align: left;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.material-info p {
  margin: 8px 0;
}
</style>
