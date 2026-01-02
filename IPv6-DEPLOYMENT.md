# King-Detective IPv6 部署指南

本指南专门用于在支持 IPv6 的免费服务器（如 frog.fm、Oracle Cloud 等）上部署 King-Detective。

##  前置要求

1. 一台支持 IPv6 的服务器
2. 已安装 Docker 和 Docker Compose
3. 服务器已启用 IPv6 网络

##  验证 IPv6 支持

在部署前，请确认服务器支持 IPv6：

```bash
# 检查 IPv6 是否启用
ip -6 addr show

# 测试 IPv6 连接
ping6 google.com

# 检查 Docker 的 IPv6 支持
docker network inspect bridge | grep IPv6
```

##  快速部署（IPv6 环境）

### 步骤 1: 创建工作目录

```bash
mkdir -p /app/king-detective/keys
cd /app/king-detective
```

### 步骤 2: 下载配置文件

```bash
# 下载 IPv6 专用的 docker-compose 文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -O docker-compose.yml

# 下载默认配置文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml

# 下载数据库文件（可选，首次部署会自动创建）
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/king-detective.db
```

### 步骤 3: 配置 IPv6 网络

确保 Docker daemon 支持 IPv6：

```bash
# 编辑 Docker daemon 配置
sudo nano /etc/docker/daemon.json
```

添加以下内容：

```json
{
  "ipv6": true,
  "fixed-cidr-v6": "fd00::/64",
  "experimental": true,
  "ip6tables": true
}
```

重启 Docker 服务：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 步骤 4: 修改配置文件

编辑 pplication.yml，配置您的管理员账号和其他设置：

```bash
nano application.yml
```

重要配置项：

```yaml
# 修改默认账号密码
web:
  account: your_username      # 修改为您的用户名
  password: your_password     # 修改为您的密码

# OCI 密钥文件目录（保持默认即可）
oci-cfg:
  key-dir-path: /app/king-detective/keys

# AI 功能（可选）
spring:
  ai:
    openai:
      api-key: your_api_key   # 如需使用 AI，填入硅基流动 API Key
      base-url: https://api.siliconflow.cn
```

### 步骤 5: 启动服务

```bash
# 启动容器
docker-compose up -d

# 查看日志
docker-compose logs -f king-detective
```

### 步骤 6: 验证 IPv6 访问

```bash
# 获取服务器的 IPv6 地址
ip -6 addr show | grep inet6 | grep global

# 示例输出：
# inet6 2001:db8::1/64 scope global
```

然后在浏览器中访问：
- IPv6: http://[2001:db8::1]:8818
- 如果有域名并配置了 AAAA 记录: http://your-domain.com:8818

##  配置 OCI 使用 IPv6

King-Detective 完全支持通过 IPv6 连接到 Oracle Cloud Infrastructure API。

### OCI API 端点支持

OCI 的大多数 API 端点都支持 IPv6 访问。确保您的网络配置正确：

1. **添加租户配置时**，OCI API 会自动使用服务器的网络协议
2. **IPv6 优先**：应用程序已配置优先使用 IPv6（如果可用）
3. **自动回退**：如果 IPv6 不可用，会自动使用 IPv4

### 在 King-Detective 中添加 OCI 租户

1. 访问 Web 界面: http://[your-ipv6]:8818
2. 使用您配置的账号密码登录
3. 导航到 "租户管理"  "添加租户"
4. 填写以下信息：

```
租户名称: 您的租户名称（自定义）
用户 OCID: ocid1.user.oc1..aaaa...
租户 OCID: ocid1.tenancy.oc1..aaaa...
区域: us-ashburn-1（或您的区域）
指纹: aa:bb:cc:dd:...
私钥文件: 上传您的 .pem 私钥文件
```

5. 点击"测试连接"验证配置
6. 保存配置

### IPv6 网络优化

如果您的服务器只有 IPv6 而没有 IPv4，请确保：

```yaml
# 在 application.yml 中已配置
server:
  address: '::'  # 监听所有 IPv6 地址
```

Java 应用程序会自动使用 IPv6 网络栈。

##  防火墙配置

### UFW 防火墙（推荐用于 IPv6）

```bash
# 允许 IPv6 流量
sudo ufw allow 8818/tcp comment 'King-Detective Web'
sudo ufw allow 6080/tcp comment 'King-Detective VNC'

# 查看规则
sudo ufw status

# 确保 IPv6 已启用
sudo nano /etc/default/ufw
# 设置 IPV6=yes
```

### iptables IPv6 规则

```bash
# 允许入站连接
sudo ip6tables -A INPUT -p tcp --dport 8818 -j ACCEPT
sudo ip6tables -A INPUT -p tcp --dport 6080 -j ACCEPT

# 保存规则
sudo ip6tables-save > /etc/iptables/rules.v6
```

##  配置 DNS AAAA 记录

如果您有域名，配置 AAAA 记录指向服务器的 IPv6 地址：

```
类型: AAAA
名称: king-detective (或 @)
值: 2001:db8::1 (您的 IPv6 地址)
TTL: 3600
```

##  故障排查

### 问题 1: 无法通过 IPv6 访问

```bash
# 检查端口是否监听 IPv6
sudo netstat -tlnp | grep 8818

# 应该看到类似：
# tcp6  0  0 :::8818  :::*  LISTEN

# 检查 Docker 容器的 IPv6 地址
docker inspect king-detective | grep IPv6Address
```

### 问题 2: OCI API 连接失败

```bash
# 测试到 OCI API 的 IPv6 连接
curl -6 -I https://iaas.us-ashburn-1.oraclecloud.com

# 检查 DNS 解析
dig AAAA iaas.us-ashburn-1.oraclecloud.com
```

### 问题 3: Docker 网络问题

```bash
# 重新创建网络
docker-compose down
docker network prune
docker-compose up -d

# 查看网络详情
docker network inspect king-detective-network
```

##  性能优化

### IPv6 MTU 设置

```bash
# 检查当前 MTU
ip -6 link show

# 设置最佳 MTU（通常 1500）
sudo ip link set dev eth0 mtu 1500
```

### Java 内存优化（针对免费服务器）

如果您的服务器内存有限（如 1GB），可以优化 Java 内存使用：

编辑 docker-compose.yml，在 environment 部分添加：

```yaml
environment:
  - JAVA_OPTS=-Xmx512m -Xms256m -Djava.net.preferIPv6Addresses=true
```

##  更新应用

```bash
cd /app/king-detective

# 拉取最新镜像
docker-compose pull

# 重启服务
docker-compose up -d
```

##  常用命令

```bash
# 查看日志
docker-compose logs -f

# 停止服务
docker-compose stop

# 重启服务
docker-compose restart

# 查看资源使用
docker stats king-detective

# 备份数据库
cp king-detective.db king-detective.db.backup

# 进入容器
docker exec -it king-detective /bin/bash
```

##  安全建议

1. **更改默认密码**：首次登录后立即修改
2. **使用 HTTPS**：配置 Nginx 反向代理 + Let's Encrypt
3. **限制访问**：使用防火墙限制来源 IP
4. **定期备份**：定期备份数据库文件
5. **监控日志**：定期检查异常登录尝试

##  免费 IPv6 服务器推荐

- **Oracle Cloud**: 永久免费套餐，支持 IPv6
- **Google Cloud**: 免费试用，IPv6 支持良好
- **frog.fm**: 免费 VPS，原生 IPv6
- **Euserv**: 免费 VPS，德国，IPv6
- **Azure**: 学生套餐，IPv6 支持

##  获取帮助

如果遇到问题：
1. 查看日志: docker-compose logs -f
2. 检查 GitHub Issues
3. 参考原项目文档

---

**King-Detective (W探长)** - 让 OCI 管理更简单！
