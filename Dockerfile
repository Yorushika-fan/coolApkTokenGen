FROM openjdk:21-slim

WORKDIR /app

# 复制 jar 和 so 文件
COPY target/unidbg-coolapk-1.0-SNAPSHOT.jar app.jar
COPY src/main/java/com/coolapk/libauth.so libauth.so

# 暴露端口
EXPOSE 8080

# 启动服务
CMD ["java", "-Xmx512m", "-jar", "app.jar", "server", "8080"]
