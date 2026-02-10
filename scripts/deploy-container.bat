@echo off
REM 国网招标爬虫 - 容器化部署脚本 (Windows)
REM 使用方法: scripts\deploy-container.bat [操作] [参数]
REM 操作: build|up|down|restart|logs|status

@echo ================================================
@echo   国网招标爬虫 - 容器部署  
@echo ================================================

REM 设置变量
set ACTION=%1
if "%ACTION%"=="" set ACTION=up
set TAG=%2
if "%TAG%"=="" set TAG=latest

REM 获取目录
set PROJECT_ROOT=%~dp0..
set DOCKER_DIR=%PROJECT_ROOT%\docker
set SCRIPTS_DIR=%PROJECT_ROOT%\scripts

REM 检查 Docker 环境
call :check_dependencies
if errorlevel 1 exit /b 1

REM 根据操作执行相应功能
goto %ACTION%

:build
    call :build_image
    goto :eof

:up
    call :build_image
    call :start_service
    goto :eof

:down
    call :stop_service
    goto :eof

:restart
    call :stop_service
    timeout /t 2 /nobreak >nul
    call :start_service
    goto :eof

:logs
    call :show_logs
    goto :eof

:status
    call :show_status
    goto :eof

:help
    call :show_help
    goto :eof

:check_dependencies
    @echo 检查依赖环境...
    
    docker --version >nul 2>&1
    if errorlevel 1 (
        @echo 错误: 未安装 Docker
        exit /b 1
    )
    
    docker-compose --version >nul 2>&1
    if errorlevel 1 (
        @echo 错误: 未安装 Docker Compose
        exit /b 1
    )
    
    @echo Docker 环境检查通过
    goto :eof

:build_image
    @echo 构建 Docker 镜像...
    call "%SCRIPTS_DIR%\build-container.bat" "%TAG%"
    goto :eof

:start_service
    @echo 启动服务...
    
    REM 检查 .env 文件
    if not exist "%PROJECT_ROOT%\.env" (
        @echo 警告: 未找到 .env 文件，复制 .env.example
        copy "%PROJECT_ROOT%\.env.example" "%PROJECT_ROOT%\.env"
        @echo 已创建 .env 文件，请根据实际情况修改配置
    )
    
    REM 创建数据目录
    for /f "tokens=2 delims==" %%i in ('findstr "^DATA_VOLUME=" "%PROJECT_ROOT%\.env"') do set DATA_VOLUME=%%i
    if defined DATA_VOLUME (
        if not exist "%DATA_VOLUME%" (
            @echo 创建数据目录: %DATA_VOLUME%
            mkdir "%DATA_VOLUME%"
        )
    )
    
    REM 启动容器
    cd /d "%DOCKER_DIR%"
    docker-compose up -d
    
    @echo 服务启动完成！
    goto :eof

:stop_service
    @echo 停止服务...
    cd /d "%DOCKER_DIR%"
    docker-compose down
    @echo 服务已停止
    goto :eof

:show_logs
    @echo 查看容器日志...
    cd /d "%DOCKER_DIR%"
    docker-compose logs -f
    goto :eof

:show_status
    @echo 查看服务状态...
    cd /d "%DOCKER_DIR%"
    docker-compose ps
    goto :eof

:show_help
    @echo 使用方法: %0 [操作] [参数]
    @echo.
    @echo 操作:
    @echo   build     构建 Docker 镜像
    @echo   up        启动服务（默认）
    @echo   down      停止服务
    @echo   restart   重启服务
    @echo   logs      查看日志
    @echo   status    查看状态
    @echo   help      显示帮助
    @echo.
    @echo 示例:
    @echo   %0 build          # 构建镜像
    @echo   %0 up             # 启动服务
    @echo   %0 up v1.0.0      # 使用指定标签启动
    @echo   %0 logs           # 实时查看日志
    goto :eof

:eof
@echo ================================================
@echo 操作完成！
@echo ================================================