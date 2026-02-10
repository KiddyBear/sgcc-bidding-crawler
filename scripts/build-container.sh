#!/bin/bash

# 国网招标爬虫 - Docker 镜像构建脚本 (Linux/macOS)
# 使用方法: ./scripts/build-container.sh [tag]

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033>&31m'
GREEN='\033>&32m'
YELLOW='\033>&33m'
BLUE='\033>&34m'
NC='\033>&0m' # No Color

# 默认镜像标签
DEFAULT_TAG="latest"
TAG=${1:-$DEFAULT_TAG}

# 项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DOCKER_DIR="${PROJECT_ROOT}/docker"

echo -e "${BLUE}================================${NC}"
echo -e "${BLUE}  国网招标爬虫 - Docker 构建  ${NC}"
echo -e "${BLUE}================================${NC}"

# 检查必要文件是否存在
echo -e "${YELLOW}检查必要文件...${NC}"

if [ ! -f "${PROJECT_ROOT}/pom.xml" ]; then
    echo -e "${RED}错误: 找不到 pom.xml 文件${NC}"
    exit 1
fi

if [ ! -f "${DOCKER_DIR}/Dockerfile" ]; then
    echo -e "${RED}错误: 找不到 Dockerfile${NC}"
    exit 1
fi

if [ ! -f "${PROJECT_ROOT}/target/sgcc-bidding-crawler-1.0.0.jar" ]; then
    echo -e "${YELLOW}警告: 找不到编译后的 jar 包，正在执行 Maven 构建...${NC}"
    
    # 进入项目根目录执行 Maven 构建
    cd "${PROJECT_ROOT}"
    mvn clean package -DskipTests
    
    if [ ! -f "${PROJECT_ROOT}/target/sgcc-bidding-crawler-1.0.0.jar" ]; then
        echo -e "${RED}错误: Maven 构建失败，无法找到 jar 包${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Maven 构建完成${NC}"
fi

# 构建 Docker 镜像
echo -e "${YELLOW}开始构建 Docker 镜像...${NC}"
echo -e "${BLUE}镜像标签: sgcc/crawler:${TAG}${NC}"

cd "${DOCKER_DIR}"

# 执行 Docker 构建
docker build -t "sgcc/crawler:${TAG}" .

if [ $? -eq 0 ]; then
    echo -e "${GREEN}Docker 镜像构建成功！${NC}"
    echo -e "${GREEN}镜像信息:${NC}"
    docker images | grep "sgcc/crawler"
else
    echo -e "${RED}Docker 镜像构建失败！${NC}"
    exit 1
fi

echo -e "${BLUE}================================${NC}"
echo -e "${GREEN}构建完成！${NC}"
echo -e "${BLUE}================================${NC}"