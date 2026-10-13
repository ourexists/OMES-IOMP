# IDEA Docker：对本文件右键 Run；Build context = 仓库根目录
# 运行前先 mvn package；并挂载仓库根目录 config → /app/config
# 业务配置以 config/sas.properties 为准（勿在镜像 ENV / Compose 里覆盖）

FROM eclipse-temurin:21-jre-jammy

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY omes-iomp-runner-sas/target/omes-iomp-runner-sas-*.jar /app/app.jar

ENV JAVA_OPTS="-Xms256m -Xmx512m" \
    TZ=Asia/Shanghai \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 9400

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
  CMD curl -fsS http://127.0.0.1:9400/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Duser.timezone=GMT+08 -jar /app/app.jar"]
