# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 先拷贝 pom 与镜像配置，利用 Docker 层缓存加速依赖下载
COPY settings.xml .
COPY pom.xml .
COPY openpis-common/pom.xml openpis-common/
COPY openpis-system/pom.xml openpis-system/
COPY openpis-material/pom.xml openpis-material/
COPY openpis-playlist/pom.xml openpis-playlist/
COPY openpis-layout/pom.xml openpis-layout/
COPY openpis-program/pom.xml openpis-program/
COPY openpis-plan/pom.xml openpis-plan/
COPY openpis-player/pom.xml openpis-player/
COPY openpis-admin/pom.xml openpis-admin/
RUN mvn -s settings.xml -B -q dependency:go-offline || true

COPY . .
RUN mvn -s settings.xml -B -DskipTests package

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/openpis-admin/target/openpis-admin-1.0.0.jar app.jar
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
