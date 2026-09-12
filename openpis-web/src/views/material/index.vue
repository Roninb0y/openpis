<template>
  <div class="material-page">
    <div class="page-header">
      <h2>媒体管理</h2>
      <el-button type="primary" @click="showUploadDialog = true">上传媒体</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchType" placeholder="类型" clearable style="width: 120px">
        <el-option label="图片" value="image" />
        <el-option label="视频" value="video" />
        <el-option label="字幕" value="text" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="输入标题搜索"
        style="width: 240px; margin-left: 12px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="materialList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="预览" width="110" align="center">
        <template #default="{ row }">
          <el-image
            v-if="row.type === 'image'"
            :src="getFileUrl(row.thumbnail || row.filePath)"
            :preview-src-list="[getFileUrl(row.filePath)]"
            preview-teleported
            fit="cover"
            style="width: 80px; height: 55px"
          />
          <el-icon v-else-if="row.type === 'video'" size="26" color="#409eff"><VideoCamera /></el-icon>
          <el-icon v-else size="26" color="#e6a23c"><Document /></el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="tagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="100" align="center">
        <template #default="{ row }">
          {{ row.type === 'video' ? (row.duration ? row.duration + '秒' : '未设置') : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handlePreview(row)">预览</el-button>
          <el-button size="small" type="primary" plain @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <UploadDialog v-model:visible="showUploadDialog" @success="loadData" />
    <PreviewDialog v-model:visible="showPreviewDialog" :material="currentMaterial" />

    <el-dialog v-model="showEditDialog" title="编辑媒体" width="460px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="editForm.type === 'video'" label="时长(秒)">
          <el-input-number v-model="editForm.duration" :min="1" :max="86400" />
          <span class="edit-tip">视频时长影响播放器中的播放节奏</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaterialList, updateMaterial, deleteMaterial } from '@/api/material'
import type { Material } from '@/api/material'
import { getFileUrl } from '@/utils/file'
import UploadDialog from './components/UploadDialog.vue'
import PreviewDialog from './components/PreviewDialog.vue'

const loading = ref(false)
const materialList = ref<Material[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchType = ref('')
const searchKeyword = ref('')

const showUploadDialog = ref(false)
const showPreviewDialog = ref(false)
const showEditDialog = ref(false)
const currentMaterial = ref<Material | null>(null)
const saving = ref(false)

const editForm = reactive({
  id: 0,
  title: '',
  description: '',
  duration: 0,
  type: ''
})

const typeLabel = (t: string) => ({ image: '图片', video: '视频', text: '字幕' })[t] || t
const tagType = (t: string) => ({ image: 'success', video: 'primary', text: 'warning' })[t] || 'info'
const formatTime = (s: string) => (s ? s.replace('T', ' ').slice(0, 19) : '')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getMaterialList({
      page: page.value,
      size: size.value,
      type: searchType.value || undefined,
      keyword: searchKeyword.value || undefined
    })
    materialList.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handlePreview = (row: Material) => {
  currentMaterial.value = row
  showPreviewDialog.value = true
}

const handleEdit = (row: Material) => {
  editForm.id = row.id
  editForm.title = row.title
  editForm.description = row.description || ''
  editForm.duration = row.duration || 0
  editForm.type = row.type
  showEditDialog.value = true
}

const saveEdit = async () => {
  saving.value = true
  try {
    await updateMaterial(editForm.id, {
      title: editForm.title,
      description: editForm.description,
      duration: editForm.duration
    })
    ElMessage.success('保存成功')
    showEditDialog.value = false
    loadData()
  } finally {
    saving.value = false
  }
}

const handleDelete = (row: Material) => {
  ElMessageBox.confirm(`确定删除「${row.title}」吗？文件与记录将同步删除。`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteMaterial(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.material-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.edit-tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}
</style>
