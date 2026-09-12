<template>
  <div class="program-form">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑栏目' : '新增栏目' }}</h2>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="base-info">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="栏目名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入栏目名称" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="选择布局" prop="layoutId">
            <el-select
              v-model="form.layoutId"
              placeholder="请选择布局"
              style="width: 100%"
              @change="onLayoutChange"
            >
              <el-option
                v-for="item in layoutOptions"
                :key="item.id"
                :label="`${item.name}（${item.resolution}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="播放类型" prop="playType">
            <el-radio-group v-model="form.playType">
              <el-radio value="loop">垫播</el-radio>
              <el-radio value="insert">插播</el-radio>
              <el-radio value="emergency">紧急</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>模块绑定播表（{{ form.layoutId ? modules.length : 0 }} 个模块）</span>
          </template>

          <el-empty v-if="modules.length === 0" description="请先选择一个布局" />

          <el-table v-else :data="modules" border size="small">
            <el-table-column prop="moduleName" label="模块" width="110" />
            <el-table-column label="类型" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="moduleTagType(row.moduleType)" size="small">
                  {{ moduleLabel(row.moduleType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="绑定播表">
              <template #default="{ row }">
                <el-select
                  v-model="row.playlistId"
                  placeholder="选择播表（可留空）"
                  clearable
                  style="width: 100%"
                >
                  <el-option
                    v-for="p in playlistsForModule(row.moduleType)"
                    :key="p.id"
                    :label="`${p.name}（${playlistTypeLabel(p.type)} / ${p.itemCount}项）`"
                    :value="p.id"
                  />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>布局预览</span></template>
          <div v-if="layoutDetail" class="preview-area">
            <div
              class="preview-canvas"
              :style="{
                width: previewWidth + 'px',
                height: (previewWidth * (layoutDetail.height || 1)) / (layoutDetail.width || 1) + 'px'
              }"
            >
              <div
                v-for="mod in modules"
                :key="mod.moduleId"
                class="preview-module"
                :style="moduleStyle(mod)"
              >
                {{ mod.moduleName }}<br />
                <small>{{ moduleLabel(mod.moduleType) }}</small>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无布局" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getLayoutList,
  getLayoutDetail
} from '@/api/layout'
import type { LayoutDetail } from '@/api/layout'
import { getPlaylistList } from '@/api/playlist'
import type { PlaylistListItem } from '@/api/playlist'
import {
  createProgram,
  getProgramDetail,
  updateProgram
} from '@/api/program'
import type { ProgramModuleBinding } from '@/api/program'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const formRef = ref()
const saving = ref(false)

const form = reactive({
  name: '',
  layoutId: null as number | null,
  playType: 'loop'
})

const rules = {
  name: [{ required: true, message: '请输入栏目名称', trigger: 'blur' }],
  layoutId: [{ required: true, message: '请选择布局', trigger: 'change' }],
  playType: [{ required: true, message: '请选择播放类型', trigger: 'change' }]
}

const layoutOptions = ref<any[]>([])
const layoutDetail = ref<LayoutDetail | null>(null)
const playlists = ref<PlaylistListItem[]>([])

interface ModuleRow {
  moduleId: string
  moduleName: string
  moduleType: string
  playlistId: number | null
}

const modules = ref<ModuleRow[]>([])

const previewWidth = 480

const moduleLabel = (t: string) =>
  ({ video: '视频', image: '图片', text: '字幕', flash: 'Flash' })[t] || t
const moduleTagType = (t: string) =>
  ({ video: 'success', image: 'danger', text: 'warning', flash: 'primary' })[t] || 'info'
const playlistTypeLabel = (t: string) =>
  ({ video: '视频', image: '图片', text: '字幕' })[t] || t

// 模块类型 → 可选播表类型（与后端校验规则一致）
const allowedPlaylistType = (moduleType: string): string => {
  const map: Record<string, string> = {
    video: 'video',
    image: 'image',
    text: 'text',
    flash: 'image'
  }
  return map[moduleType] || 'image'
}

const playlistsForModule = (moduleType: string) => {
  const t = allowedPlaylistType(moduleType)
  return playlists.value.filter((p) => p.type === t)
}

const onLayoutChange = async (layoutId: number) => {
  if (!layoutId) {
    layoutDetail.value = null
    modules.value = []
    return
  }
  const res: any = await getLayoutDetail(layoutId)
  layoutDetail.value = res.data
  const prevMap = new Map(modules.value.map((m) => [m.moduleId, m.playlistId]))
  modules.value = (res.data.modules || []).map((m: any) => ({
    moduleId: m.id,
    moduleName: m.name,
    moduleType: m.type,
    playlistId: prevMap.get(m.id) ?? null
  }))
}

const moduleStyle = (row: ModuleRow) => {
  const mod = layoutDetail.value?.modules.find((m) => m.id === row.moduleId)
  if (!mod) return {}
  const s = previewWidth / (layoutDetail.value?.width || 1)
  const colors: Record<string, string> = {
    video: '#67c23a',
    image: '#f56c6c',
    text: '#e6a23c',
    flash: '#409eff'
  }
  const bound = !!row.playlistId
  return {
    left: (mod.x || 0) * s + 'px',
    top: (mod.y || 0) * s + 'px',
    width: (mod.w || 0) * s + 'px',
    height: (mod.h || 0) * s + 'px',
    zIndex: mod.zIndex || 1,
    background: bound ? colors[mod.type] || '#ccc' : '#909399',
    opacity: bound ? 0.85 : 0.4
  }
}

const loadData = async () => {
  const layoutRes: any = await getLayoutList({ page: 1, size: 100 })
  layoutOptions.value = layoutRes.data.content

  const playlistRes: any = await getPlaylistList({ page: 1, size: 1000 })
  playlists.value = playlistRes.data.content

  if (isEdit.value) {
    const res: any = await getProgramDetail(editId.value)
    const detail = res.data
    form.name = detail.name
    form.layoutId = detail.layoutId
    form.playType = detail.playType
    await onLayoutChange(detail.layoutId)
    // 回填已有绑定
    for (const mb of detail.modules || [] as ProgramModuleBinding[]) {
      const row = modules.value.find((m) => m.moduleId === mb.moduleId)
      if (row) row.playlistId = mb.playlistId
    }
  }
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    saving.value = true

    const modulePlaylists = modules.value
      .filter((m) => m.playlistId)
      .map((m) => ({ moduleId: m.moduleId, playlistId: m.playlistId! }))

    const data = {
      name: form.name,
      layoutId: form.layoutId!,
      playType: form.playType,
      modulePlaylists
    }

    if (isEdit.value) {
      await updateProgram(editId.value, data)
      ElMessage.success('栏目已更新')
    } else {
      await createProgram(data)
      ElMessage.success('栏目已创建')
    }
    router.push('/dashboard/program')
  } catch (e) {
    // 校验失败或接口报错
  } finally {
    saving.value = false
  }
}

const handleCancel = () => router.push('/dashboard/program')

watch(
  () => modules.value.map((m) => m.playlistId),
  () => {
    // 绑定变化时触发预览重绘（moduleStyle 是方法调用，无需额外处理）
  },
  { deep: true }
)

onMounted(() => loadData())
</script>

<style scoped>
.program-form {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.base-info {
  margin-bottom: 20px;
}

.preview-area {
  display: flex;
  justify-content: center;
}

.preview-canvas {
  position: relative;
  background: #e8e8e8;
  border: 1px solid #dcdfe6;
  overflow: hidden;
}

.preview-module {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-sizing: border-box;
  text-align: center;
  overflow: hidden;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
