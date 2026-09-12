<template>
  <el-container class="dashboard">
    <el-aside width="200px" class="aside">
      <div class="logo">OpenPIS-Lite</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/material">
          <el-icon><Picture /></el-icon>
          <span>媒体管理</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/playlist">
          <el-icon><VideoPlay /></el-icon>
          <span>播表管理</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/layout">
          <el-icon><Grid /></el-icon>
          <span>布局管理</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/program">
          <el-icon><Film /></el-icon>
          <span>栏目管理</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/plan">
          <el-icon><Calendar /></el-icon>
          <span>计划管理</span>
        </el-menu-item>
        <el-menu-item index="/dashboard/report">
          <el-icon><DataAnalysis /></el-icon>
          <span>播放报表</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="title">OpenPIS-Lite 管理后台</div>
        <div class="user-info">
          <el-button size="small" @click="openPlayer">
            <el-icon style="margin-right: 4px"><Monitor /></el-icon>打开播放器
          </el-button>
          <span>欢迎，{{ userStore.username }}（{{ userStore.role }}）</span>
          <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const openPlayer = () => {
  window.open('/player?code=DEMO01', '_blank')
}

const handleLogout = () => {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
}

.aside {
  background-color: #304156;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #1f2d3d;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.title {
  font-size: 18px;
  font-weight: bold;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
