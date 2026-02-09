#!/bin/bash
# 部署脚本

set -e

echo "===== 开始部署服务 ====="

# 进入项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DOCKER_DIR="$PROJECT_DIR/docker"

cd "$DOCKER_DIR"

# 检查.env文件
if [ ! -f ".env" ]; then
    echo "警告: .env 文件不存在，将从模板创建..."
    if [ -f ".env.example" ]; then
        cp .env.example .env
        echo "请编辑 $DOCKER_DIR/.env 文件，配置钉钉Webhook等参数"
    fi
fi

# 解析命令行参数
ACTION=${1:-up}

case $ACTION in
    up)
        echo "启动服务..."
        docker-compose up -d --build
        echo "等待服务启动..."
        sleep 10
        docker-compose ps
        echo "===== 服务已启动 ====="
        echo "访问地址: http://localhost:8080"
        echo "健康检查: http://localhost:8080/api/health"
        ;;
    down)
        echo "停止服务..."
        docker-compose down
        echo "===== 服务已停止 ====="
        ;;
    restart)
        echo "重启服务..."
        docker-compose restart
        echo "===== 服务已重启 ====="
        ;;
    logs)
        echo "查看日志..."
        docker-compose logs -f --tail=100
        ;;
    status)
        echo "服务状态..."
        docker-compose ps
        ;;
    rebuild)
        echo "重新构建并启动..."
        docker-compose down
        docker-compose build --no-cache
        docker-compose up -d
        echo "===== 服务已重建并启动 ====="
        ;;
    *)
        echo "用法: $0 {up|down|restart|logs|status|rebuild}"
        echo "  up      - 启动服务"
        echo "  down    - 停止服务"
        echo "  restart - 重启服务"
        echo "  logs    - 查看日志"
        echo "  status  - 查看状态"
        echo "  rebuild - 重新构建并启动"
        exit 1
        ;;
esac
