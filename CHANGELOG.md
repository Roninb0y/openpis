# Changelog

本项目所有显著变更将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

## [Unreleased]

## [1.0.0] - 2026-09-13

首个公开版本：实现数字标牌 / 乘客信息系统的最小业务闭环
（媒体 → 播表 → 布局 → 栏目 → 计划 → 播放 → 报表）。

### Added

- **登录认证**：JWT + Spring Security 无状态认证，admin/operator 两种角色，默认管理员 `admin`（密码见使用手册），前端路由守卫
- **媒体管理**：图片 / 视频 / 字幕上传（按类型白名单与大小限制校验）、图片自动缩略图、分页 / 类型筛选 / 关键词搜索、预览、编辑、删除（同步删除磁盘文件）、视频时长手填
- **播表管理**：素材池多选加入、拖拽排序（vue-draggable-plus）、逐项播放时长、CRUD（更新为全量替换策略）
- **布局管理**：Konva.js（vue-konva）可视化编辑器——5 种分辨率预设、画布等比缩放、模块拖拽 / 缩放（最小 50×50）/ 右键设媒体类型 / 层级调整、参数表精确调参、模块配置以 JSON 存储
- **栏目管理**：布局 + 每模块绑定播表（模块类型与播表类型匹配校验）、播放类型（垫播 loop / 插播 insert / 紧急 emergency）、版面实时预览
- **计划管理**：日计划 / 周计划、时间段编排（HH:mm）、发布 / 撤销（发布事件通过 WebSocket 广播）、时间合法性校验
- **播放调度**：`GET /api/player/current` 按生效期 → 星期 → 时间段解析当前应播计划，多计划重叠按 `emergency > insert > loop` 优先级仲裁，支持 `at` 参数模拟时间
- **Web 播放端**：免登录全屏播放器，按布局等比渲染多模块并行播放（视频自然播完 / 图片轮播 / 字幕滚动），WebSocket 计划推送 + 30 秒轮询降级 + 断线 5 秒重连，设备心跳
- **播放报表**：播放日志批量上报，按日 / 按栏目 / 按素材聚合统计，ECharts 可视化
- **后端工程**：Spring Boot 3.5 Maven 九模块结构（common/system/material/playlist/layout/program/plan/player/admin），统一返回体与全局异常处理
- **前端工程**：Vue 3 + TypeScript + Vite + Element Plus + Pinia，管理后台与播放端同仓
- **部署**：Docker Compose（mysql + server + web/nginx，含 WebSocket 反向代理配置）
- **文档**：README、详细使用手册 USAGE.md、贡献指南、安全策略
- **测试**：后端 API 全链路冒烟测试脚本 `scripts/api-test.sh`（35 项断言，运行前自动清空业务数据）
- **CI**：GitHub Actions（后端 Maven 构建 + 前端 npm 构建）

### Fixed

- 修正原规划文档中的已知问题：播表列表 type+keyword 同传时只按 keyword 过滤；Konva Transformer 绑定到 Group 的坐标换算；Jackson 对 `zIndex` 属性名解析为 `zindex` 导致的空指针

### Security

- 默认管理员密码为演示用途，部署后请立即修改；JWT 密钥与数据库密码请在部署时更换（见 USAGE.md 第 4 章）

[Unreleased]: https://github.com/OWNER/REPO/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/OWNER/REPO/releases/tag/v1.0.0
