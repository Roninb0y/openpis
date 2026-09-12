<template>
  <div class="playlist-form">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑播表' : '新增播表' }}</h2>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="base-info">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="播表名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入播表名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="播表类型" prop="type">
            <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
              <el-option label="视频播表（可混排图片）" value="video" />
              <el-option label="图片播表" value="image" />
              <el-option label="字幕播表" value="text" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="20" class="editor-area">
      <el-col :span="12">
        <el-card shadow="never" class="material-pool">
          <template #header>
            <div class="card-header">
              <span>媒体素材池</span>
              <el-input
                v-model="materialSearch"
                placeholder="搜索媒体..."
                size="small"
                style="width: 180px"
                clearable
              />
            </div>
          </template>

          <div class="material-list" v-loading="materialLoading">
            <el-empty v-if="filteredMaterials.length === 0" description="暂无可用素材" />
            <el-scrollbar v-else height="450px">
              <div
                v-for="item in filteredMaterials"
                :key="item.id"
                class="material-item"
                :class="{ selected: selectedMaterialIds.has(item.id) }"
                @click="toggleSelect(item)"
              >
                <div class="material-thumb">
                  <el-image
                    v-if="item.type === 'image'"
                    :src="getFileUrl(item.thumbnail || item.filePath)"
                    fit="cover"
                    style="width: 60px; height: 45px"
                  />
                  <div v-else-if="item.type === 'video'" class="video-placeholder">
                    <el-icon size="24"><VideoCamera /></el-icon>
                  </div>
                  <div v-else class="text-placeholder">
                    <el-icon size="24"><Document /></el-icon>
                  </div>
                </div>
                <div class="material-info">
                  <div class="material-title" :title="item.title">{{ item.title }}</div>
                  <el-tag size="small" :type="tagType(item.type)">{{ typeLabel(item.type) }}</el-tag>
                </div>
              </div>
            </el-scrollbar>
          </div>

          <div class="add-btn-wrapper">
            <el-button
              type="primary"
              :disabled="selectedMaterialIds.size === 0"
              @click="addToPlaylist"
            >
              添加到播表
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" class="playlist-content">
          <template #header>
            <div class="card-header">
              <span>播表内容（{{ playlistItems.length }} 项）</span>
              <el-button
                v-if="playlistItems.length > 0"
                size="small"
                type="danger"
                plain
                @click="clearAll"
              >
                清空全部
              </el-button>
            </div>
          </template>

          <el-empty v-if="playlistItems.length === 0" description="请从左侧选择媒体添加到播表" />

          <el-scrollbar v-else height="450px">
            <VueDraggable
              v-model="playlistItems"
              handle=".drag-handle"
              ghost-class="ghost-item"
              :animation="150"
              @end="onDragEnd"
            >
              <div
                v-for="(element, index) in playlistItems"
                :key="element.materialId"
                class="playlist-item-card"
              >
                <div class="drag-handle">
                  <el-icon><Rank /></el-icon>
                </div>
                <div class="item-order">{{ index + 1 }}</div>
                <div class="item-thumb">
                  <el-image
                    v-if="element.materialType === 'image'"
                    :src="getFileUrl(element.thumbnail || element.filePath)"
                    fit="cover"
                    style="width: 48px; height: 36px"
                  />
                  <div v-else-if="element.materialType === 'video'" class="video-placeholder small">
                    <el-icon size="16"><VideoCamera /></el-icon>
                  </div>
                  <div v-else class="text-placeholder small">
                    <el-icon size="16"><Document /></el-icon>
                  </div>
                </div>
                <div class="item-info">
                  <div class="item-title" :title="element.materialTitle">
                    {{ element.materialTitle }}
                  </div>
                  <el-tag size="small" :type="tagType(element.materialType)">
                    {{ typeLabel(element.materialType) }}
                  </el-tag>
                </div>
                <div class="item-duration">
                  <el-input-number
                    v-model="element.duration"
                    :min="1"
                    :max="86400"
                    size="small"
                    placeholder="秒"
                    controls-position="right"
                    style="width: 100px"
                  />
                </div>
                <el-button
                  size="small"
                  type="danger"
                  :icon="Delete"
                  circle
                  plain
                  @click="removeItem(index)"
                />
              </div>
            </VueDraggable>
          </el-scrollbar>
        </el-card>
      </el-col>
    </el-row>

    <div class="form-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { VueDraggable } from 'vue-draggable-plus'
import { VideoCamera, Document, Rank, Delete } from '@element-plus/icons-vue'
import { getMaterialList } from '@/api/material'
import type { Material } from '@/api/material'
import { createPlaylist, getPlaylistDetail, updatePlaylist } from '@/api/playlist'
import type { PlaylistItemRequest } from '@/api/playlist'
import { getFileUrl } from '@/utils/file'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const formRef = ref()
const saving = ref(false)
const materialLoading = ref(false)

const form = reactive({
  name: '',
  type: 'video'
})

const rules = {
  name: [{ required: true, message: '请输入播表名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择播表类型', trigger: 'change' }]
}

// ========== 媒体素材池 ==========

const allMaterials = ref<Material[]>([])
const selectedMaterialIds = ref(new Set<number>())
const materialSearch = ref('')

const filteredMaterials = computed(() => {
  let list = allMaterials.value
  const targetType = form.type
  if (targetType === 'video') {
    list = list.filter((m) => m.type === 'video' || m.type === 'image')
  } else if (targetType === 'image') {
    list = list.filter((m) => m.type === 'image')
  } else if (targetType === 'text') {
    list = list.filter((m) => m.type === 'text')
  }
  if (materialSearch.value) {
    const kw = materialSearch.value.toLowerCase()
    list = list.filter((m) => m.title.toLowerCase().includes(kw))
  }
  return list
})

const toggleSelect = (item: Material) => {
  const ids = new Set(selectedMaterialIds.value)
  if (ids.has(item.id)) {
    ids.delete(item.id)
  } else {
    ids.add(item.id)
  }
  selectedMaterialIds.value = ids
}

const loadMaterials = async () => {
  materialLoading.value = true
  try {
    const res: any = await getMaterialList({ page: 1, size: 1000 })
    allMaterials.value = res.data.content
  } finally {
    materialLoading.value = false
  }
}

// ========== 播表内容 ==========

interface PlaylistItemLocal {
  materialId: number
  materialTitle: string
  materialType: string
  filePath: string
  thumbnail: string | null
  sortOrder: number
  duration: number | null
}

const playlistItems = ref<PlaylistItemLocal[]>([])

const addToPlaylist = () => {
  const selected = allMaterials.value.filter((m) => selectedMaterialIds.value.has(m.id))
  const existingIds = new Set(playlistItems.value.map((i) => i.materialId))
  const newItems = selected
    .filter((m) => !existingIds.has(m.id))
    .map((m, i) => ({
      materialId: m.id,
      materialTitle: m.title,
      materialType: m.type,
      filePath: m.filePath,
      thumbnail: m.thumbnail,
      sortOrder: playlistItems.value.length + i,
      duration: null as number | null
    }))
  playlistItems.value = [...playlistItems.value, ...newItems]
  selectedMaterialIds.value = new Set()
  ElMessage.success(`已添加 ${newItems.length} 个媒体`)
}

const removeItem = (index: number) => {
  playlistItems.value.splice(index, 1)
  playlistItems.value.forEach((item, i) => {
    item.sortOrder = i
  })
}

const clearAll = () => {
  playlistItems.value = []
}

const onDragEnd = () => {
  playlistItems.value.forEach((item, i) => {
    item.sortOrder = i
  })
}

// ========== 编辑模式 ==========

const loadPlaylistForEdit = async () => {
  if (!isEdit.value) return
  try {
    const res: any = await getPlaylistDetail(editId.value)
    const detail = res.data
    form.name = detail.name
    form.type = detail.type
    playlistItems.value = (detail.items || []).map((item: any) => ({
      materialId: item.materialId,
      materialTitle: item.materialTitle || '（素材已删除）',
      materialType: item.materialType || 'image',
      filePath: item.materialFilePath || '',
      thumbnail: item.materialThumbnail,
      sortOrder: item.sortOrder,
      duration: item.duration
    }))
  } catch (e) {
    ElMessage.error('加载播表详情失败')
  }
}

// ========== 保存 ==========

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const items: PlaylistItemRequest[] = playlistItems.value.map((item, i) => ({
      materialId: item.materialId,
      sortOrder: i,
      duration: item.duration || undefined
    }))

    const data = { name: form.name, type: form.type, items }

    if (isEdit.value) {
      await updatePlaylist(editId.value, data)
      ElMessage.success('播表已更新')
    } else {
      await createPlaylist(data)
      ElMessage.success('播表已创建')
    }
    router.push('/dashboard/playlist')
  } catch (e) {
    // 表单校验失败或接口报错
  } finally {
    saving.value = false
  }
}

const handleCancel = () => router.push('/dashboard/playlist')

// ========== 工具函数 ==========

const typeLabel = (type: string) =>
  ({ image: '图片', video: '视频', text: '字幕' })[type] || type
const tagType = (type: string) =>
  ({ image: 'success', video: 'primary', text: 'warning' })[type] || 'info'

onMounted(() => {
  loadMaterials()
  loadPlaylistForEdit()
})
</script>

<style scoped>
.playlist-form {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.base-info {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.material-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin: 0 12px 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.material-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.material-item.selected {
  border-color: #409eff;
  background-color: #d9ecff;
}

.material-thumb {
  flex-shrink: 0;
}

.material-info {
  flex: 1;
  min-width: 0;
}

.material-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.add-btn-wrapper {
  margin-top: 12px;
  text-align: center;
}

.video-placeholder {
  width: 60px;
  height: 45px;
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #909399;
}

.video-placeholder.small {
  width: 48px;
  height: 36px;
}

.text-placeholder {
  width: 60px;
  height: 45px;
  background-color: #fdf6ec;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #e6a23c;
}

.text-placeholder.small {
  width: 48px;
  height: 36px;
}

.playlist-item-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  margin: 0 12px 8px;
  background-color: #fff;
  transition: box-shadow 0.2s;
}

.playlist-item-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.ghost-item {
  opacity: 0.4;
  border: 2px dashed #409eff;
}

.drag-handle {
  cursor: grab;
  color: #909399;
  flex-shrink: 0;
}

.item-order {
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: #ecf5ff;
  color: #409eff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: bold;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.item-duration {
  flex-shrink: 0;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}
</style>
