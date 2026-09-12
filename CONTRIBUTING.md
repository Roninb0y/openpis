# 贡献指南（CONTRIBUTING）

感谢关注 OpenPIS-Lite！欢迎通过 Issue 反馈问题、提交 Pull Request 贡献代码。请先阅读本指南。

## 开发环境准备

| 依赖 | 版本要求 |
|------|---------|
| JDK | 17+（在 JDK 25 上验证可运行） |
| Node.js | 18+（建议 20） |
| MySQL | 8.0 |
| Maven | 3.9+（或使用 IDEA 内置 Maven） |

本地启动步骤见 [USAGE.md](USAGE.md) 第 3 章「安装与升级」，概要：

```bash
# 建库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS openpis_lite DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
# 后端（在 openpis-server 目录）
mvn -s settings.xml -DskipTests package
java -jar openpis-admin/target/openpis-admin-1.0.0.jar
# 前端（在 openpis-web 目录）
npm install
npm run dev
```

> 提示：国内网络建议保留 `mvn -s settings.xml`（阿里云镜像）；
> 数据库密码不要提交——放在 `openpis-server/application-local.yml`（已被 gitignore，格式见 USAGE.md 4.6），
> 连接地址 / 用户名在 `openpis-server/openpis-admin/src/main/resources/application.yml`。

## 项目结构

```
openpis-server/   Spring Boot 3.5 Maven 多模块后端（common/system/material/playlist/
                  layout/program/plan/player/admin 九个子模块）
openpis-web/      Vue 3 + TypeScript + Vite 前端（管理后台 + 全屏播放端）
scripts/          API 冒烟测试脚本
docs/             项目规划文档
```

## 开发流程

1. 从 `main` 拉出分支，命名建议：`feature/xxx`（新功能）、`fix/xxx`（缺陷）、`docs/xxx`（文档）。
2. 开发并自测通过。
3. 提交 Pull Request 到 `main`。

### 提交信息约定

采用 Conventional Commits 风格：

```
feat: 新增素材批量上传
fix: 修复播放端跨零点时间段显示错误
docs: 补充播放端 FAQ
refactor: 提取播放优先级比较逻辑
test: 补充计划校验单测
chore: 升级 Vite 至 5.4.x
```

## 自测要求

提交 PR 前请确保以下命令全部通过（CI 也会执行相同检查）：

```bash
# 后端编译（当前无单元测试，编译即 CI 门禁）
cd openpis-server && mvn -s settings.xml -DskipTests package

# 前端构建
cd openpis-web && npm install && npm run build

# （可选）后端 API 冒烟测试 —— 警告：会先清空库内业务数据！
# 需后端已在本机 8080 运行
bash scripts/api-test.sh
```

欢迎为新功能补充单元测试（JUnit 5 / Mockito），后端目前尚无单测，属于优先欢迎的贡献方向。

## 代码约定

- 后端：Java 17 语法、Lombok、Spring Data JPA；统一返回体 `Result<T>` 与业务异常 `BizException`，不要在 Controller 手写 try/catch 返回错误
- 前端：`<script setup lang="ts">` 组合式 API；接口统一走 `src/api/` 封装的 axios 实例；文件 URL 统一用 `src/utils/file.ts` 的 `getFileUrl()`，不要硬编码主机地址
- 数据库：复杂结构（布局模块、模块绑定、计划时间段）沿用 JSON 列存储方案；新增表 / 字段由 JPA `ddl-auto: update` 自动创建，破坏性变更请在 PR 说明中附迁移 SQL
- 注释与文档使用中文

## 报告缺陷

请使用 [Bug 报告模板](.github/ISSUE_TEMPLATE/bug_report.md)，尽量附上后端日志、复现步骤与环境信息。
安全类问题**不要**公开发 Issue，请按 [SECURITY.md](SECURITY.md) 的流程私下报告。

## 行为准则

保持友善与尊重，讨论聚焦技术与项目本身。
