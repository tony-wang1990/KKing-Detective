#!/bin/bash

echo "=== King-Detective 安装脚本 ==="
echo "步骤 1: 检查环境..."

# 检查必要的命令
command -v wget >/dev/null 2>&1 || { echo "错误: 未安装 wget"; exit 1; }
command -v docker >/dev/null 2>&1 || { echo "错误: 未安装 docker"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "错误: 未安装 docker-compose"; exit 1; }

echo "步骤 2: 创建目录..."
mkdir -p /app/king-detective/keys || { echo "错误: 无法创建目录"; exit 1; }
cd /app/king-detective || { echo "错误: 无法进入目录"; exit 1; }

echo "步骤 3: 下载配置文件..."
wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose.yml || { echo "错误: 下载 docker-compose.yml 失败"; exit 1; }
echo "  - docker-compose.yml 下载成功"

wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml || { echo "错误: 下载 application.yml 失败"; exit 1; }
echo "  - application.yml 下载成功"

wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/king-detective.db || { echo "错误: 下载 king-detective.db 失败"; exit 1; }
echo "  - king-detective.db 下载成功"

echo "步骤 4: 启动服务..."
docker-compose up -d || { echo "错误: 启动服务失败"; exit 1; }

echo ""
echo "=== 安装完成！ ==="
echo "访问地址: http://$(curl -s ifconfig.me):9527"
echo "默认账号: admin / admin123456"