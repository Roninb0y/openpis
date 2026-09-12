<!-- 标题建议：type(scope): 简要描述，如 feat(player): 支持字幕滚动速度配置 -->

## 变更类型

- [ ] feat 新功能
- [ ] fix 缺陷修复
- [ ] docs 文档
- [ ] refactor 重构（不改行为）
- [ ] test 测试
- [ ] chore 构建 / 工程化

## 变更说明

<!-- 做了什么？为什么这么做？ -->

## 关联 Issue

<!-- 例如：Closes #123 -->

## 自测清单

- [ ] 后端：`cd openpis-server && mvn -s settings.xml -DskipTests package` 编译通过
- [ ] 前端：`cd openpis-web && npm run build` 构建通过
- [ ] 涉及接口行为变更时，已在 Apifox / curl 层面自测，关键链路可用
- [ ] 已确认未提交任何真实密钥、密码、token（含新增配置与文档示例）
- [ ] 涉及数据库结构的变更已在 PR 说明中附迁移 SQL（`ddl-auto: update` 只增不减）

## 补充说明

<!-- 截图、性能数据、兼容性影响等 -->
