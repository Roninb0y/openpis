# 使用手册

> OpenPIS-Lite 简版数字标牌 / 乘客信息发布系统（PIS）
> 本手册面向第一次使用的用户，所有命令、路径、参数均与项目代码一致。

---

## 1. 简介

### 1.1 项目用途

OpenPIS-Lite 是一个开源的简版数字标牌 / 乘客信息发布系统，实现了内容发布系统的最小业务闭环：

```
上传媒体 → 组成播表 → 设计布局 → 布局+播表绑成栏目 → 栏目排成计划 → Web 播放端按时自动播放 → 播放数据回流报表
```

主要能力：

| 能力 | 说明 |
|------|------|
| 媒体管理 | 上传图片 / 视频 / 字幕文本，生成缩略图，支持搜索、预览、删除 |
| 播表管理 | 把多个素材按顺序组织成播放列表，支持拖拽排序、逐项时长 |
| 布局管理 | Konva.js 可视化编辑器，在画布上拖拽 / 缩放模块，设计多区域版面 |
| 栏目管理 | 选定布局，为每个模块绑定对应类型的播表，设置播放类型（垫播 / 插播 / 紧急） |
| 计划管理 | 日计划 / 周计划编排时间段，支持发布 / 撤销，发布后实时通知播放端 |
| Web 播放端 | 全屏网页播放器，按计划与布局自动播放，WebSocket 实时切换 |
| 播放报表 | 播放日志自动上报，按日 / 按栏目 / 按素材统计播放次数 |

### 1.2 适用场景

- 学习内容发布系统的完整业务链路与调度算法
- 地铁站台屏、车载屏、楼宇信息屏、校园 / 展厅信息屏等**单机或小规模**演示与原型验证
- 作为全栈开发、播放调度算法、WebSocket 实时下发的实战 / 教学项目

### 1.3 不适用场景

- 需要审核工作流、细粒度权限（RBAC）、多租户的**生产级企业部署**（当前仅 admin / operator 两种角色，且角色仅用于展示，接口层未做权限差异控制 [待确认：是否需要为 operator 增加接口级限制]）
- 大规模终端集群管理（无设备分组、批量下发、负载均衡设计）
- 需要高可用、容灾、CDN 分发的商业运营场景
- Flash 内容真实播放（Flash 模块仅作为版面占位类型，用图片播表兜底）

---

## 2. 系统要求

### 2.1 操作系统

- Windows 10 / 11、Linux（Ubuntu 20.04+、CentOS 8+）、macOS 均可
- Docker 方式部署需要本机可运行 Docker Engine（Windows 上为 Docker Desktop）

### 2.2 运行时版本

| 组件 | 最低版本 | 说明 |
|------|---------|------|
| JDK | 17 | 后端按 Java 17 编译（`maven.compiler.release=17`）；在 JDK 17~25 上验证可运行 |
| Node.js | 18 | 前端 Vite 5 要求；建议 20+ |
| MySQL | 8.0 | 使用 JSON 列类型，8.0 以下不可用 |
| Maven | 3.9+ | 仅构建后端时需要（也可使用 IDEA 内置 Maven） |

### 2.3 依赖软件

| 软件 | 用途 | 是否必需 |
|------|------|---------|
| Git | 获取源码、版本管理 | 推荐 |
| MySQL 8.0 | 业务数据存储 | 必需（Docker 方式由 compose 提供） |
| Maven 3.9+ | 后端构建 | 本地方式必需 |
| Docker + Docker Compose | 一键部署 | 可选 |
| IDEA / VSCode | 开发调试 | 可选 |

默认端口占用：后端 `8080`、前端开发服务器 `5173`、MySQL `3306`；Docker 方式下 Web 为 `8081`、MySQL 映射为宿主机 `3307`。启动前请确认端口未被占用。

---

## 3. 安装与升级

### 3.1 全新安装（本地方式）

```bash
# 1. 获取源码
git clone <仓库地址>        # [待确认：GitHub 仓库地址未配置]
cd pis1

# 2. 创建数据库（表结构由后端 JPA 自动创建，无需手工建表）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS openpis_lite DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 3. 修改数据库连接配置
#    编辑 openpis-server/openpis-admin/src/main/resources/application.yml
#    修改 spring.datasource.username / password 为你的 MySQL 账号

# 4. 构建并启动后端（在 openpis-server 目录下执行）
cd openpis-server
mvn -s settings.xml -DskipTests package
java -jar openpis-admin/target/openpis-admin-1.0.0.jar
# 预期：日志输出 "Started OpenPisApplication in x seconds"，
#       以及 "默认管理员账号已创建：admin / 123456"

# 5. 启动前端（新开一个终端）
cd openpis-web
npm install
npm run dev
# 预期：输出 "VITE v5.x ready" 与 "Local: http://localhost:5173/"
```

> 说明：
> - `-s settings.xml` 使用仓库内置的阿里云 Maven 镜像加速，国内网络建议保留。
> - `npm install` 若因本机 npm 缓存目录不可写而失败，追加参数：
>   `npm install --cache "$USERPROFILE/AppData/Local/npm-cache" --registry=https://registry.npmmirror.com`
> - 后端工作目录决定文件存储位置：在 `openpis-server` 目录下启动时，上传文件保存在 `openpis-server/uploads/`。

### 3.2 全新安装（Docker 方式）

```bash
cd pis1
docker compose up -d --build
```

启动完成后：

| 入口 | 地址 |
|------|------|
| 管理后台 | http://localhost:8081 |
| 播放端 | http://localhost:8081/player?code=TV-01 |
| MySQL（宿主机） | localhost:3307（账号 root / 438438） |

> 注意：该 compose 编写于标准 Docker 环境（含 mysql、server、web/nginx 三个服务），发布前未在真实 Docker 环境中完整实测 [待确认：建议首次使用时观察 `docker compose logs -f server` 直至出现 "Started OpenPisApplication"]。

### 3.3 升级方法

```bash
# 1. 备份
mysqldump -uroot -p openpis_lite > openpis_lite_backup.sql
# 同时备份上传文件目录 openpis-server/uploads/

# 2. 更新源码
git pull origin main

# 3. 重新构建并重启
cd openpis-server && mvn -s settings.xml -DskipTests package
java -jar openpis-admin/target/openpis-admin-1.0.0.jar &

cd ../openpis-web && npm install && npm run build   # 开发模式则改为 npm run dev
```

升级说明：
- 表结构由 JPA `ddl-auto: update` 自动维护：**新增**字段 / 表会自动创建；
- **删除字段、修改字段类型不会自动执行**，需要手工执行 SQL 迁移 [待确认：版本发布时随版本说明提供迁移 SQL]。

### 3.4 卸载方法

见 [11. 卸载 / 数据清理](#11-卸载--数据清理)。

---

## 4. 配置

### 4.1 配置文件位置

| 文件 | 作用 |
|------|------|
| `openpis-server/openpis-admin/src/main/resources/application.yml` | 后端唯一配置文件（所有模块共用） |
| `openpis-web/.env.development` | 前端开发环境变量（`npm run dev` 生效） |
| `openpis-web/.env.production` | 前端生产构建变量（`npm run build` 生效） |
| `docker-compose.yml` | Docker 部署的服务编排与环境变量 |
| `openpis-web/nginx.conf` | Docker 中 nginx 的反向代理规则 |

### 4.2 后端配置项（application.yml）

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `server.port` | `8080` | 后端 HTTP 端口 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/openpis_lite?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true` | 数据库连接串（库名 `openpis_lite`） |
| `spring.datasource.username` | `root` | 数据库用户名 |
| `spring.datasource.password` | `"438438"` | 数据库密码，**部署时务必修改** |
| `spring.jpa.hibernate.ddl-auto` | `update` | 表结构自动维护（新增字段/表） |
| `spring.servlet.multipart.max-file-size` | `500MB` | 单文件上传上限 |
| `spring.servlet.multipart.max-request-size` | `500MB` | 单请求上传上限 |
| `file.upload.path` | `./uploads/` | 上传文件存储目录（相对后端进程工作目录） |
| `file.access.url` | `http://localhost:8080/uploads` | 前端访问上传文件的 URL 前缀，**换端口 / 域名部署时必须同步修改** |
| `jwt.secret` | `openpis_secret_key_please_change_to_random_string_32bytes` | JWT 签名密钥，**至少 32 字节，生产环境必须更换** |
| `jwt.expiration` | `86400000` | Token 有效期（毫秒），默认 1 天 |
| `logging.level.com.openpis` | `info` | 项目日志级别 |

### 4.3 前端环境变量

| 变量 | 开发环境默认 | 生产构建默认 | 说明 |
|------|-------------|-------------|------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | `/api` | 后端 API 地址；播放端 WebSocket 地址由它推导（http→ws，去掉 `/api`） |
| `VITE_FILE_BASE_URL` | `http://localhost:8080/uploads` | `/uploads` | 上传文件访问地址前缀 |

### 4.4 Docker 环境变量（docker-compose.yml）

Spring Boot 支持环境变量覆盖同名配置（下划线映射点号）：

| 环境变量 | 对应配置 | compose 中的值 |
|---------|---------|---------------|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://mysql:3306/openpis_lite?...` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `"438438"` |
| `FILE_ACCESS_URL` | `file.access.url` | `http://localhost:8081/uploads`（浏览器统一走 nginx 8081） |
| `MYSQL_ROOT_PASSWORD` | MySQL 容器 root 密码 | `"438438"` |
| `MYSQL_DATABASE` | MySQL 容器初始化库名 | `openpis_lite` |

### 4.5 示例配置

修改后端为本机 3307 端口 MySQL、自定义密钥的示例（`application.yml` 相关片段）：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3307/openpis_lite?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: "你的MySQL密码"
    driver-class-name: com.mysql.cj.jdbc.Driver

file:
  upload:
    path: ./uploads/
  access:
    url: http://localhost:8080/uploads

jwt:
  secret: please_change_me_to_a_random_string_at_least_32_bytes!
  expiration: 86400000
```

---

## 5. 快速上手

从零到在播放端看到画面的最小步骤（本地方式，后端与前端已按 3.1 启动）：

### 第 1 步：登录管理后台

浏览器打开 http://localhost:5173 ，未登录会自动跳转到登录页。

- 输入默认账号 `admin / 123456`（登录页已预填），点击「登录」。
- **预期结果**：进入后台首页，左上角显示「OpenPIS-Lite」，左侧有 7 个菜单，顶栏显示「欢迎，admin（admin）」。

### 第 2 步：上传素材

左侧菜单 →「媒体管理」→「上传媒体」→ 选择文件 →（视频需填写时长秒数）→「上传」。

- **预期结果**：提示"上传成功"，列表出现新记录；图片有缩略图，可直接点「预览」查看。

### 第 3 步：组建播表

「播表管理」→「新增播表」→ 填写名称、选择类型 → 左侧素材池勾选素材 →「添加到播表」→ 拖拽调整顺序 →「保存」。

- **预期结果**：回到列表页，新播表显示媒体数量。

### 第 4 步：设计布局

「布局管理」→「新增布局」→ 填写名称、选择分辨率（如 1920×1080）→「+ 添加模块」→ 在画布上**拖拽移动**模块、**拖四角手柄缩放**、**右键**设置模块类型 →「保存」。

- **预期结果**：回到列表页，新布局显示分辨率与模块数。

### 第 5 步：创建栏目

「栏目管理」→「新增栏目」→ 填写名称 → 选择第 4 步的布局 → 播放类型选「垫播」→ 右侧表格为每个模块下拉选择第 3 步的播表 →「保存」。

- **预期结果**：列表出现新栏目，绑定模块数正确；模块类型与播表类型不匹配时会被拒绝并提示。

### 第 6 步：排期并发布

「计划管理」→「新增计划」→ 填写名称、类型选「日计划」、选择生效日期（含今天）→「+ 添加时间段」→ 选择栏目、开始 00:00、结束 23:45 →「保存」→ 回列表页点击「**发布**」。

- **预期结果**：状态变为绿色「已发布」。

### 第 7 步：观看播放

点击顶栏「打开播放器」（新窗口打开 `/player?code=DEMO01`），或手动访问 `http://localhost:5173/player?code=TV-01`。

- **预期结果**：全屏黑底画面上，各模块按布局位置并行播放：视频/图片模块轮播播表内容，字幕模块滚动显示文本；约 30 秒内自动同步计划变更（WebSocket 秒级、轮询兜底）。

### 第 8 步：查看报表

「播放报表」→ **预期结果**：近 7 天播放次数柱状图、栏目播放排行、素材播放 TOP 10 出现数据。

---

## 6. 功能详解

### 6.1 登录认证

- **作用**：保护管理后台所有接口；播放端不需要登录（以设备码标识身份）。
- **使用方法**：登录页输入用户名密码；登录后 Token 保存在浏览器 `localStorage`（键：`token` / `username` / `role`），所有请求自动携带 `Authorization: Bearer <token>`。
- **参数**：无配置项；Token 有效期由 `jwt.expiration` 控制（默认 24 小时）。
- **角色**：`admin`（管理员）与 `operator`（操作员），登录接口返回角色并在顶栏展示；当前版本角色**不影响接口权限** [待确认]。
- **示例**：默认账号 `admin / 123456`，首次启动后端时自动创建（见 `DataInitializer`）。
- **注意事项**：
  - 忘记密码：删除 `sys_user` 表中 admin 行后重启后端，会自动重建为 `123456`（见 FAQ）。
  - 401 时前端会清空本地凭证并跳回登录页。

### 6.2 媒体管理

- **作用**：管理所有可播放的素材。
- **使用方法**：「媒体管理」页 →「上传媒体」对话框选文件 → 上传；列表支持按类型筛选、按标题搜索、分页；每行可「预览 / 编辑 / 删除」。
- **参数（上传对话框）**：

  | 参数 | 说明 |
  |------|------|
  | 文件 | 必选。支持的扩展名：图片 `jpg/jpeg/png/gif/bmp/webp`、视频 `mp4/avi/mov/mkv/flv`、文本 `txt` |
  | 视频时长（秒） | 仅视频文件出现；1~86400；MVP 不自动解析，**必须手填**，播放器按此节奏播放 |
  | 描述 | 可选 |

- **大小限制**：图片 ≤ 10MB、视频 ≤ 200MB、文本 ≤ 1MB（服务端校验）；请求总体受 `500MB` multipart 上限保护。
- **示例**：上传 `宣传片.mp4` 并填时长 180 → 播放器中该素材将按 180 秒节奏编排。
- **注意事项**：
  - 文件以 UUID 重命名存储在 `uploads/materials/{image|video|text}/`；图片自动生成 300×300 等比缩略图（`thumb_` 前缀）。
  - 删除媒体会**同时删除磁盘文件**，且引用它的播表项会显示"（素材已删除）"。
  - 视频缩略图暂为占位图标 [待确认：FFmpeg 截帧在扩展阶段实现]。

### 6.3 播表管理

- **作用**：把素材按顺序组织成播放列表，是播放的最小内容单元。
- **使用方法**：「播表管理」→「新增播表」；左侧素材池点击选中（可多选）→「添加到播表」；右侧列表**按住行首图标拖拽排序**；每项可设单独时长（秒），不填使用媒体原时长。
- **参数**：

  | 参数 | 说明 |
  |------|------|
  | 名称 | 必填 |
  | 类型 | `video` 视频播表（可混排图片）/ `image` 图片播表 / `text` 字幕播表；素材池会按类型过滤 |
  | 播表项 | `{素材, 顺序, 可选时长}`；更新时整体全量替换 |

- **示例**：建一个"视频播表"，依次加入视频 A（不填时长）、图片 B（填 10 秒）→ 播放顺序为 A 播完自然接 B，B 显示 10 秒，循环。
- **注意事项**：编辑保存采用**全量替换**（先删旧项再插新项）；删除播表会级联删除其全部播表项。

### 6.4 布局管理（可视化编辑器）

- **作用**：定义一个屏幕版面：画布分辨率 + 若干矩形模块（位置、大小、类型、层级）。播放端按此布局渲染。
- **使用方法**：「布局管理」→「新增布局」进入编辑器。
  - 「+ 添加模块」在画布左上角生成新模块；
  - **拖拽**模块移动位置；**选中后拖四角手柄缩放**（最小 50×50，自动限制在画布内）；
  - 画布内**右键**模块 → 设置媒体类型（视频=绿 / 图片=红 / 字幕=黄 / Flash=蓝）；
  - 下方「模块参数表」可精确修改名称 / X / Y / 宽 / 高，以及层级（置顶 / 上移 / 下移 / 置底）。
- **参数**：

  | 参数 | 说明 |
  |------|------|
  | 名称 | 必填 |
  | 分辨率 | 预设：`1920x1080`、`1080x1920`（竖屏）、`1366x768`、`1024x768`、`800x600` |
  | 模块 | `{id, 名称, 类型 video/image/text/flash, x, y, w, h, zIndex}`；坐标为**原始分辨率像素** |

- **示例**：1920×1080 画布放三个模块：主视频区 (0,0,1280,720)、右字幕区 (1280,0,640,720)、底部图片区 (0,720,1920,360)。
- **注意事项**：
  - 编辑器画布按最大 800px 宽**等比缩放显示**，保存的是原始分辨率坐标，播放端再等比缩放到窗口。
  - 改分辨率不会自动缩放已有模块，建议先定分辨率再排版。
  - 删除布局前请确认没有栏目在引用它。

### 6.5 栏目管理

- **作用**：把"布局"和"播表"结合成一次可排期的播出内容。
- **使用方法**：「栏目管理」→「新增栏目」→ 选布局 → 右侧预览实时渲染版面 → 在绑定表中为每个模块下拉选择播表（可留空）→ 选播放类型 →「保存」。
- **参数**：

  | 参数 | 说明 |
  |------|------|
  | 名称 | 必填 |
  | 布局 | 必选，决定模块列表 |
  | 播放类型 | `loop` 垫播（日常轮循）/ `insert` 插播（临时内容）/ `emergency` 紧急（最高优先级） |
  | 模块绑定 | 模块 → 播表；类型匹配规则：视频模块→视频播表、图片模块→图片播表、字幕模块→字幕播表、Flash 模块→图片播表 |

- **示例**：紧急通知栏目 = 标准布局 + 主视频区绑一个视频播表，播放类型选「紧急」。
- **注意事项**：类型不匹配会被前端过滤选项、后端再次校验拒绝（提示"只能绑定 xx 类型的播表"）；未绑定播表的模块在播放端显示灰色占位。

### 6.6 计划管理

- **作用**：把栏目按日期与时间段排期，发布后播放端按时间自动切换。
- **使用方法**：「计划管理」→「新增计划」→ 填名称、选类型（日计划 / 周计划）、选生效日期范围 → 配置时间段 →「保存」（草稿）→ 回列表点「发布」。
- **参数**：

  | 参数 | 说明 |
  |------|------|
  | 名称 | 必填 |
  | 计划类型 | `daily` 日计划（生效期内每天生效）/ `weekly` 周计划（按星期几生效，1~7 = 周一~周日） |
  | 生效日期 | 起止日期，开始不能晚于结束 |
  | 时间段 | 栏目（必选）+ 开始 / 结束时间（HH:mm，UI 按 15 分钟步进选择） |

- **示例**：周计划"周一 08:00–10:00 播紧急栏目" + 日计划"每天 00:00–23:45 播垫播栏目"。
- **注意事项**：
  - **结束时间必须晚于开始时间**，不支持跨零点时间段（校验会直接拒绝）。
  - 计划只有**发布**（状态=已发布）后才参与播放调度；「撤销」后立即退出调度并通知播放端。
  - 多个计划时间重叠时由播放端按栏目播放类型优先级仲裁（见 6.7）。
  - 已发布计划被编辑后，播放端会在下次刷新时同步。

### 6.7 Web 播放端

- **作用**：免登录全屏播放器，按当前时间应播的计划与布局渲染画面。
- **使用方法**：浏览器打开 `http://<前端地址>/player?code=<设备编码>`，例如 `/player?code=TV-01`；也可点后台顶栏「打开播放器」。
- **参数**：

  | 参数 | 说明 |
  |------|------|
  | `code`（URL） | 设备编码，可选；不传时自动生成 `DEV-XXXXXXXX` 并存入 localStorage（键 `openpis_device_code`） |

- **播放行为**：
  - 按布局等比缩放渲染各模块；每个模块独立循环播放其绑定的播表：视频素材自然播完接下一个，图片默认 10 秒/张，字幕滚动 30 秒/条（播表项或媒体上单独设置的时长优先）。
  - **当前应播计划解析**：先按生效期过滤 → 周计划按星期过滤 → 按时间段命中 → 多个命中时按播放类型优先级 `emergency > insert > loop` 取最高，同优先级取更新时间较新的计划。
  - **计划变更感知**：WebSocket（`/ws/player`）推送秒级切换；WebSocket 不可用时 30 秒轮询兜底；连接断开每 5 秒重连。
  - 每 60 秒向 `/api/device/heartbeat` 上报心跳。
- **示例**：两个显示器分别打开 `/player?code=TV-01` 与 `/player?code=TV-02`，即为两台独立终端（都有心跳记录）。
- **注意事项**：视频需要浏览器支持的编码（建议 H.264 编码 MP4）；浏览器自动播放策略要求视频**静音自动播放**（已内置 muted）；无可用计划时显示"当前无播放计划"并持续等待。

### 6.8 播放报表

- **作用**：统计播放次数，验证业务闭环的"数据回流"。
- **使用方法**：播放端每播完一条素材自动上报一条日志（含设备、栏目、素材、起止时间）；「播放报表」页查看近 7 天柱状图、栏目排行、素材 TOP 10。
- **参数**：无 UI 参数；接口参数见第 7 章。
- **示例**：播放器运行 10 分钟后打开报表，应能看到对应次数。
- **注意事项**：素材 / 栏目删除后，历史日志保留并显示为"已删除"；日期柱状图缺数据的天数自动补 0。

---

## 7. 命令 / API 参考

### 7.1 常用命令

| 命令 | 说明 |
|------|------|
| `cd openpis-server && mvn -s settings.xml -DskipTests package` | 构建后端，产物 `openpis-admin/target/openpis-admin-1.0.0.jar` |
| `java -jar openpis-admin/target/openpis-admin-1.0.0.jar` | 启动后端（在 `openpis-server` 目录执行，`uploads/` 生成在该目录） |
| `cd openpis-web && npm install` | 安装前端依赖 |
| `cd openpis-web && npm run dev` | 启动前端开发服务器（5173，热更新） |
| `cd openpis-web && npm run build` | 生产构建，产物 `dist/` |
| `docker compose up -d --build` | Docker 一键部署 |
| `docker compose down` | 停止并移除容器（数据卷保留） |
| `docker compose down -v` | 停止并**删除数据卷**（数据库数据清空） |
| `bash scripts/api-test.sh` | 后端 API 冒烟测试（35 项断言）。**警告：会先清空库内业务数据** |

### 7.2 接口通用约定

- Base URL：`http://localhost:8080/api`（Docker 部署为 `http://localhost:8081/api`）
- 除注明"免鉴权"外，所有接口需要请求头 `Authorization: Bearer <token>`
- 统一返回体：

```json
{ "code": 200, "message": "success", "data": { } }
```

- **业务错误**返回 HTTP 200，`code` 为业务错误码；**未登录 / Token 失效**返回 HTTP 401：

```json
{ "code": 401, "message": "未登录或登录已过期", "data": null }
```

### 7.3 接口列表

#### 认证（免鉴权）

| 接口 | 说明 |
|------|------|
| `POST /api/auth/login` | 登录 |
| `GET /api/hello` | 健康检查 |

**POST /api/auth/login**

- Body：`{"username": "admin", "password": "123456"}`
- 成功返回 data：`{"token": "eyJ...", "username": "admin", "role": "admin"}`
- 错误：`401 用户名或密码错误`

#### 媒体

| 接口 | 说明 |
|------|------|
| `POST /api/materials/upload` | 上传（multipart：`file` 必选，`description` 可选，`duration` 视频时长秒，可选） |
| `GET /api/materials?page=1&size=10&type=&keyword=` | 分页列表（`type`: image/video/text） |
| `GET /api/materials/{id}` | 详情 |
| `PUT /api/materials/{id}` | 编辑：`{"title","description","duration"}`（均可选） |
| `DELETE /api/materials/{id}` | 删除（同步删除磁盘文件） |

- 成功返回 data 为 Material 对象：`{"id","title","type","filePath","thumbnail","duration","description","status","createdAt"}`
- 错误：`500 不支持的文件类型: xxx`、`500 图片大小不能超过 10MB` / `视频大小不能超过 200MB` / `文本大小不能超过 1MB`、`404 媒体不存在`

#### 播表

| 接口 | 说明 |
|------|------|
| `GET /api/playlists?page&size&type&keyword` | 分页列表（含 `itemCount`） |
| `POST /api/playlists` | 创建：`{"name","type":"video|image|text","items":[{"materialId":1,"sortOrder":0,"duration":30}]}`（`duration` 可选） |
| `GET /api/playlists/{id}` | 详情（含每项的媒体标题 / 路径 / 缩略图 / 原时长） |
| `PUT /api/playlists/{id}` | 更新（items 全量替换） |
| `DELETE /api/playlists/{id}` | 删除（级联删除 items） |

- 错误：`500 播表名称不能为空`、`500 播表类型不合法`、`500 播表不存在`

#### 布局

| 接口 | 说明 |
|------|------|
| `GET /api/layouts?page&size&keyword` | 分页列表（含 `moduleCount`） |
| `POST /api/layouts` | 创建：`{"name","resolution":"1920x1080","width":1920,"height":1080,"modules":[{"id":"m1","name":"主视频区","type":"video","x":0,"y":0,"w":1280,"h":720,"zIndex":1}]}` |
| `GET /api/layouts/{id}` | 详情（modules 为数组） |
| `PUT /api/layouts/{id}` | 更新（结构同创建） |
| `DELETE /api/layouts/{id}` | 删除 |

- 错误：`500 布局名称不能为空`、`500 分辨率不合法`、`500 布局不存在`

#### 栏目

| 接口 | 说明 |
|------|------|
| `GET /api/programs?page&size&keyword` | 分页列表（含 `layoutName`、`moduleCount`） |
| `POST /api/programs` | 创建：`{"name","layoutId":1,"playType":"loop|insert|emergency","modulePlaylists":[{"moduleId":"m1","playlistId":3}]}` |
| `GET /api/programs/{id}` | 详情（modules 含模块名 / 类型 / 绑定播表信息） |
| `PUT /api/programs/{id}` | 更新 |
| `DELETE /api/programs/{id}` | 删除 |

- 错误：`500 栏目名称不能为空`、`500 布局不存在`、`500 播放类型不合法`、`500 模块「x」（y）只能绑定 z 类型的播表`、`500 绑定的播表不存在: {id}`、`500 栏目不存在`

#### 计划

| 接口 | 说明 |
|------|------|
| `GET /api/plans?page&size&status&keyword` | 分页列表（`status`: 0 草稿 / 1 已发布，可选） |
| `POST /api/plans` | 创建（草稿）：`{"name","planType":"daily|weekly","startDate":"2026-09-13","endDate":"2026-09-13","schedule":{"items":[{"programId":1,"startTime":"08:00","endTime":"12:00"}]}}`；周计划每项需加 `"dayOfWeek":1`（1~7=周一~周日） |
| `GET /api/plans/{id}` | 详情（items 含 programName / playType） |
| `PUT /api/plans/{id}` | 更新 |
| `DELETE /api/plans/{id}` | 删除 |
| `POST /api/plans/{id}/publish` | 发布（触发 WebSocket 广播） |
| `POST /api/plans/{id}/unpublish` | 撤销（触发 WebSocket 广播） |

- 错误：`500 计划类型必须是 daily 或 weekly`、`500 生效日期不能为空`、`500 开始日期不能晚于结束日期`、`500 至少需要一条时间段配置`、`500 时间段配置中引用了不存在的栏目`、`500 周计划的时间段必须指定星期（1~7）`、`500 时间格式必须为 HH:mm，如 08:30`、`500 结束时间必须晚于开始时间`、`500 计划不存在`

#### 播放端（均免鉴权）

| 接口 | 说明 |
|------|------|
| `GET /api/player/current?code=TV-01&at=2026-09-13T10:00:00` | 当前应播计划。`code` 可选（顺带心跳）；`at` 可选，ISO 本地时间，用于模拟测试。无命中计划时 `data` 为 `null` |
| `POST /api/player/log` | 批量上报播放日志：`[{"deviceCode":"TV-01","programId":1,"materialId":2,"startedAt":"2026-09-13 10:00:00","endedAt":"2026-09-13 10:00:30"}]`（时间兼容 `yyyy-MM-dd HH:mm:ss`、ISO 本地时间、ISO Instant 三种格式） |
| `POST /api/device/heartbeat` | 设备心跳：`{"code":"TV-01","name":"1号屏"}`，返回设备 id |
| `WS /ws/player?code=TV-01` | WebSocket：终端发 `PING` 收 `PONG`；服务端广播 `{"type":"PLAN_CHANGED"}` 表示需重拉 current |

`GET /api/player/current` 成功返回 data 结构：

```json
{
  "planId": 1, "planName": "全天垫播计划",
  "programId": 1, "programName": "默认垫播栏目", "playType": "loop",
  "layoutWidth": 1920, "layoutHeight": 1080,
  "modules": [
    {
      "moduleId": "m1", "name": "主视频区", "type": "video",
      "x": 0, "y": 0, "w": 1280, "h": 720, "zIndex": 1,
      "playlist": {
        "id": 1, "name": "主视频播表", "type": "video",
        "items": [
          { "materialId": 4, "title": "a.mp4", "materialType": "video",
            "url": "http://localhost:8080/uploads/materials/video/x.mp4", "duration": 30 }
        ]
      }
    },
    { "moduleId": "m2", "...": "...", "playlist": null }
  ]
}
```

- 素材建议时长取值优先级：播表项 `duration` > 媒体 `duration` > 类型默认（图片 10 秒 / 字幕 30 秒 / 视频 0=自然播完）。

#### 报表（需鉴权）

| 接口 | 说明 |
|------|------|
| `GET /api/report/daily?days=7` | 近 N 天逐日播放次数（N 限 1~90，缺数据补 0） |
| `GET /api/report/program` | 按栏目聚合播放次数 |
| `GET /api/report/material?limit=10` | 按素材聚合 TOP N（limit 限 1~100） |

### 7.4 错误码汇总

| code | 含义 | 典型场景 |
|------|------|---------|
| 200 | 成功 | — |
| 400 | 参数错误 | 表单校验失败（如"用户名不能为空"）、请求体 JSON 解析失败、`上传文件过大` |
| 401 | 认证失败 | `用户名或密码错误`、Token 缺失 / 过期（此时 HTTP 状态码也为 401） |
| 404 | 资源不存在 | `GET /api/materials/{id}` 未命中 |
| 500 | 业务 / 系统错误 | 各类校验失败（见各接口）、未预期异常（`系统异常: ...`） |

---

## 8. 典型示例

### 示例一：5 分钟跑通最小闭环（纯界面操作）

按第 5 章的 8 步操作即可。关键预期节点：

1. 上传 2 张图片 + 1 个 txt → 列表共 3 条；
2. 建"图片播表"含 2 张图 → 详情可预览；
3. 建 1920×1080 布局含 1 个图片模块；
4. 建垫播栏目并绑定；
5. 建日计划 00:00–23:45 → 发布；
6. 打开 `/player?code=TV-01` → 图片每 10 秒轮换；
7. 报表页出现播放次数。

### 示例二：纯 curl 创建演示数据并验证调度（API 操作）

```bash
BASE=http://localhost:8080/api

# 1. 登录拿 token
TOKEN=$(curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
AUTH="Authorization: Bearer $TOKEN"

# 2. 上传一张图片（1x1 红色 PNG）
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' \
  | base64 -d > /tmp/red.png
MID=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/red.png" \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "素材ID=$MID"

# 3. 建播表 → 布局 → 栏目（中文 JSON 建议写入文件再 --data-binary 发送，
#    避免 Windows 终端把中文按 GBK 编码导致服务端解析失败，见 FAQ Q10）
printf '{"name":"\xe5\x9b\xbe\xe7\x89\x87\xe6\x92\xad\xe8\xa1\xa8","type":"image","items":[{"materialId":%s,"sortOrder":0}]}' "$MID" > /tmp/p.json
PID=$(curl -s -X POST $BASE/playlists -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/p.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

printf '{"name":"\xe5\x8d\x95\xe5\x8c\xba\xe5\xb8\x83\xe5\xb1\x80","resolution":"1920x1080","width":1920,"height":1080,"modules":[{"id":"m1","name":"main","type":"image","x":0,"y":0,"w":1920,"h":1080,"zIndex":1}]}' > /tmp/l.json
LID=$(curl -s -X POST $BASE/layouts -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/l.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

printf '{"name":"\xe6\xbc\x94\xe7\xa4\xba\xe6\xa0\x8f\xe7\x9b\xae","layoutId":%s,"playType":"loop","modulePlaylists":[{"moduleId":"m1","playlistId":%s}]}' "$LID" "$PID" > /tmp/g.json
GID=$(curl -s -X POST $BASE/programs -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/g.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 4. 建日计划并发布（今天 00:00–23:59）
TODAY=$(date +%F)
printf '{"name":"\xe5\x85\xa8\xe5\xa4\xa9\xe8\xae\xa1\xe5\x88\x92","planType":"daily","startDate":"%s","endDate":"%s","schedule":{"items":[{"programId":%s,"startTime":"00:00","endTime":"23:59"}]}}' "$TODAY" "$TODAY" "$GID" > /tmp/pl.json
PLID=$(curl -s -X POST $BASE/plans -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/pl.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
curl -s -X POST $BASE/plans/$PLID/publish -H "$AUTH"

# 5. 验证当前应播计划（浏览器打开即可播放；用 at 参数模拟任意时刻）
curl -s "$BASE/player/current?code=TV-01&at=${TODAY}T12:00:00"
# 预期：data.programId = 栏目ID，modules[0].playlist.items[0].url 指向上传的图片
```

### 示例三：紧急插播与优先级验证

在前一示例基础上：

```bash
# 1. 再建一个"紧急"栏目（复用同一布局与播表）
printf '{"name":"\xe7\xb4\xa7\xe6\x80\xa5\xe6\xa0\x8f\xe7\x9b\xae","layoutId":%s,"playType":"emergency","modulePlaylists":[{"moduleId":"m1","playlistId":%s}]}' "$LID" "$PID" > /tmp/g2.json
GID2=$(curl -s -X POST $BASE/programs -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/g2.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

# 2. 建紧急计划（只播 1 分钟窗口）并发布
printf '{"name":"\xe7\xb4\xa7\xe6\x80\xa5\xe8\xae\xa1\xe5\x88\x92","planType":"daily","startDate":"%s","endDate":"%s","schedule":{"items":[{"programId":%s,"startTime":"00:00","endTime":"23:59"}]}}' "$TODAY" "$TODAY" "$GID2" > /tmp/pl2.json
PLID2=$(curl -s -X POST $BASE/plans -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/pl2.json \
  | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
curl -s -X POST $BASE/plans/$PLID2/publish -H "$AUTH"

# 3. 验证：当前应播已切换为紧急栏目（playType=emergency）
curl -s "$BASE/player/current?code=TV-01" | grep -o '"playType":"[a-z]*"'
# 预期输出："playType":"emergency"

# 4. 撤销紧急计划 → 自动回落到垫播
curl -s -X POST $BASE/plans/$PLID2/unpublish -H "$AUTH" > /dev/null
curl -s "$BASE/player/current?code=TV-01" | grep -o '"playType":"[a-z]*"'
# 预期输出："playType":"loop"
# 已打开的播放端页面会在数秒内（WebSocket）或 ≤30 秒（轮询）自动完成同样切换
```

### 示例四：Docker Compose 部署并接入终端

```bash
cd pis1
docker compose up -d --build
docker compose logs -f server        # 等待 "Started OpenPisApplication"
# 管理后台 http://localhost:8081 → admin/123456
# 播放端 http://localhost:8081/player?code=TV-01
# MySQL 宿主机入口 localhost:3307
```

---

## 9. 常见问题 FAQ

**Q1：默认管理员账号是什么？**
`admin / 123456`，后端首次启动时自动创建（日志会打印"默认管理员账号已创建"）。

**Q2：忘记密码怎么办？**
连接数据库执行 `DELETE FROM sys_user WHERE username='admin';` 然后重启后端，会自动重建为 `123456`。

**Q3：支持哪些文件格式？最大多大？**
图片 jpg/jpeg/png/gif/bmp/webp ≤10MB；视频 mp4/avi/mov/mkv/flv ≤200MB；文本 txt ≤1MB。扩展名白名单之外直接拒绝。

**Q4：视频时长为什么必须手填？**
MVP 不引入 FFmpeg 自动解析（列入扩展阶段）。时长影响播放器节奏：不填（0）时视频按浏览器 `ended` 事件自然衔接，图片 / 字幕则使用默认 10 / 30 秒。后续可在媒体「编辑」里随时改。

**Q5：播放端需要登录吗？**
不需要。设备编码（URL 里的 `code`）即终端身份，后端据此记录心跳与日志。

**Q6：一个屏幕同一时段能播多个栏目吗？**
同一时刻只会播**一个**栏目；时间重叠时按播放类型优先级 `emergency > insert > loop` 仲裁。但一个栏目内的**不同模块**可以绑定不同播表并行播放。

**Q7：计划保存后为什么没生效？**
计划必须「发布」才参与调度；列表中状态列显示"草稿"的不会播出。另外要确认生效日期覆盖今天、时间段覆盖当前时刻。

**Q8：数据都存在哪里？**
业务数据在 MySQL 库 `openpis_lite`（9 张表：sys_user、material、playlist、playlist_item、layout、program、plan、device、play_log，由 JPA 自动创建）；上传文件在 `openpis-server/uploads/`。

**Q9：能跨零点排期吗（如 22:00–02:00）？**
当前版本校验要求"结束时间必须晚于开始时间"，不支持跨零点。可拆成两条（22:00–23:59 与 00:00–02:00，后者属于次日计划）。

**Q10：用 curl 传中文 JSON 报 `Invalid UTF-8 start byte`？**
Windows 的 Git Bash 调用系统 curl 时，命令行中的中文会按 GBK 编码传出。解决：把 JSON 写入 UTF-8 文件后用 `--data-binary @文件` 发送（示例二的做法）。

**Q11：operator 角色和 admin 有什么区别？**
当前版本仅登录展示角色差异，接口未做权限控制 [待确认：是否需要按角色限制写操作]。

**Q12：多少终端算多？**
无内置上限；单机演示建议 ≤10 台浏览器终端。大规模场景请参考"不适用场景"。

---

## 10. 故障排除

| 现象 | 可能原因 | 解决方法 |
|------|---------|---------|
| 后端启动报 `Access denied for user 'root'` | application.yml 中数据库密码不正确 | 修改 `spring.datasource.password` 后重启 |
| 后端启动报 `Communications link failure` / HikariPool 启动失败 | MySQL 未启动、端口不对、库名不对 | 确认 MySQL 服务运行、URL 与实际一致；确认已执行建库 SQL |
| 后端启动报 `Port 8080 was already in use` | 8080 被占用 | Windows：`netstat -ano | findstr :8080` 找 PID 后 `taskkill /F /PID <pid>`；或改 `server.port` |
| 编译报大量 `找不到符号: 方法 setXxx()`（Lombok 不生效） | 使用 JDK 23+ 且自行新建了未配置注解处理器的模块 | 项目父 pom 已配置 `annotationProcessorPaths`；新模块需继承同样配置（`<proc>full</proc>` + lombok 注解处理器路径） |
| `npm install` 后运行 `npm run dev` 报 `'vite' 不是内部或外部命令` | 依赖没装成功（常见于 npm 缓存目录不可写） | 用 `npm install --cache "$USERPROFILE/AppData/Local/npm-cache"` 重装，再确认 `node_modules/.bin/vite` 存在 |
| 登录后每过一段时间跳回登录页 | JWT 过期（默认 1 天）或后端重启且更换了 `jwt.secret` | 重新登录；生产环境固定 `jwt.secret` |
| 所有接口都 401 | 请求头没带 Token（手工调 API 时） | 加 `Authorization: Bearer <token>`；注意登录接口本身免鉴权 |
| 上传成功但列表图片不显示 / 播放端素材 404 | `file.access.url` 与实际访问地址不一致（换端口 / 域名 / Docker 部署时常见） | 修改 `file.access.url` 为浏览器实际能访问到的地址（Docker 下为 `http://<主机>:8081/uploads`）后重启 |
| 上传报 `上传文件过大` | 超过 multipart 限制 | 调大 `spring.servlet.multipart.max-file-size` / `max-request-size`（注意仍受单类型 10MB/200MB/1MB 业务限制） |
| 上传报 `不支持的文件类型: xxx` | 扩展名不在白名单 | 改用支持的格式，或按需在 `MaterialService.determineType` 扩展白名单 |
| 播放端视频黑屏或不动 | 视频编码浏览器不支持（如 HEVC），或上传的是无效视频文件 | 使用 H.264 编码 MP4；时长为 0 时播放依赖浏览器 `ended` 事件，请在媒体管理补填时长 |
| 播放端一直显示"当前无播放计划" | 无已发布且生效期覆盖当前时间的计划；或时间段未覆盖当前时刻 | 检查计划状态与时间段；也可用 `GET /api/player/current?at=...` 验证解析结果 |
| 发布计划后播放端长时间不切换 | WebSocket 被网络设备 / 代理拦截（未转发 Upgrade 头） | 修正代理配置（参考 `openpis-web/nginx.conf` 的 `/ws/` 段）；播放端有 30 秒轮询兜底，最多延迟 30 秒 |
| API 返回 `系统异常: JSON parse error ... Invalid UTF-8 start byte 0x..` | 请求体编码不是 UTF-8（Windows 终端中文） | JSON 写入 UTF-8 文件后 `--data-binary @file` 发送（FAQ Q10） |
| `mvn` 下载依赖慢 / 超时 | 直连 Maven Central 慢 | 使用仓库自带 `settings.xml`（阿里云镜像）：`mvn -s settings.xml ...` |
| Docker：mysql 容器反复重启 | 端口 3307 被占用或数据卷损坏 | `docker compose down`，修改 compose 中端口映射或 `docker volume rm` 数据卷后重建 |
| Docker：server 容器起不来报数据库连接失败 | MySQL 健康检查未通过 | `docker compose logs mysql` 排查；compose 已配置 `depends_on: service_healthy`，确认 MySQL 完全就绪 |

**日志位置**：
- 后端：启动方式决定。前台运行直接看控制台；重定向启动（`java -jar ... > server.log`）看 `server.log`。日志级别由 `logging.level.com.openpis` 控制。
- 前端开发服务器：控制台；浏览器端问题用 DevTools（F12）查看 Network / Console。

---

## 11. 卸载 / 数据清理

### 11.1 停止服务

```bash
# 本地方式：结束进程
#   后端：关闭 java 进程（Windows: taskkill /F /IM java.exe，注意会结束所有 java 进程）
#   前端：关闭 npm run dev 所在终端（Ctrl+C）

# Docker 方式：
docker compose down          # 停止并移除容器，数据卷保留
docker compose down -v       # 同时删除 mysql 数据卷（数据库数据清空，不可恢复）
```

### 11.2 清理数据

| 数据 | 位置 | 清理方法 |
|------|------|---------|
| 业务数据 | MySQL 库 `openpis_lite`（9 张表） | `DROP DATABASE openpis_lite;` 或仅清空部分表 `TRUNCATE TABLE play_log;` 等 |
| 上传文件 | `openpis-server/uploads/` | 直接删除该目录 |
| 播放端设备标识 | 各终端浏览器 `localStorage` 键 `openpis_device_code` | 清除浏览器站点数据 |
| 登录凭证 | 各浏览器 `localStorage` 键 `token` / `username` / `role` | 后台点「退出」或清除站点数据 |
| 构建产物 | `openpis-server/*/target/`、`openpis-web/dist/`、`node_modules/` | 删除目录 |

### 11.3 完全卸载

```bash
# 1. 停止服务（见 11.1）
# 2. 删库
mysql -uroot -p -e "DROP DATABASE IF EXISTS openpis_lite;"
# 3. 删除源码与文件
rm -rf pis1/
```

---

## 12. 获取帮助

- **项目文档**：仓库根目录 `README.md`（架构与技术栈）、`docs/` 目录（项目规划文档）、本手册
- **日志与自检**：先看后端控制台 / `server.log`；用 `GET /api/hello` 确认后端存活；`GET /api/player/current?code=xxx` 直接查看调度解析结果
- **回归测试**：`bash scripts/api-test.sh` 可一键验证后端 35 项核心链路（注意会清空业务数据）
- **问题反馈**：提交 GitHub Issue [待确认：GitHub 仓库地址未配置]
- **常见修改入口**：
  - 改端口 / 数据库：`openpis-server/openpis-admin/src/main/resources/application.yml`
  - 支持更多文件类型：`openpis-material/.../service/MaterialService.java` 的 `determineType()`
  - 调整优先级规则：`openpis-player/.../service/PlayerService.java` 的 `priorityOf()`
  - 播放节奏默认值：`openpis-player/.../service/PlayerService.java` 的 `effectiveDuration()` 与 `openpis-web/src/views/player/components/ModulePlayer.vue`
