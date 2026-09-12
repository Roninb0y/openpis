<template>
  <div class="plan-page">
    <div class="page-header">
      <h2>计划管理</h2>
      <el-button type="primary" @click="handleAdd">新增计划</el-button>
    </div>

    <div class="search-bar">
      <el-select v-model="searchStatus" placeholder="状态" clearable style="width: 130px">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
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

    <el-table :data="planList" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="计划名称" show-overflow-tooltip />
      <el-table-column label="类型" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.planType === 'daily' ? '日计划' : '周计划' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生效期" width="210" align="center">
        <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
      </el-table-column>
      <el-table-column prop="itemCount" label="时间段数" width="95" align="center" />
      <el-table-column label="状态" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="170">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button
            v-if="row.status === 0"
            size="small"
            type="success"
            @click="handlePublish(row)"
          >
            发布
          </el-button>
          <el-button v-else size="small" type="warning" @click="handleUnpublish(row)">
            撤销
          </el-button>
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
import { getPlanList, deletePlan, publishPlan, unpublishPlan } from '@/api/plan'
import type { PlanListItem } from '@/api/plan'

const router = useRouter()
const loading = ref(false)
const planList = ref<PlanListItem[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchStatus = ref<number | null>(null)
const searchKeyword = ref('')

const formatTime = (s: string) => (s ? s.replace('T', ' ').slice(0, 19) : '')

const loadData = async () => {
  loading.value = true
  try {
    const res: any = await getPlanList({
      page: page.value,
      size: size.value,
      status: searchStatus.value ?? undefined,
      keyword: searchKeyword.value || undefined
    })
    planList.value = res.data.content
    total.value = res.data.totalElements
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const handleAdd = () => router.push('/dashboard/plan/create')
const handleEdit = (row: PlanListItem) => router.push(`/dashboard/plan/edit/${row.id}`)

const handlePublish = async (row: PlanListItem) => {
  await publishPlan(row.id)
  ElMessage.success('计划已发布，播放端将自动切换')
  loadData()
}

const handleUnpublish = async (row: PlanListItem) => {
  await unpublishPlan(row.id)
  ElMessage.success('计划已撤销')
  loadData()
}

const handleDelete = (row: PlanListItem) => {
  ElMessageBox.confirm(`确定删除计划「${row.name}」吗？`, '警告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deletePlan(row.id)
    ElMessage.success('已删除')
    loadData()
  }).catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped>
.plan-page {
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
