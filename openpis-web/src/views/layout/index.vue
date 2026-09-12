<template>
  <div class="layout-page">
    <div class="page-header">
      <h2>布局管理</h2>
      <el-button type="primary" @click="handleAdd">新增布局</el-button>
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

    <el-table :data="layoutList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="布局名称" show-overflow-tooltip />
      <el-table-column prop="resolution" label="分辨率" width="130" align="center" />
      <el-table-column prop="moduleCount" label="模块数" width="90" align="center" />
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
import { getLayoutList, deleteLayout } from '@/api/layout'
import type { LayoutListItem } from '@/api/layout'

const router = useRouter()
const loading = ref(false)
const layoutList = ref<LayoutListItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const formatTime = (s: string) => (s ? s.replace('T', ' ').slice(0, 19) : '')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getLayoutList({
      page: page.value,
      size: size.value,
      keyword: searchKeyword.value || undefined
    })
    layoutList.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}
const handleAdd = () => router.push('/dashboard/layout/create')
const handleEdit = (row: LayoutListItem) => router.push(`/dashboard/layout/edit/${row.id}`)

const handleDelete = (row: LayoutListItem) => {
  ElMessageBox.confirm(`确定删除布局「${row.name}」吗？引用它的栏目将无法正常播放。`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteLayout(row.id)
    ElMessage.success('已删除')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.layout-page {
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
