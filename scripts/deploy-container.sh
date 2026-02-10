#!/bin/bash

# 国网招标爬虫 - 容器化部署脚本 (Linux/macOS)
# 使用方法: ./scripts/deploy-container.sh [操作] [参数]
# 操作: build|up|down|restart|logs|status

set -e

# 颜色定义
RED='\033>&31m'
GREEN='\033>&32m'
YELLOW='\033>&33m'
BLUE='\033>&34m'
NC='\033>&0m'

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_DIR="${PROJECT_ROOT}/docker"
SCRIPTS_DIR="${PROJECT_ROOT}/scripts"

# 默认操作
ACTION=${1:-"up"}
TAG=${2:-"latest"}

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}  国网招标爬虫 - 容器部署  ${NC}"
echo -e "${BLUE}================================${NC}"

# 检查 Docker 和 Docker Compose
check_dependencies() {
    echo -e "${YELLOW}检查依赖环境...${NC}"
    
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}错误: 未安装 Docker${NC}"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}错误: 未安装 Docker Compose${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Docker 环境检查通过${NC}"
}

# 构建镜像
build_image() {
    echo -e "${YELLOW}构建 Docker 镜像...${NC}"
    "${SCRIPTS_DIR}/build-container.sh" "${TAG}"
}

# 启动服务
start_service() {
    echo -e "${YELLOW}启动服务...${NC}"
    
    # 检查 .env 文件
    if [ ! -f "${PROJECT_ROOT}/.env" ]; then
        echo -e "${YELLOW}警告: 未找到 .env 文件，复制 .env.example${NC}"
        cp "${PROJECT_ROOT}/.env.example" "${PROJECT_ROOT}/.env"
        echo -e "${GREEN}已创建 .env 文件，请根据实际情况修改配置${NC}"
    fi
    
    # 创建数据目录
    DATA_VOLUME=$(grep "^DATA_VOLUME=" "${PROJECT_ROOT}/.env" | cut -d'=' -f2)
    if [ -n "$DATA_VOLUME" ] && [ ! -d "$DATA_VOLUME" ]; then
        echo -e "${YELLOW}创建数据目录: $DATA_VOLUME${NC}"
        mkdir -p "$DATA_VOLUME"
    fi
    
    # 启动容器
    cd "${DOCKER_DIR}"
    docker-compose up -d
    
    echo -e "${GREEN}服务启动完成！${NC}"
}

# 停止服务
stop_service() {
    echo -e "${YELLOW}停止服务...${NC}"
    cd "${DOCKER_DIR}"
    docker-compose down
    echo -e "${GREEN}服务已停止${NC}"
}

# 重启服务
restart_service() {
    echo -e "${YELLOW}重启服务...${NC}"
    stop_service
    sleep 2
    start_service
}

# 查看日志
show_logs() {
    echo -e "${YELLOW}查看容器日志...${NC}"
    cd "${DOCKER_DIR}"
    docker-compose logs -f
}

# 查看状态
show_status() {
    echo -e "${YELLOW}查看服务状态...${NC}"
    cd "${DOCKER_DIR}"
    docker-compose ps
}

# 显示帮助
show_help() {
    echo "使用方法: $0 [操作] [参数]"
    echo ""
    echo "操作:"
    echo "  build     构建 Docker 镜像"
    echo "  up        启动服务（默认）"
    echo "  down      停止服务"
    echo "  restart   重启服务"
    echo "  logs      查看日志"
    echo "  status    查看状态"
    echo "  help      显示帮助"
    echo ""
    echo "示例:"
    echo "  $0 build          # 构建镜像"
    echo "  $0 up             # 启动服务"
    echo "  $0 up v1.0.0      # 使用指定标签启动"
    echo "  $0 logs           # 实时查看日志"
}

# 主逻辑
case "$ACTION" in
    "build")
        check_dependencies
        build_image
        ;;
    "up")
        check_dependencies
        build_image
        start_service
        ;;
    "down")
        check_dependencies
        stop_service
        ;;
    "restart")
        check_dependencies
        restart_service
        ;;
    "logs")
        check_dependencies
        show_logs
        ;;
    "status")
        check_dependencies
        show_status
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        echo -e "${RED}未知操作: $ACTION${NC}"
        show_help
        exit 1
        ;;
esac

echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}操作完成！${NC}"
echo -e "${BLUE}================================${NC}"