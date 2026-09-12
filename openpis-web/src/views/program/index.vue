<template>
  <div class="program-page">
    <div class="page-header">
      <h2>栏目管理</h2>
      <el-button type="primary" @click="handleAdd">新增栏目</el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="输入名称搜索"
        style="width: 240px"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" style="margin-left: 12px" @click="handleSearch">搜索</el-button>
    </div>

    <el-table :data="programList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="栏目名称" show-overflow-tooltip />
      <el-table-column prop="layoutName" label="布局" show-overflow-tooltip />
      <el-table-column label="播放类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="playTagType(row.playType)" size="small">{{ playLabel(row.playType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="moduleCount" label="绑定模块数" width="110" align="center" />
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
import { getProgramList, deleteProgram } from '@/api/program'
import type { ProgramListItem } from '@/api/program'

const router = useRouter()
const loading = ref(false)
const programList = ref<ProgramListItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const playLabel = (t: string) => ({ loop: '垫播', insert: '插播', emergency: '紧急' })[t] || t
const playTagType = (t: string) => ({ loop: 'info', insert: 'warning', emergency: 'danger' })[t] || 'info'
const formatTime = (s: string) => (s ? s.replace('T', ' ').slice(0, 19) : '')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getProgramList({
      page: page.value,
      size: size.value,
      keyword: searchKeyword.value || undefined
    })
    programList.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}
const handleAdd = () => router.push('/dashboard/program/create')
const handleEdit = (row: ProgramListItem) => router.push(`/dashboard/program/edit/${row.id}`)

const handleDelete = (row: ProgramListItem) => {
  ElMessageBox.confirm(`确定删除栏目「${row.name}」吗？引用它的计划将无法正常播放。`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteProgram(row.id)
    ElMessage.success('已删除')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.program-page {
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
