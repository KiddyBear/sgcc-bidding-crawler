#!/bin/bash
# ============================================================
# 国网招标爬虫 - 统一部署脚本
# 用法: ./deploy.sh <命令>
#
# 命令:
#   init         首次部署（构建基础镜像 + 编译 + 部署 + 启动）
#   build        仅编译打包
#   upgrade      升级应用（编译 + 替换文件 + 重启容器）
#   pack         打包离线安装包（含基础镜像 + 程序 + 配置）
#   pack-upgrade 打包升级包（仅程序，不含镜像）
#   start        启动容器
#   stop         停止容器
#   restart      重启容器
#   status       查看运行状态
#   logs         查看应用日志
#   shell        进入容器终端
# ============================================================

set -e

# ==================== 配置区 ====================
DEPLOY_DIR="/data/sgcc-crawler"             # 服务器部署目录（挂载为 /app）
CONTAINER_NAME="sgcc-crawler"               # 容器名称
BASE_IMAGE="sgcc/base:java21"               # 基础镜像名称
PORT="8080"                                 # 宿主机端口
JAR_NAME="sgcc-bidding-crawler-1.0.0.jar"   # 构建产物 JAR 包名
# ================================================

# 自动定位项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ---------- 工具函数 ----------

info()  { echo -e "\033[32m[INFO]\033[0m $1"; }
warn()  { echo -e "\033[33m[WARN]\033[0m $1"; }
error() { echo -e "\033[31m[ERROR]\033[0m $1"; }

# 检查基础镜像是否存在
check_base_image() {
    if ! docker image inspect "$BASE_IMAGE" &>/dev/null; then
        error "基础镜像 $BASE_IMAGE 不存在"
        echo "  请先执行: ./deploy.sh init"
        exit 1
    fi
}

# 检查容器是否在运行
is_running() {
    docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"
}

# 检查容器是否存在（包括停止的）
container_exists() {
    docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"
}

# ---------- 命令实现 ----------

# 初始化配置文件（仅首次部署）
do_init_config() {
    local config_dir="$DEPLOY_DIR/config"
    local src_resources="$PROJECT_ROOT/src/main/resources"

    # 配置已存在则跳过
    if [ -f "$config_dir/application.yml" ]; then
        info "配置文件已存在，跳过初始化"
        return
    fi

    info "初始化配置文件到 $config_dir ..."
    mkdir -p "$config_dir"

    # 复制应用配置
    cp -f "$src_resources/application.yml" "$config_dir/"
    cp -f "$src_resources/application-prod.yml" "$config_dir/"

    # 复制配置参考模板
    if [ -f "$PROJECT_ROOT/.env.example" ]; then
        cp -f "$PROJECT_ROOT/.env.example" "$config_dir/.env.example"
    fi

    echo ""
    warn "============================================"
    warn " 重要: 请先修改配置文件再启动应用!"
    warn " 配置目录: $config_dir"
    warn "   application.yml      - 数据库、爬虫等核心配置"
    warn "   application-prod.yml  - 生产环境覆盖配置"
    warn "   .env.example          - 配置项参考说明"
    warn "============================================"
    echo ""
    read -p "配置是否已修改完毕? (y/N): " confirm
    if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
        info "请修改完配置后执行: ./deploy.sh start"
        exit 0
    fi
}

# 构建基础镜像
do_build_base() {
    info "构建基础镜像 $BASE_IMAGE ..."
    docker build \
        -f "$SCRIPT_DIR/Dockerfile.base" \
        -t "$BASE_IMAGE" \
        --progress=plain \
        "$SCRIPT_DIR"
    info "基础镜像构建完成"
}

# Maven 编译打包
do_build() {
    info "编译打包应用..."
    cd "$PROJECT_ROOT"

    if ! command -v mvn &>/dev/null; then
        error "未安装 Maven，请先安装"
        exit 1
    fi

    mvn clean package -DskipTests -q
    info "编译完成: target/$JAR_NAME"
}

# 部署文件到服务器目录
do_deploy_files() {
    local target_dir="$PROJECT_ROOT/target"

    # 检查构建产物
    if [ ! -f "$target_dir/$JAR_NAME" ]; then
        error "未找到 $target_dir/$JAR_NAME，请先执行 build"
        exit 1
    fi

    info "部署文件到 $DEPLOY_DIR ..."
    mkdir -p "$DEPLOY_DIR"/{logs,data,config}

    # 复制应用包
    cp -f "$target_dir/$JAR_NAME" "$DEPLOY_DIR/app.jar"

    # 同步依赖库（整体替换，确保依赖版本一致）
    if [ -d "$target_dir/lib" ]; then
        info "同步依赖库 lib/ ..."
        rm -rf "$DEPLOY_DIR/lib"
        cp -rf "$target_dir/lib" "$DEPLOY_DIR/lib"
    fi

    # 复制 ChromeDriver（仅 Linux）
    if [ -d "$target_dir/drivers/linux" ]; then
        mkdir -p "$DEPLOY_DIR/drivers/linux"
        cp -rf "$target_dir/drivers/linux/"* "$DEPLOY_DIR/drivers/linux/"
        chmod +x "$DEPLOY_DIR/drivers/linux/chromedriver-linux64/chromedriver" 2>/dev/null || true
    fi

    # 复制启动脚本
    cp -f "$SCRIPT_DIR/start.sh" "$DEPLOY_DIR/start.sh"
    chmod +x "$DEPLOY_DIR/start.sh"

    info "文件部署完成（配置文件未覆盖）"
}

# 启动容器
do_start() {
    check_base_image

    if is_running; then
        warn "容器已在运行中"
        return
    fi

    # 清理已停止的同名容器
    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi

    if [ ! -f "$DEPLOY_DIR/app.jar" ]; then
        error "部署目录 $DEPLOY_DIR 中无 app.jar，请先执行 deploy"
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

# 停止容器
do_stop() {
    if is_running; then
        info "停止容器..."
        docker stop "$CONTAINER_NAME"
        info "容器已停止"
    else
        warn "容器未在运行"
    fi
}

# 重启容器
do_restart() {
    do_stop
    # 删除旧容器
    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi
    do_start
}

# ---------- 组合命令 ----------

# 首次部署
do_init() {
    echo "=========================================="
    echo " 国网招标爬虫 - 首次部署"
    echo "=========================================="

    # 1. 构建基础镜像
    if docker image inspect "$BASE_IMAGE" &>/dev/null; then
        info "基础镜像已存在，跳过构建"
    else
        do_build_base
    fi

    # 2. 编译打包
    do_build

    # 3. 部署文件
    do_deploy_files

    # 4. 初始化配置（首次需要修改）
    do_init_config

    # 5. 启动容器
    do_start

    echo ""
    echo "=========================================="
    info "首次部署完成!"
    echo "=========================================="
    do_show_status
}

# 升级应用
do_upgrade() {
    echo "=========================================="
    echo " 国网招标爬虫 - 应用升级"
    echo "=========================================="

    check_base_image

    # 1. 编译打包
    do_build

    # 2. 停止容器
    do_stop

    # 3. 删除旧容器
    if container_exists; then
        docker rm "$CONTAINER_NAME" &>/dev/null || true
    fi

    # 4. 替换文件（替换 jar + lib + drivers，保留 config + logs + data）
    do_deploy_files

    # 5. 启动容器
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
    echo "--- 部署目录 ---"
    if [ -d "$DEPLOY_DIR" ]; then
        ls -lh "$DEPLOY_DIR/app.jar" 2>/dev/null || echo "  app.jar 不存在"
        echo "  logs/  $(du -sh "$DEPLOY_DIR/logs" 2>/dev/null | cut -f1 || echo '空')"
        echo "  data/  $(du -sh "$DEPLOY_DIR/data" 2>/dev/null | cut -f1 || echo '空')"
    else
        warn "部署目录不存在: $DEPLOY_DIR"
    fi
}

# 查看日志
do_logs() {
    local log_file="$DEPLOY_DIR/logs/startup.log"
    if [ -f "$log_file" ]; then
        tail -f "$log_file"
    else
        warn "日志文件不存在: $log_file"
        echo "尝试查看容器日志..."
        docker logs -f "$CONTAINER_NAME" 2>/dev/null || echo "容器不存在"
    fi
}

# 进入容器
do_shell() {
    if is_running; then
        docker exec -it "$CONTAINER_NAME" /bin/bash
    else
        error "容器未运行，请先启动"
        exit 1
    fi
}

# ---------- 打包命令 ----------

# 打包离线安装包（含镜像）
do_pack() {
    echo "=========================================="
    echo " 国网招标爬虫 - 打包离线安装包"
    echo "=========================================="

    # 1. 确保基础镜像存在
    if ! docker image inspect "$BASE_IMAGE" &>/dev/null; then
        info "基础镜像不存在，开始构建..."
        do_build_base
    fi

    # 2. 编译打包
    do_build

    local version=$(echo "$JAR_NAME" | grep -oP '\d+\.\d+\.\d+')
    local pkg_name="sgcc-crawler-v${version}"
    local pkg_dir="$PROJECT_ROOT/target/$pkg_name"
    local target_dir="$PROJECT_ROOT/target"

    # 3. 创建安装包目录
    info "创建安装包目录..."
    rm -rf "$pkg_dir"
    mkdir -p "$pkg_dir"/{image,app,config}

    # 4. 导出基础镜像
    info "导出基础镜像（约需 2-5 分钟）..."
    docker save "$BASE_IMAGE" | gzip > "$pkg_dir/image/sgcc-base-java21.tar.gz"

    # 5. 复制应用文件
    info "复制应用文件..."
    cp -f "$target_dir/$JAR_NAME" "$pkg_dir/app/app.jar"
    [ -d "$target_dir/lib" ] && cp -rf "$target_dir/lib" "$pkg_dir/app/lib"
    if [ -d "$target_dir/drivers/linux" ]; then
        mkdir -p "$pkg_dir/app/drivers/linux"
        cp -rf "$target_dir/drivers/linux/"* "$pkg_dir/app/drivers/linux/"
    fi
    cp -f "$SCRIPT_DIR/start.sh" "$pkg_dir/app/start.sh"

    # 6. 复制配置模板
    info "复制配置模板..."
    cp -f "$PROJECT_ROOT/src/main/resources/application.yml" "$pkg_dir/config/"
    cp -f "$PROJECT_ROOT/src/main/resources/application-prod.yml" "$pkg_dir/config/"
    [ -f "$PROJECT_ROOT/.env.example" ] && cp -f "$PROJECT_ROOT/.env.example" "$pkg_dir/config/.env.example"

    # 7. 复制安装脚本
    cp -f "$SCRIPT_DIR/install.sh" "$pkg_dir/install.sh"
    chmod +x "$pkg_dir/install.sh" "$pkg_dir/app/start.sh"

    # 8. 压缩
    info "压缩安装包..."
    cd "$PROJECT_ROOT/target"
    tar czf "${pkg_name}.tar.gz" "$pkg_name"
    rm -rf "$pkg_dir"

    echo ""
    echo "=========================================="
    info "离线安装包打包完成!"
    echo "=========================================="
    echo "安装包: target/${pkg_name}.tar.gz"
    ls -lh "$PROJECT_ROOT/target/${pkg_name}.tar.gz"
    echo ""
    echo "使用方法:"
    echo "  1. 传输到服务器:  scp target/${pkg_name}.tar.gz user@server:/tmp/"
    echo "  2. 解压:          tar xzf ${pkg_name}.tar.gz"
    echo "  3. 修改配置:      vi ${pkg_name}/config/application.yml"
    echo "  4. 安装:          cd ${pkg_name} && ./install.sh"
}

# 打包升级包（不含镜像）
do_pack_upgrade() {
    echo "=========================================="
    echo " 国网招标爬虫 - 打包升级包"
    echo "=========================================="

    # 1. 编译打包
    do_build

    local version=$(echo "$JAR_NAME" | grep -oP '\d+\.\d+\.\d+')
    local pkg_name="sgcc-crawler-upgrade-v${version}"
    local pkg_dir="$PROJECT_ROOT/target/$pkg_name"
    local target_dir="$PROJECT_ROOT/target"

    # 2. 创建升级包目录
    info "创建升级包目录..."
    rm -rf "$pkg_dir"
    mkdir -p "$pkg_dir/app"

    # 3. 复制应用文件
    info "复制应用文件..."
    cp -f "$target_dir/$JAR_NAME" "$pkg_dir/app/app.jar"
    [ -d "$target_dir/lib" ] && cp -rf "$target_dir/lib" "$pkg_dir/app/lib"
    if [ -d "$target_dir/drivers/linux" ]; then
        mkdir -p "$pkg_dir/app/drivers/linux"
        cp -rf "$target_dir/drivers/linux/"* "$pkg_dir/app/drivers/linux/"
    fi
    cp -f "$SCRIPT_DIR/start.sh" "$pkg_dir/app/start.sh"

    # 4. 复制安装脚本
    cp -f "$SCRIPT_DIR/install.sh" "$pkg_dir/install.sh"
    chmod +x "$pkg_dir/install.sh" "$pkg_dir/app/start.sh"

    # 5. 压缩
    info "压缩升级包..."
    cd "$PROJECT_ROOT/target"
    tar czf "${pkg_name}.tar.gz" "$pkg_name"
    rm -rf "$pkg_dir"

    echo ""
    echo "=========================================="
    info "升级包打包完成!"
    echo "=========================================="
    echo "升级包: target/${pkg_name}.tar.gz"
    ls -lh "$PROJECT_ROOT/target/${pkg_name}.tar.gz"
    echo ""
    echo "使用方法:"
    echo "  1. 传输到服务器:  scp target/${pkg_name}.tar.gz user@server:/tmp/"
    echo "  2. 解压:          tar xzf ${pkg_name}.tar.gz"
    echo "  3. 升级:          cd ${pkg_name} && ./install.sh upgrade"
}

# ---------- 帮助信息 ----------

show_help() {
    echo "用法: $0 <命令>"
    echo ""
    echo "部署命令:"
    echo "  init           首次部署（构建镜像 + 编译 + 部署 + 启动）"
    echo "  build          仅编译打包应用"
    echo "  upgrade        升级应用（编译 + 替换 + 重启）"
    echo ""
    echo "打包命令:"
    echo "  pack           打包离线安装包（含镜像，用于无网环境首次部署）"
    echo "  pack-upgrade   打包升级包（不含镜像，用于应用升级）"
    echo ""
    echo "运维命令:"
    echo "  start          启动容器"
    echo "  stop           停止容器"
    echo "  restart        重启容器"
    echo "  status         查看运行状态"
    echo "  logs           查看应用日志 (Ctrl+C 退出)"
    echo "  shell          进入容器终端"
    echo ""
    echo "配置:"
    echo "  部署目录:  $DEPLOY_DIR"
    echo "  基础镜像:  $BASE_IMAGE"
    echo "  容器名称:  $CONTAINER_NAME"
    echo "  端口映射:  $PORT -> 8080"
}

# ---------- 入口 ----------

case "${1:-}" in
    init)           do_init ;;
    build)          do_build ;;
    upgrade)        do_upgrade ;;
    pack)           do_pack ;;
    pack-upgrade)   do_pack_upgrade ;;
    start)          do_start ;;
    stop)           do_stop ;;
    restart)        do_restart ;;
    status)         do_show_status ;;
    logs)           do_logs ;;
    shell)          do_shell ;;
    *)              show_help ;;
esac
