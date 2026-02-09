#!/bin/bash
# 编译脚本

set -e

echo "===== 开始编译项目 ====="

# 进入项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

echo "项目目录: $PROJECT_DIR"

# 清理并编译
echo "执行 Maven 编译..."
mvn clean package -DskipTests -B

echo "===== 编译完成 ====="
echo "JAR包位置: $PROJECT_DIR/target/"
ls -la target/*.jar 2>/dev/null || echo "未找到JAR文件"
