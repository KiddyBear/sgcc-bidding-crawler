@echo off
chcp 65001 >nul
REM Windows部署脚本

echo ===== 国网招标爬虫部署脚本 =====

REM 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
set DOCKER_DIR=%PROJECT_DIR%\docker

cd /d %DOCKER_DIR%

REM 检查.env文件
if not exist ".env" (
    echo 警告: .env 文件不存在，将从模板创建...
    if exist ".env.example" (
        copy .env.example .env
        echo 请编辑 %DOCKER_DIR%\.env 文件，配置钉钉Webhook等参数
    )
)

REM 解析命令行参数
set ACTION=%1
if "%ACTION%"=="" set ACTION=up

if "%ACTION%"=="up" goto :up
if "%ACTION%"=="down" goto :down
if "%ACTION%"=="restart" goto :restart
if "%ACTION%"=="logs" goto :logs
if "%ACTION%"=="status" goto :status
if "%ACTION%"=="rebuild" goto :rebuild
goto :usage

:up
echo 启动服务...
docker-compose up -d --build
echo 等待服务启动...
timeout /t 10 /nobreak >nul
docker-compose ps
echo ===== 服务已启动 =====
echo 访问地址: http://localhost:8080
echo 健康检查: http://localhost:8080/api/health
goto :end

:down
echo 停止服务...
docker-compose down
echo ===== 服务已停止 =====
goto :end

:restart
echo 重启服务...
docker-compose restart
echo ===== 服务已重启 =====
goto :end

:logs
echo 查看日志...
docker-compose logs -f --tail=100
goto :end

:status
echo 服务状态...
docker-compose ps
goto :end

:rebuild
echo 重新构建并启动...
docker-compose down
docker-compose build --no-cache
docker-compose up -d
echo ===== 服务已重建并启动 =====
goto :end

:usage
echo 用法: %0 {up^|down^|restart^|logs^|status^|rebuild}
echo   up      - 启动服务
echo   down    - 停止服务
echo   restart - 重启服务
echo   logs    - 查看日志
echo   status  - 查看状态
echo   rebuild - 重新构建并启动
goto :end

:end
pause
