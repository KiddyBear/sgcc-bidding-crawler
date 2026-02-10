@echo off
REM 国网招标爬虫 - Docker 镜像构建脚本 (Windows)
REM 使用方法: scripts\build-container.bat [tag]

@echo ================================================
@echo   国网招标爬虫 - Docker 构建  
@echo ================================================

REM 设置默认标签
set TAG=%1
if "%TAG%"=="" set TAG=latest

REM 获取项目根目录
set PROJECT_ROOT=%~dp0..
set DOCKER_DIR=%PROJECT_ROOT%\docker

@echo 检查必要文件...

REM 检查 pom.xml
if not exist "%PROJECT_ROOT%\pom.xml" (
    @echo 错误: 找不到 pom.xml 文件
    exit /b 1
)

REM 检查 Dockerfile
if not exist "%DOCKER_DIR%\Dockerfile" (
    @echo 错误: 找不到 Dockerfile
    exit /b 1
)

REM 检查 jar 包
if not exist "%PROJECT_ROOT%\target\sgcc-bidding-crawler-1.0.0.jar" (
    @echo 警告: 找不到编译后的 jar 包，正在执行 Maven 构建...
    
    REM 切换到项目根目录执行 Maven 构建
    cd /d "%PROJECT_ROOT%"
    call mvn clean package -DskipTests
    
    if not exist "%PROJECT_ROOT%\target\sgcc-bidding-crawler-1.0.0.jar" (
        @echo 错误: Maven 构建失败，无法找到 jar 包
        exit /b 1
    )
    
    @echo Maven 构建完成
)

REM 构建 Docker 镜像
@echo 开始构建 Docker 镜像...
@echo 镜像标签: sgcc/crawler:%TAG%

cd /d "%DOCKER_DIR%"

REM 执行 Docker 构建
docker build -t "sgcc/crawler:%TAG%" .

if %ERRORLEVEL% equ 0 (
    @echo Docker 镜像构建成功！
    @echo 镜像信息:
    docker images | findstr "sgcc/crawler"
) else (
    @echo Docker 镜像构建失败！
    exit /b 1
)

@echo ================================================
@echo 构建完成！
@echo ================================================