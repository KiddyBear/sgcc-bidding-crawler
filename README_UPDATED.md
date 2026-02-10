# 国网招标爬虫 - Docker 容器化部署指南

基于 Spring Boot 3 + Java 21 构建的国网招标信息爬虫系统，现已全面支持 Docker 容器化部署。

## 🐳 容器化部署概览

### 核心特性
- **生产级基础镜像**：Ubuntu 20.04 LTS + OpenJDK 21
- **完整爬虫环境**：预装 Chrome 浏览器、ChromeDriver、中文字体
- **一键部署体验**：自动化构建和部署脚本
- **企业级可靠性**：数据持久化、健康检查、自动重启
- **跨平台支持**：Linux/macOS/Windows 全平台兼容

### 🚀 三步快速部署

1. **准备环境**
```bash
# 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

2. **配置部署**
```bash
# 复制配置模板
cp .env.example .env

# 编辑关键配置
nano .env  # 设置数据库连接和钉钉通知
```

3. **启动服务**
```bash
# Linux/macOS
chmod +x scripts/*.sh
./scripts/deploy-container.sh up

# Windows
scripts\deploy-container.bat up
```

### 📁 部署文件结构
```
国网招标信息-Java/
├── docker/
│   ├── Dockerfile              # Ubuntu 20.04 + Java 21 生产镜像
│   └── docker-compose.yml      # 服务编排配置
├── scripts/
│   ├── build-container.sh      # Linux 构建脚本
│   ├── build-container.bat     # Windows 构建脚本
│   ├── deploy-container.sh     # Linux 部署脚本
│   └── deploy-container.bat    # Windows 部署脚本
├── .env.example               # 环境变量模板
└── README.md                  # 部署文档
```

## 🛠️ 环境要求

### 容器化部署（推荐方案）
- **Docker Engine**: 20.10 或更高版本
- **Docker Compose**: 1.29 或更高版本
- **系统资源**: 2GB RAM + 2 CPU cores
- **存储空间**: 至少 5GB 可用空间

### 传统部署（备选方案）
- **Java**: OpenJDK 21 或 Oracle JDK 21
- **Maven**: 3.8+ 构建工具
- **MySQL**: 8.0+ 数据库
- **Chrome**: 最新版浏览器（Selenium 驱动）
- **操作系统**: Ubuntu 20.04/CentOS 7+/Windows 10+

## 🏗️ Docker 镜像详解

### 基础环境配置
```dockerfile
FROM ubuntu:20.04

# 系统环境
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

# Java 环境
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 应用环境
ENV APP_HOME=/app
```

### 爬虫依赖组件
- **Java 运行时**: OpenJDK 21
- **Web 浏览器**: Google Chrome Stable
- **浏览器驱动**: ChromeDriver (自动版本匹配)
- **中文字体**: 文泉驿正黑体、微米黑
- **系统工具**: curl、wget、unzip、fontconfig

### 安全加固措施
- 非交互式安装模式
- 最小化依赖包安装
- 定期安全更新机制
- 用户权限限制

## ⚙️ 配置管理

### 环境变量配置 (.env)
```bash
# 应用配置
APP_PORT=8080
SPRING_PROFILES_ACTIVE=prod

# 数据库连接
DB_URL=jdbc:mysql://host.docker.internal:3306/sgcc_crawler
DB_USERNAME=inspection
DB_PASSWORD=ENC(encrypted_password)

# 钉钉通知
DINGTALK_WEBHOOK=ENC(encrypted_webhook)
DINGTALK_SECRET=ENC(encrypted_secret)
DINGTALK_ENABLED=true

# 爬虫配置
CRAWLER_HEADLESS=true
CRAWLER_MAX_PAGES=5
CRAWLER_REQUEST_INTERVAL=2000

# 数据持久化
DATA_VOLUME=/data/sgcc_bidding_crawler
```

### 服务编排配置 (docker-compose.yml)
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

## ▶️ 部署操作指南

### Linux/macOS 部署
```bash
# 1. 给脚本添加执行权限
chmod +x scripts/*.sh

# 2. 一键部署（构建+启动）
./scripts/deploy-container.sh up

# 3. 查看服务状态
./scripts/deploy-container.sh status

# 4. 实时查看日志
./scripts/deploy-container.sh logs

# 5. 停止服务
./scripts/deploy-container.sh down

# 6. 重启服务
./scripts/deploy-container.sh restart

# 7. 仅构建镜像
./scripts/deploy-container.sh build
```

### Windows 部署
```cmd
# 1. 一键部署（构建+启动）
scripts\deploy-container.bat up

# 2. 查看服务状态
scripts\deploy-container.bat status

# 3. 实时查看日志
scripts\deploy-container.bat logs

# 4. 停止服务
scripts\deploy-container.bat down

# 5. 重启服务
scripts\deploy-container.bat restart

# 6. 仅构建镜像
scripts\deploy-container.bat build
```

### Docker 原生命令
```bash
# 查看运行中的容器
docker ps

# 查看容器详细信息
docker inspect sgcc-crawler

# 查看容器日志
docker logs -f sgcc-crawler

# 进入容器调试
docker exec -it sgcc-crawler bash

# 查看容器资源使用
docker stats sgcc-crawler

# 查看网络配置
docker network ls
docker network inspect sgcc-crawler-network
```

## 🗃️ 数据持久化管理

### 自动挂载的数据卷
1. **应用数据卷**: `/data/sgcc_bidding_crawler` → 容器内 `/app/data`
2. **日志数据卷**: `./logs` → 容器内 `/app/logs`

### 数据备份策略
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

echo "Backup completed: $DATE"
```

### 数据恢复操作
```bash
# 恢复数据卷
docker run --rm \
  -v sgcc-data:/data \
  -v /backup:/backup \
  alpine tar xzf /backup/data-latest.tar.gz -C /data

# 恢复数据库
gunzip < /backup/db-latest.sql.gz | mysql -h host.docker.internal -u inspection -p sgcc_crawler
```

## 🏥 健康检查与监控

### 内置健康检查端点
```bash
# 基础健康状态
curl http://localhost:8080/api/actuator/health

# 详细健康信息
curl http://localhost:8080/api/actuator/health/detail

# 爬虫运行状态
curl http://localhost:8080/api/crawler/status

# 系统指标
curl http://localhost:8080/api/actuator/metrics
```

### 监控脚本示例
```bash
#!/bin/bash
# health-monitor.sh

SERVICE_URL="http://localhost:8080/api/actuator/health"
MAX_RETRIES=3
RETRY_DELAY=10

check_service() {
    local retry_count=0
    
    while [ $retry_count -lt $MAX_RETRIES ]; do
        if curl -f $SERVICE_URL >/dev/null 2>&1; then
            echo "[OK] $(date): Service is healthy"
            return 0
        else
            echo "[WARN] $(date): Service check failed, retry $((retry_count+1))/$MAX_RETRIES"
            retry_count=$((retry_count+1))
            sleep $RETRY_DELAY
        fi
    done
    
    echo "[ERROR] $(date): Service is unhealthy"
    # 触发告警或自动重启
    ./scripts/deploy-container.sh restart
    return 1
}

# 定时检查
while true; do
    check_service
    sleep 60
done
```

## 🔧 高级配置选项

### JVM 性能调优
```bash
# 在 .env 文件中配置
JAVA_OPTS=-Xms1g -Xmx2g \
          -XX:+UseG1GC \
          -XX:MaxGCPauseMillis=200 \
          -XX:+HeapDumpOnOutOfMemoryError \
          -XX:HeapDumpPath=/app/logs/ \
          -Dfile.encoding=UTF-8
```

### 容器资源限制
```yaml
# docker-compose.yml
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

### 网络自定义配置
```yaml
networks:
  sgcc-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1
```

## ❌ 故障排除指南

### 常见问题诊断

#### 1. 构建失败问题
```bash
# 清理构建环境
./scripts/deploy-container.sh down
docker builder prune -a
docker system prune -f

# 强制重新构建
./scripts/deploy-container.sh build --no-cache --force-rm
```

#### 2. 容器启动失败
```bash
# 查看详细错误日志
docker logs sgcc-crawler

# 检查容器配置
docker inspect sgcc-crawler | jq '.[].State'

# 验证环境变量
docker exec sgcc-crawler env

# 检查文件权限
docker exec sgcc-crawler ls -la /app
```

#### 3. 数据库连接问题
```bash
# 测试数据库连通性
docker exec sgcc-crawler nc -zv host.docker.internal 3306

# 验证数据库凭据
mysql -h host.docker.internal -u inspection -p -e "SELECT 1;"

# 检查数据库服务状态
systemctl status mysql  # 在数据库服务器上执行
```

#### 4. Chrome/爬虫功能异常
```bash
# 验证 Chrome 安装
docker exec sgcc-crawler google-chrome --version

# 验证 ChromeDriver
docker exec sgcc-crawler chromedriver --version

# 测试无头浏览器
docker exec sgcc-crawler google-chrome --no-sandbox --headless --dump-dom https://www.baidu.com

# 检查字体配置
docker exec sgcc-crawler fc-list :lang=zh
```

### 调试技巧集合
```bash
# 实时监控容器资源
docker stats sgcc-crawler

# 查看文件系统变更
docker diff sgcc-crawler

# 网络连接调试
docker exec sgcc-crawler netstat -tuln

# 进程监控
docker exec sgcc-crawler ps aux

# 磁盘使用情况
docker exec sgcc-crawler df -h
```

## 🔒 安全最佳实践

### 镜像安全加固
```bash
# 定期更新基础镜像
./scripts/build-container.sh latest

# 漏洞扫描
docker scan sgcc/crawler:latest

# 使用最小化基础镜像（可选）
# FROM ubuntu:20.04-slim
```

### 运行时安全配置
```yaml
# docker-compose.yml
services:
  sgcc-crawler:
    security_opt:
      - no-new-privileges:true
    read_only: true
    tmpfs:
      - /tmp
      - /var/run
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    user: "1000:1000"
```

### 网络安全策略
```bash
# 限制容器网络访问
iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
iptables -A INPUT -p tcp --dport 8080 -j LOG --log-prefix "SGCC_CRAWLER_BLOCKED: "
iptables -A INPUT -p tcp --dport 8080 -j DROP

# 使用专用网络
docker network create --internal sgcc-private-network
```

## 📈 性能优化建议

### 应用层优化
```yaml
# application.yml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 30
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

# crawler 配置优化
crawler:
  request-interval: 3000  # 增加请求间隔
  max-pages: 3           # 减少单次爬取页数
  headless: true         # 启用无头模式
```

### 系统层优化
```bash
# 系统参数调优
echo 'vm.max_map_count=262144' >> /etc/sysctl.conf
echo 'fs.file-max=65536' >> /etc/sysctl.conf
sysctl -p

# Docker 守护进程优化
# /etc/docker/daemon.json
{
  "default-ulimits": {
    "nofile": {
      "Name": "nofile",
      "Hard": 65536,
      "Soft": 65536
    }
  }
}
```

## 🔄 高可用部署方案

### 多实例负载均衡
```yaml
# docker-compose.ha.yml
version: '3.8'

services:
  sgcc-crawler-1:
    <<: *crawler-base
    container_name: sgcc-crawler-1
    ports:
      - "8080:8080"
    
  sgcc-crawler-2:
    <<: *crawler-base
    container_name: sgcc-crawler-2
    ports:
      - "8081:8080"

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - sgcc-crawler-1
      - sgcc-crawler-2
```

### 数据库高可用
```yaml
# docker-compose.db-ha.yml
version: '3.8'

services:
  mysql-master:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: sgcc_crawler
    
  mysql-slave:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
    depends_on:
      - mysql-master
```

## 📊 监控告警集成

### Prometheus 监控配置
```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'sgcc-crawler'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/api/actuator/prometheus'
    relabel_configs:
      - source_labels: [__address__]
        target_label: instance
        replacement: 'sgcc-crawler'
```

### 告警规则配置
```yaml
# alert-rules.yml
groups:
  - name: sgcc-crawler-alerts
    rules:
      - alert: ServiceDown
        expr: up{job="sgcc-crawler"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "SGCC Crawler service is down"
          
      - alert: HighMemoryUsage
        expr: process_memory_bytes{job="sgcc-crawler"} > 1.5e9
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage detected"
```

## 🎯 生产环境最佳实践

### 部署前检查清单
- [ ] Docker 环境版本符合要求
- [ ] 系统资源满足最低要求
- [ ] 数据库连接测试通过
- [ ] 钉钉通知配置验证
- [ ] 数据持久化路径权限确认
- [ ] 防火墙端口开放验证
- [ ] 备份策略制定完成

### 运维监控要点
- 定期检查容器健康状态
- 监控系统资源使用情况
- 跟踪应用日志异常信息
- 验证数据备份完整性
- 更新安全补丁和依赖

### 应急响应流程
1. **故障发现**: 监控告警触发
2. **初步诊断**: 查看容器状态和日志
3. **问题定位**: 分析错误原因
4. **应急处理**: 重启服务或回滚版本
5. **根本解决**: 修复配置或代码问题
6. **验证恢复**: 确认服务正常运行

---
**版本**: 1.0.0  
**最后更新**: 2024年  
**技术支持**: 请参考故障排除章节或联系维护团队