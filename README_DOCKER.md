# 国网招标爬虫 - Docker 容器化部署指南

基于 Spring Boot 3 + Java 21 构建的国网招标信息爬虫系统，现已全面支持 Docker 容器化部署。

## 🐳 容器化部署特性

### 核心优势
- **生产级镜像**：基于 Ubuntu 20.04 LTS，稳定可靠
- **完整环境**：内置 OpenJDK 21、Chrome 浏览器、ChromeDriver
- **中文字体支持**：预装文泉驿字体，解决爬虫渲染乱码问题
- **一键部署**：提供完整的构建和部署脚本
- **数据持久化**：自动挂载数据卷，确保数据安全
- **健康检查**：内置健康监测机制
- **开机自启**：容器异常时自动重启

## 🚀 快速开始

### 1. 环境准备

#### 安装 Docker
```bash
# Ubuntu/Debian
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER

# CentOS/RHEL
sudo yum install -y yum-utils
curl -fsSL https://get.docker.com | sh

# Windows/macOS: 下载 Docker Desktop 官方安装包
```

#### 安装 Docker Compose
```bash
# Linux
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 验证安装
docker --version
docker-compose --version
```

### 2. 配置环境变量

```bash
# 复制环境配置模板
cp .env.example .env

# 编辑配置文件
nano .env
```

主要配置项：
```bash
# 应用端口
APP_PORT=8080

# 数据库连接
DB_URL=jdbc:mysql://host.docker.internal:3306/sgcc_crawler
DB_USERNAME=inspection
DB_PASSWORD=ENC(your_encrypted_password)

# 钉钉通知
DINGTALK_WEBHOOK=ENC(your_encrypted_webhook)
DINGTALK_SECRET=ENC(your_encrypted_secret)
DINGTALK_ENABLED=true

# 爬虫配置
CRAWLER_HEADLESS=true
CRAWLER_MAX_PAGES=5
CRAWLER_REQUEST_INTERVAL=2000

# 数据存储路径
DATA_VOLUME=/data/sgcc_bidding_crawler
```

### 3. 一键部署

#### Linux/macOS 系统
```bash
# 给脚本添加执行权限
chmod +x scripts/*.sh

# 一键构建并启动
./scripts/deploy-container.sh up

# 查看服务状态
./scripts/deploy-container.sh status

# 查看实时日志
./scripts/deploy-container.sh logs
```

#### Windows 系统
```cmd
# 一键构建并启动
scripts\deploy-container.bat up

# 查看服务状态
scripts\deploy-container.bat status

# 查看实时日志
scripts\deploy-container.bat logs
```

## 📁 项目结构

```
国网招标信息-Java/
├── docker/
│   ├── Dockerfile              # 生产级 Docker 镜像定义
│   └── docker-compose.yml      # 服务编排配置
├── scripts/
│   ├── build-container.sh      # Linux 构建脚本
│   ├── build-container.bat     # Windows 构建脚本
│   ├── deploy-container.sh     # Linux 部署脚本
│   └── deploy-container.bat    # Windows 部署脚本
├── .env.example               # 环境变量模板
└── README.md                  # 项目说明文档
```

## 🏗️ Docker 镜像详解

### 基础环境
- **基础镜像**：Ubuntu 20.04 LTS
- **Java 环境**：OpenJDK 21
- **时区设置**：Asia/Shanghai

### 爬虫依赖
- **浏览器**：Google Chrome Stable
- **驱动程序**：ChromeDriver（自动匹配版本）
- **中文字体**：fonts-wqy-zenhei、fonts-wqy-microhei

### 安全特性
- 非交互式安装模式
- 最小化依赖包
- 定期安全更新

## ⚙️ 服务配置详解

### docker-compose.yml 配置说明

```yaml
version: '3.8'

services:
  sgcc-crawler:
    build:
      context: ..
      dockerfile: docker/Dockerfile
    container_name: sgcc-crawler
    image: sgcc/crawler:latest
    ports:
      - "${APP_PORT:-8080}:8080"
    volumes:
      - ${DATA_VOLUME:-/data/sgcc_bidding_crawler}:/app/data
      - ./logs:/app/logs
    environment:
      - JAVA_OPTS=-Xms512m -Xmx2g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}
      # 数据库和钉钉配置...
    restart: always
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - sgcc-network

networks:
  sgcc-network:
    driver: bridge
```

### 关键配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `restart: always` | 容器开机自启 | 必须开启 |
| `ports` | 端口映射 | 8080:8080 |
| `volumes` | 数据持久化 | /data/sgcc_bidding_crawler |
| `healthcheck` | 健康检查 | 30秒间隔 |

## 📊 服务管理命令

### 基础操作
```bash
# 启动服务
./scripts/deploy-container.sh up

# 停止服务
./scripts/deploy-container.sh down

# 重启服务
./scripts/deploy-container.sh restart

# 构建镜像
./scripts/deploy-container.sh build

# 查看状态
./scripts/deploy-container.sh status
```

### Docker 原生命令
```bash
# 查看运行中的容器
docker ps

# 查看容器日志
docker logs -f sgcc-crawler

# 进入容器调试
docker exec -it sgcc-crawler bash

# 查看容器资源使用
docker stats sgcc-crawler

# 查看网络连接
docker network ls
```

## 🗃️ 数据持久化

### 自动挂载的数据卷
1. **应用数据**：`/data/sgcc_bidding_crawler` → `/app/data`
2. **日志文件**：`./logs` → `/app/logs`

### 数据备份与恢复
```bash
# 备份数据
tar czf backup-$(date +%Y%m%d).tar.gz /data/sgcc_bidding_crawler

# 恢复数据
tar xzf backup-*.tar.gz -C /data/sgcc_bidding_crawler
```

## 🏥 健康检查与监控

### 内置健康检查端点
```bash
# 应用健康状态
curl http://localhost:8080/api/actuator/health

# 详细健康信息
curl http://localhost:8080/api/actuator/health/detail

# 爬虫状态
curl http://localhost:8080/api/crawler/status
```

### 监控脚本示例
```bash
#!/bin/bash
# health-monitor.sh

SERVICE_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=3
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f $SERVICE_URL >/dev/null 2>&1; then
        echo "[OK] Service is healthy"
        exit 0
    else
        echo "[WARN] Service check failed, retry $((RETRY_COUNT+1))/$MAX_RETRIES"
        RETRY_COUNT=$((RETRY_COUNT+1))
        sleep 10
    fi
done

echo "[ERROR] Service is unhealthy, restarting..."
./scripts/deploy-container.sh restart
```

## 🔧 高级配置

### 自定义 JVM 参数
在 `.env` 文件中添加：
```bash
JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError
```

### 资源限制配置
```yaml
# docker/docker-compose.yml
services:
  sgcc-crawler:
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '1.0'
        reservations:
          memory: 512M
          cpus: '0.5'
```

### 网络隔离配置
```yaml
networks:
  sgcc-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
```

## ❌ 故障排除

### 常见问题及解决方案

#### 1. 构建失败
```bash
# 清理构建缓存
./scripts/deploy-container.sh down
docker builder prune -a

# 重新构建
./scripts/deploy-container.sh build --no-cache
```

#### 2. 容器启动失败
```bash
# 查看详细错误
docker logs sgcc-crawler

# 检查配置
docker exec sgcc-crawler cat /app/application.yml

# 验证环境变量
docker exec sgcc-crawler env
```

#### 3. 数据库连接问题
```bash
# 测试数据库连通性
docker exec sgcc-crawler nc -zv host.docker.internal 3306

# 检查数据库服务
telnet your-database-host 3306
```

#### 4. Chrome/爬虫问题
```bash
# 验证 Chrome 安装
docker exec sgcc-crawler google-chrome --version

# 验证 ChromeDriver
docker exec sgcc-crawler chromedriver --version

# 测试无头模式
docker exec sgcc-crawler google-chrome --no-sandbox --headless --dump-dom https://www.example.com
```

### 调试技巧
```bash
# 实时监控容器
docker stats sgcc-crawler

# 查看文件系统变化
docker diff sgcc-crawler

# 进入容器调试
docker exec -it sgcc-crawler bash

# 检查网络配置
docker network inspect sgcc-crawler-network
```

## 🔒 安全最佳实践

### 镜像安全
```bash
# 定期更新基础镜像
./scripts/build-container.sh latest

# 扫描镜像漏洞
docker scan sgcc/crawler:latest

# 使用最小化基础镜像
# FROM ubuntu:20.04-slim （可选）
```

### 运行时安全
```yaml
# docker/docker-compose.yml
services:
  sgcc-crawler:
    security_opt:
      - no-new-privileges:true
    read_only: true
    tmpfs:
      - /tmp
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
```

## 📈 性能优化建议

### JVM 调优
```bash
# 在 .env 中配置
JAVA_OPTS=-Xms1g -Xmx2g \
          -XX:+UseG1GC \
          -XX:MaxGCPauseMillis=200 \
          -XX:+HeapDumpOnOutOfMemoryError \
          -XX:HeapDumpPath=/app/logs/
```

### 数据库连接池优化
```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      connection-timeout: 30000
      idle-timeout: 600000
```

### 容器资源配置
```yaml
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 3G
      cpus: '2.0'
    reservations:
      memory: 1G
      cpus: '1.0'
```

## 🔄 备份与恢复策略

### 自动备份脚本
```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="/backup/sgcc-crawler"
DATE=$(date +%Y%m%d_%H%M%S)

# 备份数据卷
docker run --rm \
  -v sgcc-data:/data \
  -v $BACKUP_DIR:/backup \
  alpine tar czf /backup/data-$DATE.tar.gz -C /data .

# 备份数据库
mysqldump -h host.docker.internal -u inspection -p sgcc_crawler | gzip > $BACKUP_DIR/db-$DATE.sql.gz

# 清理旧备份（保留7天）
find $BACKUP_DIR -name "*.tar.gz" -mtime +7 -delete
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete
```

### 恢复操作
```bash
# 恢复数据卷
docker run --rm \
  -v sgcc-data:/data \
  -v /backup:/backup \
  alpine tar xzf /backup/data-latest.tar.gz -C /data

# 恢复数据库
gunzip < /backup/db-latest.sql.gz | mysql -h host.docker.internal -u inspection -p sgcc_crawler
```

## 📊 监控告警集成

### Prometheus 监控配置
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'sgcc-crawler'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/api/actuator/prometheus'
```

### Grafana 仪表板配置
```json
{
  "dashboard": {
    "title": "SGCC Crawler Monitoring",
    "panels": [
      {
        "title": "Health Status",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job='sgcc-crawler'}"
          }
        ]
      }
    ]
  }
}
```

## 🎯 生产环境部署建议

### 1. 高可用部署
```yaml
# docker-compose.prod.yml
version: '3.8'
services:
  sgcc-crawler-1:
    # 主实例配置
    
  sgcc-crawler-2:
    # 备用实例配置
    ports:
      - "8081:8080"
```

### 2. 负载均衡配置
```nginx
# nginx.conf
upstream sgcc_crawler {
    server localhost:8080;
    server localhost:8081;
}

server {
    listen 80;
    location / {
        proxy_pass http://sgcc_crawler;
    }
}
```

### 3. 日志集中管理
```yaml
# docker-compose.yml
services:
  sgcc-crawler:
    logging:
      driver: "fluentd"
      options:
        fluentd-address: localhost:24224
        tag: sgcc.crawler
```

## 📞 技术支持

如遇问题，请提供以下信息：
1. 错误日志内容
2. Docker 版本信息
3. 操作系统环境
4. 相关配置文件内容（敏感信息请脱敏）

---
**版本**: 1.0.0  
**最后更新**: 2024年  
**许可证**: MIT