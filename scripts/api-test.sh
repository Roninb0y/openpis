#!/bin/bash
# OpenPIS-Lite 后端 API 全链路冒烟测试
# 说明：JSON 报文先写入文件（UTF-8）再以 --data-binary 发送，
#       避免 Git Bash 调用 Windows curl 时中文被 GBK 转码导致解析失败。
BASE=http://localhost:8080/api
PASS=0; FAIL=0
ok()   { PASS=$((PASS+1)); echo "PASS: $1"; }
fail() { FAIL=$((FAIL+1)); echo "FAIL: $1  >>> $2"; }

jsonval() { grep -o "\"$1\":[0-9]*" | head -1 | cut -d: -f2; }

req() { # method path json  ->  response body
  echo -n "$3" > /tmp/_req.json
  curl -s -X "$1" "$BASE$2" -H "$AUTH" -H 'Content-Type: application/json' --data-binary @/tmp/_req.json
}

echo "===== 0. 清理历史数据 ====="
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
AUTH="Authorization: Bearer $TOKEN"

cleanup() { # $1=resource
  IDS=$(curl -s "$BASE/$1?page=1&size=100" -H "$AUTH" | grep -o '"id":[0-9]*' | cut -d: -f2)
  for id in $IDS; do curl -s -X DELETE "$BASE/$1/$id" -H "$AUTH" > /dev/null; done
}
cleanup plans
cleanup programs
cleanup layouts
cleanup playlists
cleanup materials
ok "历史数据清理完成"

echo "===== 1. 登录认证 ====="
[ -n "$TOKEN" ] && ok "正确密码登录获取token" || fail "登录" "token为空"

R=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"wrong"}')
echo "$R" | grep -q '"code":401' && ok "错误密码返回业务401" || fail "错误密码处理" "$R"

CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE/materials)
[ "$CODE" = "401" ] && ok "无token访问受保护接口→HTTP401" || fail "无token访问" "HTTP=$CODE"

echo "===== 2. 媒体管理 ====="
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' | base64 -d > /tmp/red.png
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==' | base64 -d > /tmp/blue.png
printf '\xe6\xac\xa2\xe8\xbf\x8e\xe4\xb9\x98\xe5\x9d\x90\xe6\x9c\xac\xe6\xac\xa1\xe5\x88\x97\xe8\xbd\xa6\xef\xbc\x8cOpenPIS-Lite \xe5\xad\x97\xe5\xb9\x95\xe6\xb5\x8b\xe8\xaf\x95\xe3\x80\x82' > /tmp/sub.txt

M1=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/red.png" | jsonval id)
M2=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/blue.png" | jsonval id)
M3=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/sub.txt" | jsonval id)
head -c 100000 /dev/urandom > /tmp/fake.mp4
M4=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/fake.mp4" -F "duration=60" | jsonval id)
[ -n "$M1" ] && [ -n "$M2" ] && [ -n "$M3" ] && [ -n "$M4" ] && ok "上传图片x2/文本/视频(手填时长60s)" || fail "上传" "M1=$M1 M2=$M2 M3=$M3 M4=$M4"

CNT=$(curl -s "$BASE/materials?page=1&size=10" -H "$AUTH" | grep -o '"totalElements":[0-9]*' | cut -d: -f2)
[ "$CNT" = "4" ] && ok "媒体列表分页 totalElements=4" || fail "媒体列表" "total=$CNT"

LCNT=$(curl -s "$BASE/materials?page=1&size=10&type=video" -H "$AUTH" | grep -o '"totalElements":[0-9]*' | cut -d: -f2)
[ "$LCNT" = "1" ] && ok "媒体按类型筛选 video=1" || fail "类型筛选" "total=$LCNT"

cp /tmp/red.png /tmp/bad.exe
R=$(curl -s -X POST $BASE/materials/upload -H "$AUTH" -F "file=@/tmp/bad.exe")
echo "$R" | grep -q '不支持的文件类型' && ok "拒绝不支持的文件类型" || fail "文件类型校验" "$R"

R=$(curl -s -X PUT "$BASE/materials/$M4" -H "$AUTH" -H 'Content-Type: application/json' --data-binary '{"duration":120,"title":"fake.mp4"}')
echo "$R" | grep -q '"duration":120' && ok "编辑视频时长(手填120s)" || fail "编辑媒体" "$R"

echo "===== 3. 播表管理 ====="
R=$(req POST /playlists "{\"name\":\"主视频播表\",\"type\":\"video\",\"items\":[{\"materialId\":$M4,\"sortOrder\":0,\"duration\":30},{\"materialId\":$M1,\"sortOrder\":1},{\"materialId\":$M2,\"sortOrder\":2}]}")
P1=$(echo "$R" | jsonval id)
R=$(req POST /playlists "{\"name\":\"字幕播表\",\"type\":\"text\",\"items\":[{\"materialId\":$M3,\"sortOrder\":0}]}")
P2=$(echo "$R" | jsonval id)
R=$(req POST /playlists "{\"name\":\"底部图片播表\",\"type\":\"image\",\"items\":[{\"materialId\":$M2,\"sortOrder\":0}]}")
P3=$(echo "$R" | jsonval id)
[ -n "$P1" ] && [ -n "$P2" ] && [ -n "$P3" ] && ok "创建视频/字幕/图片播表" || fail "创建播表" "P1=$P1 P2=$P2 P3=$P3"

R=$(curl -s "$BASE/playlists/$P1" -H "$AUTH")
echo "$R" | grep -q '"materialTitle"' && ok "播表详情返回媒体信息" || fail "播表详情" "$R"
echo "$R" | grep -q '"materialThumbnail"' && ok "播表详情返回缩略图字段" || fail "缩略图字段" "$R"

R=$(req PUT "/playlists/$P1" "{\"name\":\"主视频播表\",\"type\":\"video\",\"items\":[{\"materialId\":$M4,\"sortOrder\":0,\"duration\":30},{\"materialId\":$M1,\"sortOrder\":1}]}")
CNT2=$(echo "$R" | grep -o '"materialId":[0-9]*' | wc -l)
[ "$CNT2" = "2" ] && ok "更新播表(全量替换)后播表项=2" || fail "更新播表" "items=$CNT2 $R"

echo "===== 4. 布局管理 ====="
R=$(req POST /layouts '{"name":"标准三区布局","resolution":"1920x1080","width":1920,"height":1080,"modules":[{"id":"m1","name":"主视频区","type":"video","x":0,"y":0,"w":1280,"h":720,"zIndex":1},{"id":"m2","name":"右字幕区","type":"text","x":1280,"y":0,"w":640,"h":720,"zIndex":2},{"id":"m3","name":"底部图片区","type":"image","x":0,"y":720,"w":1920,"h":360,"zIndex":3}]}')
L1=$(echo "$R" | jsonval id)
[ -n "$L1" ] && ok "创建三模块布局" || fail "创建布局" "$R"

R=$(curl -s "$BASE/layouts/$L1" -H "$AUTH")
echo "$R" | grep -q '"zIndex":3' && ok "布局详情含模块JSON" || fail "布局详情" "$R"

echo "===== 5. 栏目管理 ====="
R=$(req POST /programs "{\"name\":\"默认垫播栏目\",\"layoutId\":$L1,\"playType\":\"loop\",\"modulePlaylists\":[{\"moduleId\":\"m1\",\"playlistId\":$P1},{\"moduleId\":\"m2\",\"playlistId\":$P2},{\"moduleId\":\"m3\",\"playlistId\":$P3}]}")
G1=$(echo "$R" | jsonval id)
[ -n "$G1" ] && ok "创建栏目(全模块绑定)" || fail "创建栏目" "$R"

R=$(req POST /programs "{\"name\":\"非法绑定\",\"layoutId\":$L1,\"playType\":\"loop\",\"modulePlaylists\":[{\"moduleId\":\"m2\",\"playlistId\":$P1}]}")
echo "$R" | grep -q '只能绑定' && ok "模块/播表类型不匹配被拒绝" || fail "类型匹配校验" "$R"

R=$(req POST /programs "{\"name\":\"紧急插播栏目\",\"layoutId\":$L1,\"playType\":\"emergency\",\"modulePlaylists\":[{\"moduleId\":\"m1\",\"playlistId\":$P1}]}")
G2=$(echo "$R" | jsonval id)
[ -n "$G2" ] && ok "创建紧急栏目(仅绑m1)" || fail "创建紧急栏目" "$R"

R=$(curl -s "$BASE/programs/$G1" -H "$AUTH")
echo "$R" | grep -q '"playlistName"' && ok "栏目详情返回播表绑定信息" || fail "栏目详情" "$R"

echo "===== 6. 计划管理 ====="
TODAY=$(date +%F)
R=$(req POST /plans "{\"name\":\"全天垫播计划\",\"planType\":\"daily\",\"startDate\":\"$TODAY\",\"endDate\":\"$TODAY\",\"schedule\":{\"items\":[{\"programId\":$G1,\"startTime\":\"00:00\",\"endTime\":\"23:59\"}]}}")
PL1=$(echo "$R" | jsonval id)
[ -n "$PL1" ] && ok "创建日计划" || fail "创建日计划" "$R"

R=$(req POST /plans "{\"name\":\"时间非法\",\"planType\":\"daily\",\"startDate\":\"$TODAY\",\"endDate\":\"$TODAY\",\"schedule\":{\"items\":[{\"programId\":$G1,\"startTime\":\"12:00\",\"endTime\":\"08:00\"}]}}")
echo "$R" | grep -q '结束时间必须晚于开始时间' && ok "时间校验生效" || fail "时间校验" "$R"

R=$(curl -s -X POST "$BASE/plans/$PL1/publish" -H "$AUTH")
echo "$R" | grep -q '"code":200' && ok "发布计划" || fail "发布计划" "$R"

echo "===== 7. 播放解析 ====="
R=$(curl -s "$BASE/player/current?code=DEV-TEST-01")
echo "$R" | grep -q "\"programId\":$G1" && ok "当前应播计划=垫播栏目" || fail "播放解析" "$R"
echo "$R" | grep -q '"url":"http' && ok "素材URL已拼接完整" || fail "素材URL" "$R"
echo "$R" | grep -q '"layoutWidth":1920' && ok "返回布局分辨率" || fail "布局分辨率" "$R"

R=$(req POST /plans "{\"name\":\"紧急计划\",\"planType\":\"daily\",\"startDate\":\"$TODAY\",\"endDate\":\"$TODAY\",\"schedule\":{\"items\":[{\"programId\":$G2,\"startTime\":\"00:00\",\"endTime\":\"23:59\"}]}}")
PL2=$(echo "$R" | jsonval id)
curl -s -X POST "$BASE/plans/$PL2/publish" -H "$AUTH" > /dev/null
R=$(curl -s "$BASE/player/current?code=DEV-TEST-01")
echo "$R" | grep -q "\"programId\":$G2" && ok "紧急计划优先于垫播计划" || fail "优先级解析" "$R"
echo "$R" | grep -q '"playlist":null' && ok "紧急栏目未绑定模块返回null" || fail "未绑定模块处理" "$R"
curl -s -X POST "$BASE/plans/$PL2/unpublish" -H "$AUTH" > /dev/null
R=$(curl -s "$BASE/player/current?code=DEV-TEST-01")
echo "$R" | grep -q "\"programId\":$G1" && ok "撤销紧急计划后回落到垫播" || fail "撤销回落" "$R"

# 周计划：生效期设为未来一个月，用 at 模拟上周一 → 不应命中
NEXTMONTH=$(date -d "+30 days" +%F 2>/dev/null || date -v+30d +%F)
LASTMONDAY=$(date -d "last monday" +%F 2>/dev/null || date -v-7d +%F)
R=$(req POST /plans "{\"name\":\"周一晨间计划\",\"planType\":\"weekly\",\"startDate\":\"$TODAY\",\"endDate\":\"$NEXTMONTH\",\"schedule\":{\"items\":[{\"programId\":$G2,\"dayOfWeek\":1,\"startTime\":\"08:00\",\"endTime\":\"10:00\"}]}}")
PL3=$(echo "$R" | jsonval id)
curl -s -X POST "$BASE/plans/$PL3/publish" -H "$AUTH" > /dev/null
R=$(curl -s "$BASE/player/current?at=${LASTMONDAY}T09:00:00")
echo "$R" | grep -q '"data":null' && ok "周计划在生效期外不命中(模拟上周一)" || fail "周计划日期窗口" "$R"
# 用 at 模拟未来某周一上午 → 应命中紧急栏目
FUTMONDAY=$(date -d "next monday" +%F 2>/dev/null || date -v+1d +%F)
R=$(curl -s "$BASE/player/current?at=${FUTMONDAY}T09:00:00")
echo "$R" | grep -q "\"programId\":$G2" && ok "周计划在指定星期上午命中" || fail "周计划命中" "$R"

echo "===== 8. 播放日志与报表 ====="
R=$(curl -s -X POST $BASE/device/heartbeat -H 'Content-Type: application/json' --data-binary '{"code":"DEV-TEST-01","name":"DEV-TEST-01"}')
echo "$R" | grep -q '"code":200' && ok "设备心跳" || fail "设备心跳" "$R"

NOW=$(date '+%Y-%m-%d %H:%M:%S')
R=$(curl -s -X POST $BASE/player/log -H 'Content-Type: application/json' \
  --data-binary "[{\"deviceCode\":\"DEV-TEST-01\",\"programId\":$G1,\"materialId\":$M4,\"startedAt\":\"$NOW\",\"endedAt\":\"$NOW\"},{\"deviceCode\":\"DEV-TEST-01\",\"programId\":$G1,\"materialId\":$M1,\"startedAt\":\"$NOW\",\"endedAt\":\"$NOW\"}]")
echo "$R" | grep -q '"code":200' && ok "上报播放日志x2" || fail "播放日志" "$R"

R=$(curl -s "$BASE/report/daily?days=7" -H "$AUTH")
TODAY_CNT=$(echo "$R" | grep -o "\"date\":\"$TODAY\",\"count\":[0-9]*" | cut -d: -f3)
[ "${TODAY_CNT:-0}" -ge 2 ] && ok "日报表当日次数=$TODAY_CNT" || fail "日报表" "$R"

R=$(curl -s "$BASE/report/program" -H "$AUTH")
echo "$R" | grep -q '"count":2' && ok "栏目播放统计" || fail "栏目统计" "$R"

echo "===== 9. WebSocket 端点 ====="
CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/ws/player?code=DEV-TEST-01")
[ "$CODE" = "400" ] && ok "WS端点存在(400为curl无升级头所致)" || fail "WS端点" "HTTP=$CODE"

echo ""
echo "=========================================="
echo "测试结果: PASS=$PASS FAIL=$FAIL"
echo "=========================================="
