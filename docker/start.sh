#!/bin/bash
# ============================================================
# 国网招标爬虫 - 容器启动脚本
# 说明: 后台启动应用并保持容器运行，便于 exec 进入调试
# ============================================================

echo "=========================================="
echo " 国网招标爬虫 容器启动"
echo " $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

cd /app

# ---- 前置检查 ----
if [ ! -f "app.jar" ]; then
    echo "[ERROR] 未找到 /app/app.jar，请检查部署目录"
    # 保持容器运行以便排查
    exec tail -f /dev/null
fi

# 确保目录存在
mkdir -p logs data

# 确保 chromedriver 有执行权限
if [ -f "drivers/linux/chromedriver-linux64/chromedriver" ]; then
    chmod +x drivers/linux/chromedriver-linux64/chromedriver
fi

# ---- 启动应用 ----
echo "[INFO] 启动应用..."

# 构建启动参数：如果存在外部配置目录，优先使用外部配置
JAVA_OPTS=""
if [ -d "/app/config" ] && [ -f "/app/config/application.yml" ]; then
    JAVA_OPTS="--spring.config.additional-location=file:/app/config/"
    echo "[INFO] 使用外部配置: /app/config/"
fi

nohup java -jar app.jar $JAVA_OPTS > logs/startup.log 2>&1 &
APP_PID=$!
echo $APP_PID > app.pid

echo "[INFO] PID: $APP_PID"
echo "[INFO] 日志: tail -f /app/logs/startup.log"
echo "=========================================="

# ---- 保持容器运行 ----
while true; do
    if ! kill -0 $APP_PID 2>/dev/null; then
        echo "[WARN] $(date '+%H:%M:%S') 应用进程已退出"
        echo "--- 最近日志 ---"
        tail -20 logs/startup.log 2>/dev/null
        echo "--- 容器保持运行，可 exec 进入排查 ---"
    fi
    sleep 30
done
