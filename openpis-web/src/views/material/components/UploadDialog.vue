<template>
  <el-dialog v-model="visible" title="上传媒体" width="520px" :close-on-click-modal="false">
    <el-form :model="form" label-position="top">
      <el-form-item label="选择文件">
        <el-upload
          ref="uploadRef"
          action="#"
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          drag
        >
          <el-button type="primary">选择文件</el-button>
          <template #tip>
            <div class="el-upload__tip">
              支持图片（jpg/png/gif）、视频（mp4/mov/avi）、文本（txt）；图片≤10MB、视频≤200MB
            </div>
          </template>
        </el-upload>
      </el-form-item>

      <el-form-item v-if="isVideo" label="视频时长（秒）">
        <el-input-number v-model="form.duration" :min="1" :max="86400" placeholder="请填写视频时长" />
        <span class="tip-text">MVP 暂不自动解析视频时长，请填写实际时长，播放器将按此节奏播放</span>
      </el-form-item>

      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入描述" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadMaterial } from '@/api/material'

const props = defineProps<{ visible: boolean }>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const uploadRef = ref()
const loading = ref(false)
const selectedFile = ref<File | null>(null)

const form = reactive({
  description: '',
  duration: 60
})

const VIDEO_EXTS = ['mp4', 'avi', 'mov', 'mkv', 'flv']

const isVideo = computed(() => {
  if (!selectedFile.value) return false
  const name = selectedFile.value.name.toLowerCase()
  return VIDEO_EXTS.some((ext) => name.endsWith('.' + ext))
})

const handleFileChange = (file: any) => {
  selectedFile.value = file.raw
}

const handleSubmit = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  loading.value = true
  try {
    await uploadMaterial(
      selectedFile.value,
      form.description,
      isVideo.value ? form.duration : undefined
    )
    ElMessage.success('上传成功')
    emit('success')
    visible.value = false
    resetForm()
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  selectedFile.value = null
  form.description = ''
  form.duration = 60
  uploadRef.value?.clearFiles()
}
</script>

<style scoped>
.tip-text {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
