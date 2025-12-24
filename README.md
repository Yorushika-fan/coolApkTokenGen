# 酷安 X-App-Token API 服务 (unidbg)

使用 unidbg 模拟调用 `libauth.so` 生成酷安 X-App-Token 的 HTTP API 服务。

## 快速开始

### 构建

```bash
cd unidbg-coolapk
mvn clean package -DskipTests
```

### 运行

```bash
# 启动 API 服务 (默认端口 8080)
java -jar target/unidbg-coolapk-1.0-SNAPSHOT.jar server

# 指定端口
java -jar target/unidbg-coolapk-1.0-SNAPSHOT.jar server 9000

# 命令行模式 (单次生成)
java -jar target/unidbg-coolapk-1.0-SNAPSHOT.jar [deviceId]
```

## API 接口

### 生成 Token

```
GET /token?device_id=<device_id>
```

**参数:**
- `device_id` (必需): X-App-Device 值

**响应:**
```json
{
  "token": "v3JDJ5JDEwJE5qazBZekUw...",
  "elapsed_ms": 248
}
```

**示例:**
```bash
curl "http://localhost:8080/token?device_id=sxWduByOxADMuITM5ADNy4SQzEVQgszREZjTQZENwMjMgsTat9WYphFI7kWbvFWaYByOgsDI7AyOwc2d3gXY1pVMvNFSsZTR5pUZE5mM2oWQvpnc3IkSWh0aEVFR"
```

### 健康检查

```
GET /health
```

**响应:**
```json
{"status": "ok"}
```

## 服务器部署

### Docker 部署

创建 `Dockerfile`:

```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY target/unidbg-coolapk-1.0-SNAPSHOT.jar app.jar
COPY src/main/java/com/coolapk/libauth.so libauth.so
EXPOSE 8080
CMD ["java", "-jar", "app.jar", "server", "8080"]
```

构建并运行:

```bash
docker build -t coolapk-token .
docker run -d -p 8080:8080 --name coolapk-token coolapk-token
```

### Systemd 服务

创建 `/etc/systemd/system/coolapk-token.service`:

```ini
[Unit]
Description=Coolapk Token API Service
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/coolapk-token
ExecStart=/usr/bin/java -jar unidbg-coolapk-1.0-SNAPSHOT.jar server 8080
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务:

```bash
sudo systemctl daemon-reload
sudo systemctl enable coolapk-token
sudo systemctl start coolapk-token
```

### Nginx 反向代理

```nginx
server {
    listen 80;
    server_name api.example.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Python 客户端示例

```python
import requests

def get_token(device_id: str, api_url: str = "http://localhost:8080") -> str:
    resp = requests.get(f"{api_url}/token", params={"device_id": device_id})
    resp.raise_for_status()
    return resp.json()["token"]

# 使用
token = get_token("sxWduByOxADMuITM5ADNy4SQzEVQgs...")
print(token)
```

## 注意事项

1. **libauth.so**: 确保 `libauth.so` 文件在工作目录或 jar 包同级目录
2. **内存**: 首次启动需要加载 unidbg 模拟器，建议分配至少 512MB 内存
3. **并发**: 默认使用 4 线程处理请求，可根据需要调整
