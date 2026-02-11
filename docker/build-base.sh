#!/bin/bash
# ============================================================
# 国网招标爬虫 - 基础镜像构建（仅需执行一次）
# 构建内容: Ubuntu 20.04 + OpenJDK 21 + Chrome + 中文字体
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_NAME="sgcc/base:java21"

echo "=========================================="
echo " 构建基础镜像: $IMAGE_NAME"
echo "=========================================="

# 构建镜像
docker build \
    -f "$SCRIPT_DIR/Dockerfile.base" \
    -t "$IMAGE_NAME" \
    --progress=plain \
    "$SCRIPT_DIR"

echo ""
echo "=========================================="
echo " 基础镜像构建成功"
echo "=========================================="
docker images | head -1
docker images | grep sgcc/base
echo ""
echo "下一步: 执行 ./deploy.sh init 完成首次部署"
