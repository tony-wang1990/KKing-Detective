#!/bin/bash
#
# King-Detective IPv6 快速安装脚本
# 适用于支持 IPv6 的服务器
#

set -e

echo "========================================"
echo " King-Detective (W探长) IPv6 部署安装"
echo "========================================"
echo ""

# 检查是否为 root
if [ "$EUID" -ne 0 ]; then
   echo "请使用 root 权限运行此脚本"
   echo "使用: sudo bash install-ipv6.sh"
   exit 1
fi

# 检查 Docker
echo "检查 Docker..."
if ! command -v docker &gt; /dev/null; then
    echo "Docker 未安装，正在安装..."
    curl -fsSL https://get.docker.com | sh
    systemctl start docker
    systemctl enable docker
fi

# 检查 Docker Compose
echo "检查 Docker Compose..."
if ! command -v docker-compose &gt; /dev/null; then
    echo "Docker Compose 未安装，正在安装..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
fi

# 检查网络环境
echo ""
echo "检查网络环境..."
HAS_IPV4=0
HAS_IPV6=0

if curl -4 -s --max-time 3 ifconfig.me &gt; /dev/null 2&gt;&1; then
    HAS_IPV4=1
    IPV4_ADDR=$(curl -4 -s ifconfig.me)
    echo " 检测到 IPv4: $IPV4_ADDR"
fi

if curl -6 -s --max-time 3 ifconfig.me &gt; /dev/null 2&gt;&1; then
    HAS_IPV6=1
    IPV6_ADDR=$(curl -6 -s ifconfig.me)
    echo " 检测到 IPv6: $IPV6_ADDR"
fi

# 测试 NAT64/DNS64
if [ $HAS_IPV6 -eq 1 ] && [ $HAS_IPV4 -eq 0 ]; then
    echo ""
    echo "检测到纯 IPv6 环境，测试 NAT64/DNS64..."
    if curl -6 -s --max-time 5 http://www.google.com &gt; /dev/null 2&gt;&1; then
        echo " NAT64/DNS64 可用，可以访问 IPv4 资源"
    else
        echo " 警告: NAT64/DNS64 不可用"
        echo "建议配置 DNS64 解析器:"
        echo "  nameserver 2001:4860:4860::6464"
        echo ""
        read -p "是否自动配置 Google DNS64? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "nameserver 2001:4860:4860::6464" &gt; /etc/resolv.conf
            echo "nameserver 2001:4860:4860::64" &gt;&gt; /etc/resolv.conf
            echo " DNS64 已配置"
        fi
    fi
fi

# 创建工作目录
echo ""
echo "创建工作目录..."
mkdir -p /app/king-detective/keys
cd /app/king-detective

# 下载配置文件
echo "下载配置文件..."
curl -fsSL https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -o docker-compose.yml
curl -fsSL https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml -o application.yml

# 配置 Docker IPv6
echo "配置 Docker IPv6 支持..."
cat &gt; /etc/docker/daemon.json &lt;&lt; 'EOF'
{
  "ipv6": true,
  "fixed-cidr-v6": "fd00::/64",
  "experimental": true,
  "ip6tables": true
}
EOF

systemctl restart docker
sleep 3

# 提示用户配置
echo ""
echo "========================================"
echo " 配置管理员账号密码"
echo "========================================"
echo ""
read -p "请输入管理员用户名 [admin]: " ADMIN_USER
ADMIN_USER=${ADMIN_USER:-admin}

read -s -p "请输入管理员密码 [admin123456]: " ADMIN_PASS
ADMIN_PASS=${ADMIN_PASS:-admin123456}
echo ""

# 修改配置文件
sed -i "s/account: admin/account: $ADMIN_USER/" application.yml
sed -i "s/password: admin123456/password: $ADMIN_PASS/" application.yml

# 启动服务
echo ""
echo "启动 King-Detective..."
docker-compose up -d

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 显示访问信息
echo ""
echo "========================================"
echo "  安装完成！"
echo "========================================"
echo ""
echo "访问地址:"
if [ $HAS_IPV4 -eq 1 ]; then
    echo "  IPv4: http://$IPV4_ADDR:9527"
fi
if [ $HAS_IPV6 -eq 1 ]; then
    echo "  IPv6: http://[$IPV6_ADDR]:9527"
fi
echo ""
echo "管理员账号:"
echo "  用户名: $ADMIN_USER"
echo "  密码: $ADMIN_PASS"
echo ""
echo "常用命令:"
echo "  查看日志: docker-compose logs -f"
echo "  重启服务: docker-compose restart"
echo "  停止服务: docker-compose stop"
echo ""
echo "密钥文件存放目录: /app/king-detective/keys"
echo ""
echo "详细文档: https://github.com/tony-wang1990/King-Detective"
echo "========================================"
