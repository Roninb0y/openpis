# OpenPIS-Lite

> 一个大学生友好的简版数字标牌 / 乘客信息发布系统（PIS），由《PIS媒体发布软件用户使用手册 v2.0》精简而来。
> 核心理念：**先闭环，后精致** —— 把"上传媒体 → 组播表 → 画布局 → 编栏目 → 排计划 → 终端播放 → 数据回流"这条主线跑通。

![Java](https://img.shields.io/badge/Java-17-blue) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green) ![Vue](https://img.shields.io/badge/Vue-3-42b883) ![Element Plus](https://img.shields.io/badge/Element%20Plus-2-blueviolet)

## 项目背景

数字标牌（Digital Signage）/ 乘客信息系统（PIS）广泛用于地铁站台屏、车载屏、楼宇广告机等场景。商业 PIS 软件动辄 141 页用户手册、几十个功能模块。本项目从其中提炼出**最小业务闭环**做开源实现，适合：

- 学习内容发布系统的完整业务链路（媒体 → 播表 → 布局 → 栏目 → 计划 → 播放 → 报表）
- 作为全栈 / 播放调度算法 / WebSocket 实时下发的实战项目

## 功能一览

| 模块 | 说明 |
|------|------|
| 登录认证 | JWT + Spring Security，admin/operator 两种角色，前端路由守卫 |
| 媒体管理 | 图片/视频/字幕上传、缩略图、分页/类型筛选/搜索、预览、删除、时长手填 |
| 播表管理 | 素材池多选加入、**拖拽排序**（vue-draggable-plus）、逐项播放时长、CRUD |
| 布局管理 | **Konva.js 可视化编辑器**：分辨率预设、模块拖拽/缩放/右键设类型/层级调整、参数表精确调参 |
| 栏目管理 | 布局 + 每个模块绑定播表，播放类型（垫播 loop / 插播 insert / 紧急 emergency），版面实时预览 |
| 计划管理 | 日计划 / 周计划、时间段编排、**发布/撤销**（WebSocket 实时通知播放端） |
| Web 播放端 | 全屏自适应、按布局渲染多模块并行播放（视频轮播/图片轮播/字幕滚动）、**WebSocket 推送 + 30s 轮询降级** |
| 播放报表 | 播放日志上报，ECharts 按日统计、栏目/素材排行 |

**播放调度算法**：同一时刻多个计划重叠时，按播放类型优先级自动仲裁（emergency > insert > loop），周计划按星期生效；时间段要求结束时间晚于开始时间。

<!-- 开源到 GitHub 后，把下面的 OWNER/REPO 替换为真实仓库地址即可启用 CI 徽章
[![CI](https://github.com/OWNER/REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/ci.yml)
-->

## 文档导航

| 文档 | 说明 |
|------|------|
| [USAGE.md](USAGE.md) | **详细使用手册**：安装升级 / 全部配置项 / 功能详解 / API 参考 / 典型示例 / FAQ / 故障排除 |
| [CONTRIBUTING.md](CONTRIBUTING.md) | 参与贡献指南（开发环境 / 提交约定 / 自测要求） |
| [CHANGELOG.md](CHANGELOG.md) | 版本变更记录（Keep a Changelog 格式） |
| [SECURITY.md](SECURITY.md) | 安全漏洞私下报告流程与部署安全建议 |
| [LICENSE](LICENSE) | MIT 开源协议 |
| [.env.example](.env.example) | 环境变量清单模板（仅变量名与说明，不含真实值） |

## 系统架构

```
┌─────────────────────────────────────────────────────────┐
│              Web 管理后台 (Vue 3 + TS + Element Plus)    │
│  媒体 │ 播表 │ 布局编辑器(Konva) │ 栏目 │ 计划 │ 报表    │
└──────────────────────┬──────────────────────────────────┘
                       │ REST API / WebSocket
┌──────────────────────▼──────────────────────────────────┐
│              后端 (Spring Boot 3.5 多模块 Maven)          │
│  openpis-common  公共组件（统一返回体/JWT/异常/文件配置）  │
│  openpis-system  登录认证、用户                           │
│  openpis-material / playlist / layout / program / plan   │
│  openpis-player  计划解析调度、WebSocket下发、日志、报表   │
│  openpis-admin   启动入口                                 │
└──────────┬───────────────────────────┬──────────────────┘
           │                           │
   ┌───────▼───────┐          ┌────────▼────────┐
   │  MySQL 8.0    │          │ 本地文件存储      │
   │  (业务数据)    │          │ /uploads/**     │
   └───────────────┘          └─────────────────┘

   ┌─────────────────────────────────────────────┐
   │  Web 播放端 /player?code=xxx（免登录全屏）     │
   │  拉取当前计划 → 按布局渲染 → WS 接收计划变更    │
   └─────────────────────────────────────────────┘
```

## 技术栈

- **后端**：Java 17、Spring Boot 3.5、Spring Security + JJWT、Spring Data JPA、MySQL 8.0、Spring WebSocket、Thumbnailator
- **前端**：Vue 3 + TypeScript + Vite、Element Plus、Pinia、Vue Router、Axios、Konva.js (vue-konva)、vue-draggable-plus、ECharts
- **工程**：Maven 多模块、Docker Compose、Nginx

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
docker compose up -d --build
```

启动完成后：

- 管理后台：http://localhost:8081 （默认账号 `admin / 123456`）
- 播放端：http://localhost:8081/player?code=TV-01
- MySQL 已内置于 compose（宿主机端口 3307）

### 方式二：本地开发

前置要求：JDK 17+、Node.js 18+、MySQL 8.0

```bash
# 1. 建库
mysql -uroot -p -e "CREATE DATABASE openpis_lite DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

# 2. 修改数据库连接（openpis-server/openpis-admin/src/main/resources/application.yml）

# 3. 启动后端（表结构由 JPA 自动创建）
cd openpis-server
mvn -s settings.xml -DskipTests package
java -jar openpis-admin/target/openpis-admin-1.0.0.jar

# 4. 启动前端
cd ../openpis-web
npm install
npm run dev
```

访问 http://localhost:5173 （管理后台，admin/123456），播放端在 http://localhost:5173/player?code=TV-01

### 演示流程（约 5 分钟）

1. 登录管理后台 → **媒体管理** 上传几张图片、一段字幕 txt（视频需手填时长）
2. **播表管理** 新建播表：素材池勾选 → 添加到播表 → 拖拽排序 → 保存
3. **布局管理** 新建布局：选 1920×1080 → 添加 3 个模块 → 拖拽摆放、右键设类型 → 保存
4. **栏目管理** 新建栏目：选布局 → 为每个模块绑定播表 → 播放类型选"垫播"
5. **计划管理** 新建日计划：选栏目、设 00:00–23:59 → 保存 → **发布**
6. 打开播放端 http://localhost:5173/player?code=TV-01 → 画面按布局自动播放
7. 修改计划/再发布一个"紧急"计划 → 播放端秒级自动切换（WebSocket）
8. **播放报表** 查看播放次数统计

## 项目结构

```
pis1/
├── openpis-server/            # 后端（Maven 多模块）
│   ├── openpis-common/        # 统一返回体、异常、JWT、文件配置
│   ├── openpis-system/        # 登录认证、用户
│   ├── openpis-material/      # 媒体管理
│   ├── openpis-playlist/      # 播表管理
│   ├── openpis-layout/        # 布局管理
│   ├── openpis-program/       # 栏目管理
│   ├── openpis-plan/          # 计划管理（含发布/撤销事件）
│   ├── openpis-player/        # 播放解析调度、WebSocket、心跳、日志、报表
│   ├── openpis-admin/         # 启动入口 + application.yml
│   └── Dockerfile
├── openpis-web/               # 前端（Vue 3 + Vite）
│   └── src/
│       ├── api/               # 接口封装（axios 统一拦截）
│       ├── views/             # login/dashboard/material/playlist/layout/program/plan/report/player
│       ├── stores/ router/ utils/
│   ├── Dockerfile  nginx.conf
├── docker-compose.yml
├── docs/                      # 项目规划文档
└── scripts/api-test.sh        # 后端 API 全链路冒烟测试（35 项断言）
```

## 核心接口

| 接口 | 说明 |
|------|------|
| `POST /api/auth/login` | 登录，返回 JWT |
| `POST /api/materials/upload` | 上传媒体（multipart，视频支持 duration 参数） |
| `GET/POST/PUT/DELETE /api/playlists` | 播表 CRUD（更新为全量替换） |
| `GET/POST/PUT/DELETE /api/layouts` | 布局 CRUD（modules 存 JSON） |
| `GET/POST/PUT/DELETE /api/programs` | 栏目 CRUD（模块绑定播表 + 类型校验） |
| `POST /api/plans/{id}/publish` / `unpublish` | 计划发布/撤销（触发 WS 广播） |
| `GET /api/player/current?code=&at=` | **当前应播计划解析**（at 可模拟时间，便于测试） |
| `WS /ws/player?code=` | 播放端 WebSocket（计划变更推送 + 心跳） |
| `POST /api/player/log` / `GET /api/report/*` | 播放日志上报与报表统计 |

## 设计说明

- **JSON 字段**：`layout.modules`、`program.modulePlaylists`、`plan.schedule` 使用 MySQL JSON 列存储复杂结构，减少关联表数量；查询需求复杂后再拆表。
- **调度算法**：`PlayerService.resolveCurrent()` 按生效期 → 星期 → 时间段 → 播放类型优先级逐层过滤，多计划重叠时 emergency > insert > loop，同优先级取更新时间较新者。
- **计划变更推送**：发布/撤销发布 Spring 事件 → WebSocket Handler 向全部在线终端广播 `PLAN_CHANGED` → 播放端重拉 `/api/player/current`；WebSocket 不可用时 30 秒轮询兜底。
- **视频时长**：MVP 不做 FFmpeg 解析，由上传者手填；播放端按"播表项时长 > 媒体时长 > 类型默认值"取节奏。

## 已知边界（MVP 范围）

- 无审核工作流、无细粒度 RBAC（仅 admin/operator 展示角色）
- 视频缩略图暂为占位图标；Flash 模块作为占位类型（绑定图片播表兜底）
- 设备仅心跳记录，无设备管理页面

## 路线图

- [ ] FFmpeg 视频时长解析 + 截帧
- [ ] MinIO 对象存储、Redis 缓存
- [ ] 审核工作流、CSV 导入、批量上传
- [ ] 实时新闻抓取、直播流接入
- [ ] Android / Windows 原生播放器（ExoPlayer / Electron）
- [ ] GitHub Actions CI、单元测试

## 测试

### 播放器浏览器回归测试

需要 Node.js 20+。在真实 Chromium 中验证单素材循环、30 秒轮询期间的图片/字幕计时、
播表变更、迟到字幕响应和桌面/竖屏显示。接口与媒体使用测试数据，不需要启动后端或数据库。

```bash
cd openpis-web
npm ci
npx playwright install chromium
npm test
```

测试会自动启动独立的 Vite 服务（127.0.0.1:4173）；请保持该端口空闲。
失败时的浏览器 trace 与显示检查截图保存在 `openpis-web/test-results/`，不提交到 Git。
GitHub Actions 的前端任务也会运行这些测试。

### 后端 API 冒烟测试

**仅在可丢弃的测试数据库上运行：脚本会先删除已有业务数据。**

```bash
# 后端启动后执行（需 Git Bash / bash）
bash scripts/api-test.sh
```

覆盖登录鉴权、媒体、播表、布局、栏目、计划、播放解析（含优先级、周计划、跨零点）、日志报表共 35 项断言。
