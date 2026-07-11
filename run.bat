@echo off
chcp 65001 > nul
echo ==============================================
echo           教学档案管理系统 一键启动
echo ==============================================
echo.

echo 正在启动 后端服务 (端口 8080)...
start "后端服务" cmd /k "cd /d %~dp0backend && java -jar target/archive-system-1.0.0.jar"

echo 等待 3 秒确保后端启动...
ping 127.0.0.1 -n 4 >nul

echo 正在启动 前端服务 (端口 5175)...
start "前端服务" cmd /k "cd /d %~dp0frontend && npm run dev"

echo 等待 3 秒后自动打开登录页...
ping 127.0.0.1 -n 4 >nul
start "" "http://localhost:5175/login"

echo.
echo ==============================================
echo      启动完成！前后端服务已全部运行！
echo      浏览器访问：http://localhost:5175/login
echo      账号：admin   密码：123456
echo ==============================================
pause
