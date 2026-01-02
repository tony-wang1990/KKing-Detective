#!/bin/bash
set -e

echo "正在安装 King-Detective..."

# 创建目录
mkdir -p /app/king-detective/keys
cd /app/king-detective

# 下载配置文件
wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose.yml
wget -q https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml

# 启动服务
docker-compose up -d

echo "安装完成！"
echo "访问地址: http://$(curl -s ifconfig.me):9527"
echo "默认账号: admin / admin123456"
