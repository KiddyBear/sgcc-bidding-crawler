# 国网招标信息爬虫

基于 Spring Boot + Selenium 的国网电子商务平台招标公告自动采集系统。

---

## 技术栈

- Java 21 / Spring Boot 3.2
- Selenium + Chrome（无头模式）
- MyBatis-Plus + MySQL
- 钉钉机器人通知
- Docker 容器化部署

---

## 项目结构

```
├── docker/                     # Docker 部署文件
│   ├── Dockerfile.base         # 基础镜像（Ubuntu + JDK + Chrome）
│   ├── deploy.sh               # 统一部署脚本（开发机用）
│   ├── install.sh              # 离线安装脚本（随包发布）
│   ├── build-base.sh           # 基础镜像构建脚本
│   └── start.sh                # 容器内启动脚本
├── chromedriver/               # ChromeDriver 驱动（按系统分目录）
├── src/main/                   # 应用源码
├── .env.example                # 配置参考模板（部署前必读）
├── data/                       # 数据目录
└── pom.xml
```

---

## 部署方案

提供两种部署方式，根据服务器网络情况选择：

| 方案 | 适用场景 | 服务器要求 |
| --- | --- | --- |
| **方案 A: 在线部署** | 服务器可联网，有源码和 Maven | Docker + Maven + 外网 |
| **方案 B: 离线部署** | 服务器无外网，仅有 Docker | 仅需 Docker |

---

## 方案 A: 在线部署（服务器有网）

### 环境要求

| 项目     | 要求                  |
| -------- | --------------------- |
| 操作系统 | Ubuntu 20.04 (x86_64)  |
| Docker   | 20.10+                 |
| Maven    | 3.8+                   |
| 网络     | 首次构建需要外网       |

### 部署架构

采用 **基础镜像 + 卷挂载** 方式，实现一次构建镜像、后续仅更新程序包：

```
┌─────────────────────────────────────────┐
│             Docker 容器                  │
│  基础镜像: sgcc/base:java21             │
│  (Ubuntu + JDK 21 + Chrome + 字体)      │
│                                          │
│  /app  ← ─ ─ ─ 卷挂载 ─ ─ ─ ┐          │
│    ├── app.jar                │          │
│    ├── lib/                   │          │
│    ├── config/                │          │
│    ├── drivers/               │          │
│    ├── start.sh               │          │
│    ├── logs/          /data/sgcc-crawler │
│    └── data/          (宿主机目录)       │
└─────────────────────────────────────────┘
```

> **优势**: 基础镜像仅构建一次（~1GB），后续升级只需替换 app.jar + lib/，无需重新传输镜像。

---

### 一、首次部署

#### 1. 上传项目到服务器

```bash
# 将项目代码上传到服务器
scp -r . user@server:/opt/sgcc-crawler
ssh user@server
cd /opt/sgcc-crawler
```

#### 2. 一键部署

```bash
chmod +x docker/deploy.sh docker/build-base.sh docker/start.sh
./docker/deploy.sh init
```

`init` 命令自动执行以下步骤：
1. 构建基础镜像 `sgcc/base:java21`（首次约 10-15 分钟）
2. Maven 编译打包
3. 部署文件到 `/data/sgcc-crawler`（app.jar + lib/ + drivers/）
4. **初始化配置** — 复制配置模板到 `/data/sgcc-crawler/config/`，提示修改
5. 启动容器

> **重要**: init 过程中会暂停并提示修改配置文件。请参考 `.env.example` 中的说明，修改 `config/application.yml` 和 `config/application-prod.yml` 中的数据库、钉钉等配置。

#### 3. 配置文件说明

首次部署时，脚本会将配置模板复制到部署目录：

```
/data/sgcc-crawler/config/
├── application.yml         # ← 修改数据库连接、爬虫参数等
├── application-prod.yml    # ← 修改生产环境覆盖配置
└── .env.example            # ← 配置项参考说明（只读）
```

必须修改的配置项：
- `spring.datasource.url` — 数据库地址
- `spring.datasource.username / password` — 数据库账号密码
- `dingtalk.webhook / secret` — 钉钉机器人配置
- `crawler.headless` — 生产环境建议设为 `true`

> 配置修改后无需重新编译，重启容器即可生效：`./docker/deploy.sh restart`

#### 4. 验证部署

```bash
# 查看状态
./docker/deploy.sh status

# 查看应用日志
./docker/deploy.sh logs

# 健康检查
curl http://localhost:8080/api/actuator/health
```

---

### 二、应用升级

当代码有更新时，**无需重新构建镜像**，只需：

#### 方式一：服务器有源码（推荐）

```bash
cd /opt/sgcc-crawler
git pull                      # 拉取最新代码
./docker/deploy.sh upgrade    # 自动编译 + 替换 + 重启
```

`upgrade` 命令自动执行：
1. Maven 编译打包
2. 停止容器
3. 替换 app.jar + lib/ 依赖库 + drivers/（**保留 config/、logs/、data/**）
4. 重启容器

> 升级时 lib/ 目录会整体替换，确保依赖版本与新代码一致，避免依赖缺失问题。

#### 方式二：仅传输程序包

在开发机编译后，传输程序包到服务器：

```bash
# 开发机 - 编译并打包
mvn clean package -DskipTests

# 传输 JAR 包和依赖库
scp target/sgcc-bidding-crawler-1.0.0.jar user@server:/data/sgcc-crawler/app.jar
scp -r target/lib user@server:/data/sgcc-crawler/lib

# 服务器 - 重启容器
./docker/deploy.sh restart
```

> **注意**: 升级时必须同时替换 app.jar 和 lib/ 目录，以确保依赖完整。

---

### 三、日常运维

```bash
./docker/deploy.sh <命令>
```

| 命令      | 说明                           |
| --------- | ------------------------------ |
| `init`    | 首次部署（构建镜像+编译+启动） |
| `build`   | 仅编译打包                     |
| `upgrade` | 升级应用（编译+替换+重启）     |
| `pack`          | 打包离线安装包（含镜像）   |
| `pack-upgrade`  | 打包升级包（不含镜像）   |
| `start`   | 启动容器                       |
| `stop`    | 停止容器                       |
| `restart` | 重启容器                       |
| `status`  | 查看运行状态                   |
| `logs`    | 查看应用日志（Ctrl+C 退出）    |
| `shell`   | 进入容器终端                   |

#### 常用排查命令

```bash
# 进入容器
./docker/deploy.sh shell

# 在容器内查看进程
ps aux | grep java

# 在容器内查看日志
tail -100f /app/logs/startup.log

# 查看容器日志（包含启动信息）
docker logs sgcc-crawler
```

---

### 四、配置说明

#### 配置文件位置

| 位置 | 说明 |
| --- | --- |
| `.env.example` (项目根目录) | 配置参考模板，包含所有可配置项说明 |
| `/data/sgcc-crawler/config/application.yml` | 实际生效的主配置 |
| `/data/sgcc-crawler/config/application-prod.yml` | 实际生效的生产环境配置 |

> 配置优先级: `config/application-prod.yml` > `config/application.yml` > JAR 内嵌配置

修改配置后重启即可生效：`./docker/deploy.sh restart`

#### 部署参数

deploy.sh 顶部可修改以下配置：

```bash
DEPLOY_DIR="/data/sgcc-crawler"    # 部署目录
CONTAINER_NAME="sgcc-crawler"      # 容器名
BASE_IMAGE="sgcc/base:java21"      # 基础镜像
PORT="8080"                        # 端口
```

#### 应用配置

关键配置项：
- 数据库连接：`spring.datasource.*`
- 钉钉通知：`dingtalk.*`
- 爬虫参数：`crawler.*`（无头模式、间隔、页数等）

---

### 五、服务器目录结构

```
/data/sgcc-crawler/              # 部署目录（挂载为容器 /app）
├── app.jar                      # 应用 JAR 包         ← 升级时替换
├── start.sh                     # 启动脚本           ← 升级时替换
├── lib/                         # 依赖库            ← 升级时替换
├── config/                      # 配置文件           ← 升级时保留
│   ├── application.yml
│   ├── application-prod.yml
│   └── .env.example
├── drivers/                     # ChromeDriver       ← 升级时替换
│   └── linux/chromedriver-linux64/chromedriver
├── logs/                        # 运行日志           ← 升级时保留
│   └── startup.log
└── data/                        # 数据文件           ← 升级时保留
```

---

### 六、基础镜像说明

基础镜像 `sgcc/base:java21` 包含：

- Ubuntu 20.04 LTS
- OpenJDK 21
- Google Chrome（最新稳定版）
- 中文字体（文泉驿正黑、微米黑）
- 基础工具（vim、curl、net-tools 等）

如需重新构建基础镜像（如升级 Chrome 版本）：

```bash
./docker/build-base.sh
./docker/deploy.sh restart
```

> Chrome 大版本更新时，需同步更新 `chromedriver/` 目录下的驱动文件。

---

## 方案 B: 离线部署（服务器无网）

适用于目标服务器无法访问外网的场景。在有网的开发机上打包，将安装包传输到服务器直接部署。

### 环境要求

| 环境 | 要求 |
| --- | --- |
| 开发机（打包用） | Docker + Maven + 外网 |
| 服务器（运行用） | 仅需 Docker 20.10+ |

### 一、打包安装包（开发机执行）

```bash
cd /path/to/sgcc-crawler
./docker/deploy.sh pack
```

输出产物: `target/sgcc-crawler-v1.0.0.tar.gz`

安装包内容:
```
sgcc-crawler-v1.0.0/
├── install.sh              # 安装脚本
├── image/                  # 基础镜像（~1GB）
│   └── sgcc-base-java21.tar.gz
├── app/                    # 应用程序
│   ├── app.jar
│   ├── lib/
│   ├── drivers/
│   └── start.sh
└── config/                 # 配置模板（安装前修改）
    ├── application.yml
    ├── application-prod.yml
    └── .env.example
```

### 二、首次安装（服务器执行）

```bash
# 1. 传输安装包到服务器
scp target/sgcc-crawler-v1.0.0.tar.gz user@server:/tmp/

# 2. 登录服务器并解压
ssh user@server
cd /tmp
tar xzf sgcc-crawler-v1.0.0.tar.gz
cd sgcc-crawler-v1.0.0

# 3. 修改配置（重要！安装前必须完成）
vi config/application.yml        # 修改数据库、爬虫等配置
vi config/application-prod.yml   # 修改生产环境配置

# 4. 一键安装
./install.sh
```

安装脚本自动执行:
1. 加载基础镜像 `sgcc/base:java21`（从 tar.gz 导入，无需网络）
2. 部署文件到 `/data/sgcc-crawler`
3. 复制配置文件
4. 启动容器

### 三、应用升级（无需镜像）

升级时只需传输升级包，不含镜像，体积小很多：

```bash
# 开发机 - 打包升级包
./docker/deploy.sh pack-upgrade

# 传输到服务器
scp target/sgcc-crawler-upgrade-v1.0.0.tar.gz user@server:/tmp/
```

```bash
# 服务器 - 执行升级
cd /tmp
tar xzf sgcc-crawler-upgrade-v1.0.0.tar.gz
cd sgcc-crawler-upgrade-v1.0.0
./install.sh upgrade
```

升级操作:
- 替换: app.jar + lib/ + drivers/ + start.sh
- 保留: config/ + logs/ + data/

### 四、离线环境日常运维

安装完成后，可使用 `install.sh` 进行日常运维：

```bash
./install.sh status     # 查看状态
./install.sh logs       # 查看日志
./install.sh restart    # 重启服务
./install.sh shell      # 进入容器
./install.sh stop       # 停止服务
```

> **提示**: 建议将 `install.sh` 复制到服务器固定位置（如 `/opt/sgcc-crawler/install.sh`），方便日常使用。

### 安装包对比

| 包类型 | 内容 | 约体积 | 用途 |
| --- | --- | --- | --- |
| 安装包 `pack` | 镜像 + 程序 + 配置 | ~1.2GB | 首次部署 |
| 升级包 `pack-upgrade` | 仅程序 | ~60MB | 应用升级 |
