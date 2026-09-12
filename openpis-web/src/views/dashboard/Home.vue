<template>
  <div class="home">
    <h1>欢迎使用 OpenPIS-Lite</h1>
    <p>这是一个简版数字标牌 / 乘客信息发布系统（PIS）。</p>
    <p class="chain">业务主线：媒体 → 播表 → 布局 → 栏目 → 计划 → 播放 → 报表</p>

    <el-row :gutter="20" style="margin-top: 30px">
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>媒体管理</template>
          <div>上传、管理和预览图片、视频、字幕素材。</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>播表 / 布局</template>
          <div>把素材组合成播放列表，用可视化编辑器设计多区域版面。</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>计划 / 播放</template>
          <div>按日/周编排播放计划，Web 播放端按计划自动播放并回流数据。</div>
        </el-card>
      </el-col>
    </el-row>

    <el-alert
      style="margin-top: 30px"
      type="info"
      :closable="false"
      title="快速体验"
      description="先在「媒体管理」上传素材 → 「播表管理」组建播表 → 「布局管理」画一个版面 → 「栏目管理」绑定播表 → 「计划管理」发布计划 → 顶栏「打开播放器」观看效果。"
    />
  </div>
</template>

<style scoped>
.home {
  padding: 20px;
}

.chain {
  color: #409eff;
  font-weight: bold;
}
</style>
