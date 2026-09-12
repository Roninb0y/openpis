<template>
  <div class="playlist-page">
    <div class="page-header">
      <h2>播表管理</h2>
      <el-button type="primary" @click="handleAdd">新增播表</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchType" placeholder="类型" clearable style="width: 140px">
        <el-option label="视频播表" value="video" />
        <el-option label="图片播表" value="image" />
        <el-option label="字幕播表" value="text" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="输入名称搜索"
        style="width: 240px; margin-left: 12px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="playlistList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="播表名称" show-overflow-tooltip />
      <el-table-column label="类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="tagType(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="itemCount" label="媒体数量" width="100" align="center" />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlaylistList, deletePlaylist } from '@/api/playlist'
import type { PlaylistListItem } from '@/api/playlist'

const router = useRouter()
const loading = ref(false)
const playlistList = ref<PlaylistListItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchType = ref('')
const searchKeyword = ref('')

const typeLabel = (t: string) =>
  ({ video: '视频播表', image: '图片播表', text: '字幕播表' })[t] || t
const tagType = (t: string) => ({ video: 'primary', image: 'success', text: 'warning' })[t] || 'info'
const formatTime = (s: string) => (s ? s.replace('T', ' ').slice(0, 19) : '')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getPlaylistList({
      page: page.value,
      size: size.value,
      type: searchType.value || undefined,
      keyword: searchKeyword.value || undefined
    })
    playlistList.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleAdd = () => router.push('/dashboard/playlist/create')
const handleEdit = (row: PlaylistListItem) => router.push(`/dashboard/playlist/edit/${row.id}`)

const handleDelete = (row: PlaylistListItem) => {
  ElMessageBox.confirm(`确定删除播表「${row.name}」吗？此操作不可恢复。`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deletePlaylist(row.id)
    ElMessage.success('删除成功')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.playlist-page {
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
</style>
