# IDEA Docker：对本文件右键 Run；Build context = 仓库根目录
# 运行前先 mvn package；并挂载仓库根目录 config → /app/config
# 业务配置以 config/admin.properties 为准（勿在镜像 ENV / Compose 里覆盖）

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY omes-iomp-runner-admin/target/omes-iomp-runner-admin-*.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m" \
    TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 10010

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
  CMD curl -fsS http://127.0.0.1:10010/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=GMT+08 -jar /app/app.jar"]
