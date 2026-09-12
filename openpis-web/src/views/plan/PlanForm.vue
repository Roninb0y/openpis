<template>
  <div class="plan-form">
    <div class="page-header">
      <h2>{{ isEdit ? '编辑计划' : '新增计划' }}</h2>
    </div>

    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" class="base-info">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="计划名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入计划名称" />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="计划类型" prop="planType">
            <el-radio-group v-model="form.planType" @change="onPlanTypeChange">
              <el-radio value="daily">日计划</el-radio>
              <el-radio value="weekly">周计划</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生效日期" prop="dateRange">
            <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>
            {{ form.planType === 'daily' ? '每日时间段（生效期内每天播放）' : '按星期配置时间段' }}
          </span>
          <el-button type="primary" size="small" @click="addRow(activeDay)">
            + 添加时间段
          </el-button>
        </div>
      </template>

      <!-- 日计划：单一列表 -->
      <template v-if="form.planType === 'daily'">
        <el-table :data="dailyRows" border size="small">
          <el-table-column label="栏目" min-width="220">
            <template #default="{ row }">
              <el-select v-model="row.programId" placeholder="选择栏目" style="width: 100%">
                <el-option
                  v-for="p in programs"
                  :key="p.id"
                  :label="`${p.name}（${playLabel(p.playType)}）`"
                  :value="p.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="开始时间" width="160">
            <template #default="{ row }">
              <el-time-select
                v-model="row.startTime"
                start="00:00"
                step="00:15"
                end="23:45"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="结束时间" width="160">
            <template #default="{ row }">
              <el-time-select
                v-model="row.endTime"
                start="00:15"
                step="00:15"
                end="24:00"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" align="center">
            <template #default="{ $index }">
              <el-button size="small" type="danger" plain @click="removeRow(dailyRows, $index)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="dailyRows.length === 0" description="暂无时间段，点击右上角添加" />
      </template>

      <!-- 周计划：按星期分组 -->
      <template v-else>
        <el-tabs v-model="activeDay">
          <el-tab-pane
            v-for="(day, index) in WEEK_DAYS"
            :key="index"
            :label="day"
            :name="String(index + 1)"
          >
            <el-table :data="weeklyRows[index + 1] || []" border size="small">
              <el-table-column label="栏目" min-width="220">
                <template #default="{ row }">
                  <el-select v-model="row.programId" placeholder="选择栏目" style="width: 100%">
                    <el-option
                      v-for="p in programs"
                      :key="p.id"
                      :label="`${p.name}（${playLabel(p.playType)}）`"
                      :value="p.id"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="开始时间" width="160">
                <template #default="{ row }">
                  <el-time-select
                    v-model="row.startTime"
                    start="00:00"
                    step="00:15"
                    end="23:45"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="结束时间" width="160">
                <template #default="{ row }">
                  <el-time-select
                    v-model="row.endTime"
                    start="00:15"
                    step="00:15"
                    end="24:00"
                    style="width: 100%"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="90" align="center">
                <template #default="{ $index }">
                  <el-button
                    size="small"
                    type="danger"
                    plain
                    @click="removeRow(weeklyRows[index + 1], $index)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty
              v-if="!weeklyRows[index + 1] || weeklyRows[index + 1].length === 0"
              :description="`周${day}暂无时间段`"
              :image-size="60"
            />
          </el-tab-pane>
        </el-tabs>
      </template>
    </el-card>

    <div class="form-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createPlan, getPlanDetail, updatePlan } from '@/api/plan'
import type { ScheduleItem } from '@/api/plan'
import { getProgramList } from '@/api/program'
import type { ProgramListItem } from '@/api/program'

const router = useRouter()
const route = useRoute()

const isEdit = computed(() => !!route.params.id)
const editId = computed(() => Number(route.params.id))

const WEEK_DAYS = ['一', '二', '三', '四', '五', '六', '日']

const formRef = ref()
const saving = ref(false)

const form = reactive({
  name: '',
  planType: 'daily' as 'daily' | 'weekly',
  dateRange: null as [string, string] | null
})

const rules = {
  name: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  planType: [{ required: true, message: '请选择计划类型', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

interface RowItem {
  programId: number | null
  dayOfWeek: number | null
  startTime: string
  endTime: string
}

const dailyRows = ref<RowItem[]>([])
const weeklyRows = reactive<Record<number, RowItem[]>>({
  1: [], 2: [], 3: [], 4: [], 5: [], 6: [], 7: []
})
const activeDay = ref('1')

const programs = ref<ProgramListItem[]>([])

const playLabel = (t: string) => ({ loop: '垫播', insert: '插播', emergency: '紧急' })[t] || t

const addRow = (dayKey?: string) => {
  const row: RowItem = { programId: null, dayOfWeek: null, startTime: '08:00', endTime: '12:00' }
  if (form.planType === 'daily') {
    dailyRows.value.push(row)
  } else {
    const day = Number(dayKey || activeDay.value)
    row.dayOfWeek = day
    weeklyRows[day].push(row)
  }
}

const removeRow = (list: RowItem[] | undefined, index: number) => {
  list?.splice(index, 1)
}

const onPlanTypeChange = () => {
  // 切换类型保留已配置的栏目/时间，dayOfWeek 由保存时按容器决定
}

const collectItems = (): ScheduleItem[] => {
  const items: ScheduleItem[] = []
  if (form.planType === 'daily') {
    for (const row of dailyRows.value) {
      if (!row.programId) continue
      items.push({
        programId: row.programId,
        startTime: row.startTime,
        endTime: row.endTime
      })
    }
  } else {
    for (const day of [1, 2, 3, 4, 5, 6, 7]) {
      for (const row of weeklyRows[day]) {
        if (!row.programId) continue
        items.push({
          programId: row.programId,
          dayOfWeek: day,
          startTime: row.startTime,
          endTime: row.endTime
        })
      }
    }
  }
  return items
}

const handleSave = async () => {
  try {
    await formRef.value.validate()
    const items = collectItems()
    if (items.length === 0) {
      ElMessage.warning('至少配置一条完整的时间段（请选择栏目）')
      return
    }
    saving.value = true

    const data = {
      name: form.name,
      planType: form.planType,
      startDate: form.dateRange![0],
      endDate: form.dateRange![1],
      schedule: { items }
    }

    if (isEdit.value) {
      await updatePlan(editId.value, data)
      ElMessage.success('计划已更新')
    } else {
      await createPlan(data)
      ElMessage.success('计划已创建（草稿），请点击「发布」生效')
    }
    router.push('/dashboard/plan')
  } catch (e) {
    // 校验失败
  } finally {
    saving.value = false
  }
}

const handleCancel = () => router.push('/dashboard/plan')

const loadData = async () => {
  const res: any = await getProgramList({ page: 1, size: 1000 })
  programs.value = res.data.content

  if (isEdit.value) {
    const detailRes: any = await getPlanDetail(editId.value)
    const detail = detailRes.data
    form.name = detail.name
    form.planType = detail.planType
    form.dateRange = [detail.startDate, detail.endDate]

    for (const item of detail.items || []) {
      const row: RowItem = {
        programId: item.programId,
        dayOfWeek: item.dayOfWeek,
        startTime: item.startTime,
        endTime: item.endTime
      }
      if (form.planType === 'daily') {
        dailyRows.value.push(row)
      } else if (item.dayOfWeek) {
        weeklyRows[item.dayOfWeek].push(row)
      }
    }
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.plan-form {
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

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 0;
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
