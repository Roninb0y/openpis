<template>
  <div class="report-page">
    <h2>播放报表</h2>

    <el-row :gutter="20">
      <el-col :span="24">
        <el-card shadow="never">
          <template #header><span>近 7 天播放次数</span></template>
          <div ref="dailyChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>栏目播放排行</span></template>
          <el-table :data="programStats" border size="small" v-loading="loading">
            <el-table-column prop="name" label="栏目" show-overflow-tooltip />
            <el-table-column prop="count" label="播放次数" width="110" align="center" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header><span>素材播放排行（TOP 10）</span></template>
          <el-table :data="materialStats" border size="small" v-loading="loading">
            <el-table-column prop="title" label="素材" show-overflow-tooltip />
            <el-table-column prop="count" label="播放次数" width="110" align="center" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getReportDaily, getReportProgram, getReportMaterial } from '@/api/player'

const dailyChartRef = ref()
const programStats = ref<any[]>([])
const materialStats = ref<any[]>([])
const loading = ref(false)
let chart: echarts.ECharts | null = null

const loadDaily = async () => {
  const res: any = await getReportDaily(7)
  const data = res.data as Array<{ date: string; count: number }>
  chart?.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map((d) => d.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '播放次数',
        type: 'bar',
        data: data.map((d) => d.count),
        itemStyle: { color: '#409eff' },
        barMaxWidth: 40
      }
    ],
    grid: { left: 40, right: 20, top: 30, bottom: 30 }
  })
}

const handleResize = () => chart?.resize()

onMounted(async () => {
  loading.value = true
  chart = echarts.init(dailyChartRef.value)
  window.addEventListener('resize', handleResize)
  try {
    await loadDaily()
    const pRes: any = await getReportProgram()
    programStats.value = pRes.data
    const mRes: any = await getReportMaterial(10)
    materialStats.value = mRes.data
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.report-page {
  padding: 20px;
}

.chart {
  width: 100%;
  height: 320px;
}
</style>
