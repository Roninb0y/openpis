<template>
  <div class="layout-editor">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑布局' : '新增布局' }}</h2>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="base-info">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="布局名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入布局名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分辨率" prop="resolution">
            <el-select
              v-model="form.resolution"
              placeholder="选择分辨率"
              style="width: 100%"
              @change="onResolutionChange"
            >
              <el-option label="1920 × 1080（横屏 16:9）" value="1920x1080" />
              <el-option label="1080 × 1920（竖屏 9:16）" value="1080x1920" />
              <el-option label="1366 × 768（横屏）" value="1366x768" />
              <el-option label="1024 × 768（横屏 4:3）" value="1024x768" />
              <el-option label="800 × 600（横屏）" value="800x600" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" size="small" @click="addModule">+ 添加模块</el-button>
      <el-button size="small" :disabled="!selectedModuleId" @click="deleteModule">删除选中模块</el-button>
      <el-divider direction="vertical" />
      <span class="toolbar-hint">拖拽模块移动位置，拖拽四角手柄调整大小，右键设定媒体类型</span>
    </div>

    <div class="canvas-wrapper" ref="canvasWrapperRef" @click="closeContextMenu">
      <v-stage ref="stageRef" :config="stageConfig" @mousedown="handleStageMouseDown">
        <v-layer>
          <v-rect :config="bgRect" />
          <v-group
            v-for="mod in sortedModules"
            :key="mod.id"
            :config="groupConfig(mod)"
            @dragend="onDragEnd(mod, $event)"
            @click="selectModule(mod.id)"
            @tap="selectModule(mod.id)"
            @contextmenu="onModuleContextMenu(mod, $event)"
          >
            <v-rect :config="getModRect(mod)" />
            <v-text :config="getModText(mod)" />
          </v-group>
          <v-transformer ref="transformerRef" :config="transformerConfig" />
        </v-layer>
      </v-stage>

      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      >
        <div class="menu-item" @click="setModuleType('video')">视频（绿色）</div>
        <div class="menu-item" @click="setModuleType('image')">图片（红色）</div>
        <div class="menu-item" @click="setModuleType('text')">字幕（黄色）</div>
        <div class="menu-item" @click="setModuleType('flash')">Flash（蓝色）</div>
      </div>
    </div>

    <el-card shadow="never" class="module-table">
      <template #header><span>模块参数表</span></template>
      <el-table :data="modules" border size="small">
        <el-table-column prop="name" label="名称" width="140">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="tagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="X" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.x" size="small" :min="0" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="Y" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.y" size="small" :min="0" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="宽" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.w" size="small" :min="50" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="高" width="110">
          <template #default="{ row }">
            <el-input-number v-model="row.h" size="small" :min="50" controls-position="right" style="width: 90px" />
          </template>
        </el-table-column>
        <el-table-column label="层级" width="190">
          <template #default="{ row, $index }">
            <el-button-group size="small">
              <el-button :disabled="$index === 0" @click="moveLayer(row, 'top')" title="置顶">⟰</el-button>
              <el-button :disabled="$index === 0" @click="moveLayer(row, 'up')" title="上移">↑</el-button>
              <el-button :disabled="$index === modules.length - 1" @click="moveLayer(row, 'down')" title="下移">↓</el-button>
              <el-button :disabled="$index === modules.length - 1" @click="moveLayer(row, 'bottom')" title="置底">⟱</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="form-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createLayout, getLayoutDetail, updateLayout } from '@/api/layout'
import type { ModuleConfig } from '@/api/layout'

const router = useRouter()
const route = useRoute()
const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const formRef = ref()
const saving = ref(false)
const stageRef = ref()
const transformerRef = ref()
const canvasWrapperRef = ref()

const form = reactive({
  name: '',
  resolution: '1920x1080'
})

const rules = {
  name: [{ required: true, message: '请输入布局名称', trigger: 'blur' }]
}

// ========== 分辨率预设 ==========

const resolutionMap: Record<string, { w: number; h: number }> = {
  '1920x1080': { w: 1920, h: 1080 },
  '1080x1920': { w: 1080, h: 1920 },
  '1366x768': { w: 1366, h: 768 },
  '1024x768': { w: 1024, h: 768 },
  '800x600': { w: 800, h: 600 }
}

const layoutWidth = computed(() => resolutionMap[form.resolution]?.w || 1920)
const layoutHeight = computed(() => resolutionMap[form.resolution]?.h || 1080)

// 画布显示最大 800px 宽，等比例缩放；保存的是原始分辨率坐标
const maxCanvasWidth = 800
const scale = computed(() => maxCanvasWidth / layoutWidth.value)
const stageConfig = computed(() => ({
  width: maxCanvasWidth,
  height: Math.round(layoutHeight.value * scale.value)
}))

const bgRect = computed(() => ({
  width: maxCanvasWidth,
  height: Math.round(layoutHeight.value * scale.value),
  fill: '#e8e8e8',
  listening: false
}))

// ========== 模块管理 ==========

const modules = ref<ModuleConfig[]>([])
const selectedModuleId = ref<string | null>(null)
let moduleCounter = 1

const sortedModules = computed(() => [...modules.value].sort((a, b) => a.zIndex - b.zIndex))

const addModule = () => {
  const id = `m${moduleCounter++}`
  const w = Math.round(layoutWidth.value / 3)
  const h = Math.round(layoutHeight.value / 3)
  const newModule: ModuleConfig = {
    id,
    name: `模块${modules.value.length + 1}`,
    type: 'video',
    x: 50,
    y: 50,
    w: Math.max(w, 100),
    h: Math.max(h, 100),
    zIndex: modules.value.length + 1
  }
  modules.value.push(newModule)
  selectModule(id)
}

const deleteModule = () => {
  if (!selectedModuleId.value) return
  modules.value = modules.value.filter((m) => m.id !== selectedModuleId.value)
  selectedModuleId.value = null
  clearTransformer()
}

const selectModule = (id: string) => {
  selectedModuleId.value = id
}

const handleStageMouseDown = (e: any) => {
  if (e.target === e.target.getStage()) {
    selectedModuleId.value = null
    clearTransformer()
  }
}

const clearTransformer = () => {
  const transformer = transformerRef.value?.getStage()?.findOne('Transformer')
  if (transformer) transformer.nodes([])
}

const clamp = (v: number, min: number, max: number) => Math.min(Math.max(v, min), Math.max(max, min))

const groupConfig = (mod: ModuleConfig) => ({
  id: mod.id,
  x: clamp(mod.x, 0, layoutWidth.value - mod.w) * scale.value,
  y: clamp(mod.y, 0, layoutHeight.value - mod.h) * scale.value,
  width: mod.w * scale.value,
  height: mod.h * scale.value,
  draggable: true
})

// 拖拽结束后把缩放坐标换算回原始分辨率坐标
const onDragEnd = (mod: ModuleConfig, e: any) => {
  mod.x = Math.round(clamp(e.target.x() / scale.value, 0, layoutWidth.value - mod.w))
  mod.y = Math.round(clamp(e.target.y() / scale.value, 0, layoutHeight.value - mod.h))
  e.target.x(mod.x * scale.value)
  e.target.y(mod.y * scale.value)
}

// ========== 右键菜单 ==========

const contextMenu = reactive({ visible: false, x: 0, y: 0, moduleId: '' })

const onModuleContextMenu = (mod: ModuleConfig, e: any) => {
  e.evt.preventDefault()
  e.cancelBubble = true
  selectModule(mod.id)
  const rect = canvasWrapperRef.value?.getBoundingClientRect()
  if (!rect) return
  contextMenu.moduleId = mod.id
  contextMenu.x = e.evt.clientX - rect.left
  contextMenu.y = e.evt.clientY - rect.top
  contextMenu.visible = true
}

const closeContextMenu = () => {
  contextMenu.visible = false
}

const setModuleType = (type: 'video' | 'image' | 'text' | 'flash') => {
  const mod = modules.value.find((m) => m.id === contextMenu.moduleId)
  if (mod) mod.type = type
  contextMenu.visible = false
}

// ========== 层级调整 ==========

const moveLayer = (mod: ModuleConfig, dir: 'top' | 'up' | 'down' | 'bottom') => {
  const sorted = sortedModules.value.map((m) => m.id)
  const idx = sorted.indexOf(mod.id)
  if (idx === -1) return

  switch (dir) {
    case 'top': {
      const maxZ = Math.max(...modules.value.map((m) => m.zIndex))
      mod.zIndex = maxZ + 1
      break
    }
    case 'up': {
      if (idx > 0) {
        const target = modules.value.find((m) => m.id === sorted[idx - 1])
        if (target) {
          const tmp = mod.zIndex
          mod.zIndex = target.zIndex
          target.zIndex = tmp
        }
      }
      break
    }
    case 'down': {
      if (idx < sorted.length - 1) {
        const target = modules.value.find((m) => m.id === sorted[idx + 1])
        if (target) {
          const tmp = mod.zIndex
          mod.zIndex = target.zIndex
          target.zIndex = tmp
        }
      }
      break
    }
    case 'bottom': {
      const minZ = Math.min(...modules.value.map((m) => m.zIndex))
      mod.zIndex = minZ - 1
      break
    }
  }
  modules.value = [...modules.value]
}

// ========== Transformer（缩放手柄）==========

const transformerConfig = {
  keepRatio: false,
  enabledAnchors: ['top-left', 'top-right', 'bottom-left', 'bottom-right'],
  boundBoxFunc: (oldBox: any, newBox: any) => {
    if (newBox.width < 50 || newBox.height < 50) return oldBox
    return newBox
  }
}

watch(selectedModuleId, async (id) => {
  await nextTick()
  const stage = stageRef.value?.getStage()
  const transformer = transformerRef.value?.getStage()?.findOne('Transformer')
  if (!stage || !transformer) return

  if (id) {
    const nodes = stage.find(`#${id}`)
    if (nodes.length > 0) {
      const node = nodes[0]
      transformer.nodes([node])

      node.off('transformend')
      node.on('transformend', () => {
        const mod = modules.value.find((m) => m.id === id)
        if (mod) {
          const newW = Math.max(50, Math.round((node.width() * node.scaleX()) / scale.value))
          const newH = Math.max(50, Math.round((node.height() * node.scaleY()) / scale.value))
          mod.x = Math.round(clamp(node.x() / scale.value, 0, layoutWidth.value - newW))
          mod.y = Math.round(clamp(node.y() / scale.value, 0, layoutHeight.value - newH))
          mod.w = newW
          mod.h = newH
          node.scaleX(1)
          node.scaleY(1)
          node.x(mod.x * scale.value)
          node.y(mod.y * scale.value)
        }
      })
    }
  } else {
    transformer.nodes([])
  }
})

// ========== 画布渲染 ==========

const typeColors: Record<string, string> = {
  video: '#67c23a',
  image: '#f56c6c',
  text: '#e6a23c',
  flash: '#409eff'
}

const getModRect = (mod: ModuleConfig) => ({
  width: mod.w * scale.value,
  height: mod.h * scale.value,
  fill: typeColors[mod.type] || '#ccc',
  stroke: selectedModuleId.value === mod.id ? '#000' : 'transparent',
  strokeWidth: 2,
  cornerRadius: 3,
  listening: true
})

const getModText = (mod: ModuleConfig) => ({
  text: `${mod.name}\n(${typeLabel(mod.type)})`,
  fontSize: Math.max(12, 14 * scale.value),
  fill: '#fff',
  width: mod.w * scale.value,
  height: mod.h * scale.value,
  padding: 5,
  align: 'center',
  verticalAlign: 'middle',
  listening: false
})

const typeLabel = (t: string) =>
  ({ video: '视频', image: '图片', text: '字幕', flash: 'Flash' })[t] || t
const tagType = (t: string) =>
  ({ video: 'success', image: 'danger', text: 'warning', flash: 'primary' })[t] || 'info'

const onResolutionChange = () => {
  selectedModuleId.value = null
  clearTransformer()
}

// ========== 保存 / 加载 ==========

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const data = {
      name: form.name,
      resolution: form.resolution,
      width: layoutWidth.value,
      height: layoutHeight.value,
      modules: modules.value
    }

    if (isEdit.value) {
      await updateLayout(editId.value, data)
      ElMessage.success('布局已更新')
    } else {
      await createLayout(data)
      ElMessage.success('布局已创建')
    }
    router.push('/dashboard/layout')
  } catch (e) {
    // 校验失败
  } finally {
    saving.value = false
  }
}

const handleCancel = () => router.push('/dashboard/layout')

const loadLayoutForEdit = async () => {
  if (!isEdit.value) return
  try {
    const res: any = await getLayoutDetail(editId.value)
    const detail = res.data
    form.name = detail.name
    form.resolution = detail.resolution
    modules.value = detail.modules || []
    const nums = modules.value.map((m) => parseInt(m.id.replace('m', '')) || 0)
    moduleCounter = nums.length > 0 ? Math.max(...nums) + 1 : 1
  } catch (e) {
    ElMessage.error('加载布局失败')
  }
}

onMounted(() => {
  loadLayoutForEdit()
  document.addEventListener('click', closeContextMenu)
})
</script>

<style scoped>
.layout-editor {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.base-info {
  margin-bottom: 16px;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.toolbar-hint {
  color: #909399;
  font-size: 13px;
}

.canvas-wrapper {
  position: relative;
  margin-bottom: 20px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  display: inline-block;
}

.context-menu {
  position: absolute;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 150px;
}

.menu-item {
  padding: 8px 16px;
  cursor: pointer;
  font-size: 13px;
}

.menu-item:hover {
  background: #ecf5ff;
  color: #409eff;
}

.module-table {
  margin-bottom: 20px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  border-top: 1px solid #ebeef5;
}
</style>
