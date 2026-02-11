#!/bin/bash
# ============================================================
# 国网招标爬虫 - 离线安装/升级脚本
# 此脚本随安装包发布，在目标服务器上运行，无需外网
#
# 用法: ./install.sh [命令]
#
# 命令:
#   install    首次安装（加载镜像 + 部署 + 配置 + 启动）
#   upgrade    升级应用（替换程序包 + 重启，保留配置和数据）
#   start      启动容器
#   stop       停止容器
#   restart    重启容器
#   status     查看运行状态
#   logs       查看应用日志
#   shell      进入容器终端
#
# 不带参数则默认执行 install
# ============================================================

set -e

# ==================== 配置区 ====================
DEPLOY_DIR="/data/sgcc-crawler"             # 部署目录（挂载为容器 /app）
CONTAINER_NAME="sgcc-crawler"               # 容器名称
BASE_IMAGE="sgcc/base:java21"               # 基础镜像名称
PORT="8080"                                 # 宿主机端口
# ================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------- 工具函数 ----------

info()  { echo -e "\033[32m[INFO]\033[0m $1"; }
warn()  { echo -e "\033[33m[WARN]\033[0m $1"; }
error() { echo -e "\033[31m[ERROR]\033[0m $1"; }

is_running() {
    docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"
}

container_exists() {
    docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"
}

# ---------- 核心操作 ----------

# 加载基础镜像
do_load_image() {
    if docker image inspect "$BASE_IMAGE" &>/dev/null; then
        info "基础镜像已存在，跳过加载"
        return
    fi

    local image_file=$(find "$SCRIPT_DIR/image" -name "*.tar.gz" -o -name "*.tar" 2>/dev/null | head -1)

    if [ -z "$image_file" ]; then
        error "未找到镜像文件，请确认 image/ 目录下有 .tar.gz 文件"
        exit 1
    fi

    info "加载基础镜像: $(basename "$image_file") ..."
    info "（文件较大，约需 1-3 分钟，请耐心等待）"
    docker load < "$image_file"
    info "基础镜像加载完成"
}

# 部署应用文件
do_deploy_files() {
    local app_dir="$SCRIPT_DIR/app"

    if [ ! -f "$app_dir/app.jar" ]; then
        error "未找到应用文件: $app_dir/app.jar"
        exit 1
    fi

    info "部署文件到 $DEPLOY_DIR ..."
    mkdir -p "$DEPLOY_DIR"/{logs,data,config}

    # 复制应用包
    cp -f "$app_dir/app.jar" "$DEPLOY_DIR/app.jar"

    # 同步依赖库（整体替换）
    if [ -d "$app_dir/lib" ]; then
        info "同步依赖库 lib/ ..."
        rm -rf "$DEPLOY_DIR/lib"
        cp -rf "$app_dir/lib" "$DEPLOY_DIR/lib"
    fi

    # 复制驱动
    if [ -d "$app_dir/drivers" ]; then
        rm -rf "$DEPLOY_DIR/drivers"
        cp -rf "$app_dir/drivers" "$DEPLOY_DIR/drivers"
        chmod +x "$DEPLOY_DIR/drivers/linux/chromedriver-linux64/chromedriver" 2>/dev/null || true
    fi

    # 复制启动脚本
    cp -f "$app_dir/start.sh" "$DEPLOY_DIR/start.sh"
    chmod +x "$DEPLOY_DIR/start.sh"

    info "文件部署完成（配置文件未覆盖）"
}

# 初始化配置（仅首次安装）
do_init_config() {
    local config_dir="$DEPLOY_DIR/config"

    # 已有配置则跳过
    if [ -f "$config_dir/application.yml" ]; then
        info "配置文件已存在，保留现有配置"
        return
    fi

    # 从安装包复制配置模板
    if [ -d "$SCRIPT_DIR/config" ]; then
        info "复制配置模板到 $config_dir ..."
        cp -f "$SCRIPT_DIR/config/"* "$config_dir/" 2>/dev/null || true
    fi

    echo ""
    warn "============================================"
    warn " 重要: 请检查配置文件是否已正确修改!"
    warn " 配置目录: $config_dir"
    warn "   application.yml      - 数据库、爬虫等核心配置"
    warn "   application-prod.yml - 生产环境覆盖配置"
    warn "   .env.example         - 配置项参考说明"
    warn "============================================"
    echo ""
    warn "提示: 建议在安装前先修改 config/ 目录下的配置文件"
    echo ""
    read -p "继续安装? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        info "请修改完配置后重新执行: ./install.sh"
        exit 0
    fi
}

# 启动容器
do_start() {
    if ! docker image inspect "$BASE_IMAGE" &>/dev/null; then
        error "基础镜像不存在，请先执行安装"
        exit 1
    fi

    if is_running; then
        warn "容器已在运行中"
        return
    fi

    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi

    if [ ! -f "$DEPLOY_DIR/app.jar" ]; then
        error "未找到 $DEPLOY_DIR/app.jar"
        exit 1
    fi

    info "启动容器 $CONTAINER_NAME ..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        --restart always \
        -p "$PORT":8080 \
        -v "$DEPLOY_DIR":/app \
        "$BASE_IMAGE" \
        /app/start.sh

    sleep 3
    if is_running; then
        info "容器启动成功  端口: $PORT"
    else
        error "容器启动失败，查看日志:"
        docker logs "$CONTAINER_NAME" 2>&1 | tail -20
        exit 1
    fi
}

do_stop() {
    if is_running; then
        info "停止容器..."
        docker stop "$CONTAINER_NAME"
        info "已停止"
    else
        warn "容器未运行"
    fi
}

do_restart() {
    do_stop
    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi
    do_start
}

# ---------- 组合命令 ----------

# 首次安装
do_install() {
    echo "=========================================="
    echo " 国网招标爬虫 - 离线安装"
    echo "=========================================="

    # 检查 Docker
    if ! command -v docker &>/dev/null; then
        error "未安装 Docker，请先安装 Docker 20.10+"
        exit 1
    fi

    # 1. 加载镜像
    do_load_image

    # 2. 部署文件
    do_deploy_files

    # 3. 初始化配置
    do_init_config

    # 4. 启动
    do_start

    echo ""
    echo "=========================================="
    info "安装完成!"
    echo "=========================================="
    do_show_status
    echo ""
    echo "常用命令:"
    echo "  ./install.sh status   查看状态"
    echo "  ./install.sh logs     查看日志"
    echo "  ./install.sh restart  重启服务"
    echo "  ./install.sh shell    进入容器"
}

# 升级
do_upgrade() {
    echo "=========================================="
    echo " 国网招标爬虫 - 应用升级"
    echo "=========================================="

    # 加载镜像（如安装包中包含）
    if [ -d "$SCRIPT_DIR/image" ] && ls "$SCRIPT_DIR/image/"*.tar* &>/dev/null 2>&1; then
        do_load_image
    fi

    # 停止
    do_stop
    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi

    # 替换文件（保留 config + logs + data）
    do_deploy_files

    # 启动
    do_start

    echo ""
    echo "=========================================="
    info "升级完成!"
    echo "=========================================="
    do_show_status
}

# 查看状态
do_show_status() {
    echo ""
    echo "--- 容器状态 ---"
    if is_running; then
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -1
        docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep "$CONTAINER_NAME"
    else
        warn "容器未运行"
    fi
    echo ""
    if [ -d "$DEPLOY_DIR" ]; then
        echo "--- 部署目录 ---"
        ls -lh "$DEPLOY_DIR/app.jar" 2>/dev/null || echo "  app.jar 不存在"
        echo "  config/ $([ -f "$DEPLOY_DIR/config/application.yml" ] && echo '已配置' || echo '未配置')"
        echo "  logs/   $(du -sh "$DEPLOY_DIR/logs" 2>/dev/null | cut -f1 || echo '空')"
        echo "  data/   $(du -sh "$DEPLOY_DIR/data" 2>/dev/null | cut -f1 || echo '空')"
    fi
}

do_logs() {
    local log_file="$DEPLOY_DIR/logs/startup.log"
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        docker logs -f "$CONTAINER_NAME" 2>/dev/null || echo "容器不存在"
    fi
}

do_shell() {
    if is_running; then
        docker exec -it "$CONTAINER_NAME" /bin/bash
    else
        error "容器未运行"
        exit 1
    fi
}

# ---------- 帮助 ----------

show_help() {
    echo "国网招标爬虫 - 离线安装/升级脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "  install    首次安装（默认）"
    echo "  upgrade    升级应用"
    echo "  start      启动容器"
    echo "  stop       停止容器"
    echo "  restart    重启容器"
    echo "  status     查看状态"
    echo "  logs       查看日志 (Ctrl+C 退出)"
    echo "  shell      进入容器"
    echo ""
    echo "首次安装: 解压安装包 → 修改 config/ 下的配置 → ./install.sh"
    echo "应用升级: 解压升级包 → ./install.sh upgrade"
}

# ---------- 入口 ----------

case "${1:-install}" in
    install)    do_install ;;
    upgrade)    do_upgrade ;;
    start)      do_start ;;
    stop)       do_stop ;;
    restart)    do_restart ;;
    status)     do_show_status ;;
    logs)       do_logs ;;
    shell)      do_shell ;;
    help|-h)    show_help ;;
    *)          show_help ;;
esac
