@echo off
chcp 65001 >nul
REM Windows编译脚本

echo ===== 开始编译项目 =====

REM 获取脚本所在目录
set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..

cd /d %PROJECT_DIR%

echo 项目目录: %PROJECT_DIR%

REM 清理并编译
echo 执行 Maven 编译...
call mvn clean package -DskipTests -B

if %ERRORLEVEL% NEQ 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo ===== 编译完成 =====
echo JAR包位置: %PROJECT_DIR%\target\
dir /b target\*.jar 2>nul || echo 未找到JAR文件

pause
