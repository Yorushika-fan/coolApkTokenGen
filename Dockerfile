# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -q

# 运行阶段 - 使用 Debian 而非 Alpine，因为 unidbg 需要 glibc
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 安装必要的库
RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 \
    && rm -rf /var/lib/apt/lists/*

# 从构建阶段复制 jar
COPY --from=builder /build/target/unidbg-coolapk-1.0-SNAPSHOT.jar app.jar
COPY src/main/java/com/coolapk/libauth.so libauth.so

EXPOSE 8080

CMD ["java", "-Xmx512m", "-jar", "app.jar", "server", "8080"]
