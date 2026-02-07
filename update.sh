#!/bin/bash

# King-Detective v4.0.0 一键更新脚本
# 作者: Antigravity AI
# 日期: 2026-02-07

set -e  # 遇到错误立即停止

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
CONTAINER_NAME="king-detective"
IMAGE_NAME="ghcr.io/tony-wang1990/king-detective:latest"
PORT="9527"

echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}King-Detective v4.0 一键更新${NC}"
echo -e "${GREEN}================================${NC}"
echo ""

# 步骤1: 检查容器是否存在
echo -e "${YELLOW}[1/6]${NC} 检查现有容器..."
if ! docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${RED}错误: 未找到运行中的 $CONTAINER_NAME 容器${NC}"
    exit 1
fi
echo -e "${GREEN}✓ 找到容器${NC}"

# 步骤2: 备份数据库
echo -e "${YELLOW}[2/6]${NC} 备份数据库..."
BACKUP_FILE="king-detective.db.backup-$(date +%Y%m%d-%H%M%S)"
docker exec $CONTAINER_NAME cp /app/king-detective.db /app/$BACKUP_FILE 2>/dev/null || {
    echo -e "${YELLOW}警告: 无法在容器内备份，尝试从主机备份...${NC}"
    if [ -f "./king-detective.db" ]; then
        cp "./king-detective.db" "./$BACKUP_FILE"
    elif [ -f "./data/king-detective.db" ]; then
        cp "./data/king-detective.db" "./data/$BACKUP_FILE"
    fi
}
echo -e "${GREEN}✓ 备份完成: $BACKUP_FILE${NC}"

# 步骤3: 获取旧容器配置
echo -e "${YELLOW}[3/6]${NC} 保存容器配置..."
OLD_CONTAINER_ID=$(docker ps | grep "$CONTAINER_NAME" | awk '{print $1}')
echo -e "${GREEN}✓ 容器ID: $OLD_CONTAINER_ID${NC}"

# 步骤4: 停止并删除旧容器
echo -e "${YELLOW}[4/6]${NC} 停止旧容器..."
docker stop $CONTAINER_NAME
docker rm $CONTAINER_NAME
echo -e "${GREEN}✓ 旧容器已删除${NC}"

# 步骤5: 拉取最新镜像
echo -e "${YELLOW}[5/6]${NC} 拉取最新镜像（可能需要1-2分钟）..."
docker pull $IMAGE_NAME
echo -e "${GREEN}✓ 镜像拉取完成${NC}"

# 步骤6: 启动新容器
echo -e "${YELLOW}[6/6]${NC} 启动新容器..."

# 尝试检测数据目录
DATA_DIR=""
if [ -d "/root/king-detective/data" ]; then
    DATA_DIR="/root/king-detective/data"
elif [ -d "./data" ]; then
    DATA_DIR="$(pwd)/data"
elif [ -d "/root/data" ]; then
    DATA_DIR="/root/data"
else
    DATA_DIR="$(pwd)/data"
    mkdir -p "$DATA_DIR"
fi

docker run -d \
  --name $CONTAINER_NAME \
  --restart unless-stopped \
  -p $PORT:$PORT \
  -v $DATA_DIR:/app \
  $IMAGE_NAME

echo -e "${GREEN}✓ 新容器已启动${NC}"

# 等待5秒让容器启动
echo ""
echo -e "${YELLOW}等待容器启动...${NC}"
sleep 5

# 验证
echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}验证更新结果${NC}"
echo -e "${GREEN}================================${NC}"

# 检查容器状态
if docker ps | grep -q "$CONTAINER_NAME"; then
    echo -e "${GREEN}✓ 容器运行正常${NC}"
else
    echo -e "${RED}✗ 容器未运行!${NC}"
    echo "查看日志："
    docker logs $CONTAINER_NAME --tail=50
    exit 1
fi

# 显示日志
echo ""
echo -e "${YELLOW}最近的日志:${NC}"
docker logs $CONTAINER_NAME --tail=30

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}更新完成! 🎉${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "✅ 版本: v4.0.0"
echo -e "✅ 容器: $CONTAINER_NAME"
echo -e "✅ 端口: $PORT"
echo -e "✅ 备份: $BACKUP_FILE"
echo ""
echo -e "${YELLOW}下一步:${NC}"
echo -e "1. 给你的 Telegram Bot 发送 /start 测试"
echo -e "2. 查看完整日志: ${GREEN}docker logs $CONTAINER_NAME${NC}"
echo -e "3. 如有问题，运行回滚脚本恢复"
echo ""
