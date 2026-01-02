# King-Detective IPv6 部署完整指南

##  OCI SDK 与 IPv6 兼容性说明

###  支持的部分
- **OCI Java SDK**: 支持通过 IPv6 网络进行通信
- **OCI 服务**: VCN、实例、API Gateway 等都支持 IPv6
- **应用程序**: King-Detective 完全支持在 IPv6 环境中运行

###  重要限制
**OCI 控制平面 API 端点当前主要提供 IPv4 访问**

- API 端点（如 `iaas.us-ashburn-1.oraclecloud.com`）主要使用 IPv4
- 在**纯 IPv6 环境**下，需要额外配置才能访问 OCI API

---

##  部署场景与解决方案

### 场景 1: IPv4/IPv6 双栈服务器（推荐）

**适用于**: 大多数云服务器（Oracle Cloud、Google Cloud、AWS 等）

这是**最简单**的部署方式，无需额外配置。

#### 部署步骤

```bash
# 1. 创建工作目录
mkdir -p /app/king-detective/keys
cd /app/king-detective

# 2. 下载 docker-compose 文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -O docker-compose.yml

# 3. 下载配置文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml

# 4. 修改配置
nano application.yml
# 修改 web.account 和 web.password

# 5. 启动服务
docker-compose up -d

# 6. 验证
# IPv4 访问: http://your-ipv4:9527
# IPv6 访问: http://[your-ipv6]:9527
```

---

### 场景 2: 纯 IPv6 服务器 + NAT64/DNS64（推荐用于纯 IPv6）

**适用于**: frog.fm、某些免费 VPS 等仅提供 IPv6 的服务器

#### 方案 A: 使用公共 NAT64/DNS64 服务

许多 IPv6-only 服务器提供商已经配置了 NAT64/DNS64，允许 IPv6 客户端访问 IPv4 服务。

**检查是否已有 NAT64/DNS64**:

```bash
# 测试是否能访问 IPv4-only 网站
ping6 -c 4 google.com
curl -6 http://www.google.com

# 如果成功，说明已有 NAT64/DNS64，可以直接部署！
```

**部署步骤**（与场景 1 相同）:

```bash
mkdir -p /app/king-detective/keys
cd /app/king-detective
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -O docker-compose.yml
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml
nano application.yml  # 修改账号密码
docker-compose up -d
```

#### 方案 B: 手动配置 NAT64/DNS64

如果服务器没有 NAT64/DNS64，可以使用公共服务：

```bash
# 1. 配置 DNS64 解析器（Google 公共 DNS64）
sudo nano /etc/resolv.conf
```

添加：
```
nameserver 2001:4860:4860::6464
nameserver 2001:4860:4860::64
```

```bash
# 2. 验证配置
ping6 -c 4 google.com  # 应该能 ping 通

# 3. 然后正常部署 King-Detective
cd /app/king-detective
docker-compose up -d
```

**常用公共 DNS64 服务器**:
- **Google**: `2001:4860:4860::6464`
- **Cloudflare**: `2606:4700:4700::64`
- **OpenDNS**: `2620:119:35::35`

---

### 场景 3: 通过 Cloudflare Tunnel（任何网络环境）

这个方案**完全绕过网络限制**，适合任何环境！

#### 步骤 1: 安装 Cloudflared

```bash
# 下载 cloudflared
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64
chmod +x cloudflared-linux-amd64
sudo mv cloudflared-linux-amd64 /usr/local/bin/cloudflared

# 登录 Cloudflare
cloudflared tunnel login
```

#### 步骤 2: 创建隧道

```bash
# 创建隧道
cloudflared tunnel create king-detective

# 记下返回的 Tunnel ID
```

#### 步骤 3: 配置隧道

```bash
# 创建配置文件
mkdir -p ~/.cloudflared
nano ~/.cloudflared/config.yml
```

添加以下内容：
```yaml
tunnel: <your-tunnel-id>
credentials-file: /root/.cloudflared/<your-tunnel-id>.json

ingress:
  - hostname: king-detective.yourdomain.com
    service: http://localhost:9527
  - service: http_status:404
```

#### 步骤 4: 配置 DNS 和启动

```bash
# 配置 DNS（自动添加 CNAME 记录）
cloudflared tunnel route dns <tunnel-id> king-detective.yourdomain.com

# 启动隧道（后台运行）
cloudflared tunnel run king-detective &

# 或者设置为系统服务
cloudflared service install
systemctl start cloudflared
systemctl enable cloudflared
```

#### 步骤 5: 部署 King-Detective

```bash
cd /app/king-detective
docker-compose up -d
```

现在可以通过 `https://king-detective.yourdomain.com` 访问（自动 HTTPS）！

---

### 场景 4: 使用 IPv6 到 IPv4 代理

如果其他方案都不行，可以在容器内使用代理。

#### 创建带代理的 docker-compose.yml

```yaml
version: '3.8'

services:
  # Tinyproxy - 提供 HTTP 代理
  proxy:
    image: vimagick/tinyproxy
    container_name: king-detective-proxy
    restart: always
    ports:
      - "8888:8888"
    command: -d

  king-detective:
    image: ghcr.io/tony-wang1990/king-detective:main
    container_name: king-detective
    restart: always
    ports:
      - "[::]:9527:9527"
    volumes:
      - ./application.yml:/app/king-detective/application.yml
      - ./king-detective.db:/app/king-detective/king-detective.db
      - ./keys:/app/king-detective/keys
    environment:
      # 通过代理访问外部 IPv4 API
      - HTTP_PROXY=http://proxy:8888
      - HTTPS_PROXY=http://proxy:8888
      - JAVA_OPTS=-Dhttp.proxyHost=proxy -Dhttp.proxyPort=8888 -Dhttps.proxyHost=proxy -Dhttps.proxyPort=8888
    depends_on:
      - proxy
```

---

##  部署前准备清单

### 1. 确认网络环境

```bash
# 检查 IPv4
curl -4 ifconfig.me

# 检查 IPv6
curl -6 ifconfig.me

# 测试 OCI API 可达性（IPv4）
curl -4 -I https://iaas.us-ashburn-1.oraclecloud.com

# 测试 OCI API 可达性（IPv6 + NAT64）
curl -6 -I https://iaas.us-ashburn-1.oraclecloud.com
```

### 2. 选择合适的方案

| 网络环境 | 推荐方案 | 难度 |
|---------|---------|------|
| IPv4 + IPv6 双栈 | 场景 1 |  简单 |
| 纯 IPv6 + 已有 NAT64/DNS64 | 场景 2A |  简单 |
| 纯 IPv6 + 无 NAT64 | 场景 2B 或 3 |  中等 |
| 任何网络 + 需要域名 | 场景 3 |  推荐 |
| 受限网络环境 | 场景 4 |  复杂 |

### 3. 准备 OCI 配置

在 OCI 控制台准备以下信息：
- 用户 OCID
- 租户 OCID
- 区域（Region）
- API 密钥指纹
- 私钥文件（.pem）

---

##  快速开始（推荐配置）

### 对于双栈或有 NAT64 的服务器

```bash
# 一键部署脚本
curl -fsSL https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/install-ipv6.sh | bash
```

### 手动部署

```bash
# 1. 创建目录
mkdir -p /app/king-detective/keys && cd /app/king-detective

# 2. 下载文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -O docker-compose.yml
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml

# 3. 修改配置
nano application.yml

# 4. 配置 Docker IPv6（如果需要）
sudo nano /etc/docker/daemon.json
```

添加：
```json
{
  "ipv6": true,
  "fixed-cidr-v6": "fd00::/64"
}
```

```bash
sudo systemctl restart docker

# 5. 启动服务
docker-compose up -d

# 6. 查看日志
docker-compose logs -f
```

---

##  故障排查

### 问题: 无法连接到 OCI API

```bash
# 1. 测试 DNS 解析
dig iaas.us-ashburn-1.oraclecloud.com
dig AAAA iaas.us-ashburn-1.oraclecloud.com

# 2. 测试连接
curl -v https://iaas.us-ashburn-1.oraclecloud.com

# 3. 检查 NAT64
ping6 -c 4 www.google.com

# 4. 查看容器日志
docker logs king-detective | grep -i error
```

### 问题: IPv6 端口不监听

```bash
# 检查端口监听
netstat -tlnp | grep 9527
# 应该看到 tcp6

# 检查容器网络
docker inspect king-detective | grep IPv6
```

### 问题: Java 优先使用 IPv4

在 `application.yml` 或环境变量中明确指定：

```yaml
# application.yml
server:
  address: '::'  # 强制 IPv6
```

或在 docker-compose.yml 中：

```yaml
environment:
  - JAVA_OPTS=-Djava.net.preferIPv6Addresses=true
```

---

##  推荐方案总结

1. **如果有双栈网络**: 直接部署，最简单 
2. **如果是纯 IPv6 + frog.fm 等**: 先测试 NAT64，大概率可以直接用   
3. **如果想要最佳体验**: 使用 Cloudflare Tunnel，获得免费 HTTPS 和全球加速 
4. **如果网络受限**: 使用代理方案 

---

##  支持

遇到问题请：
1. 检查日志: `docker logs king-detective`
2. 验证网络: `curl -6 https://iaas.us-ashburn-1.oraclecloud.com`
3. 提交 Issue 到 GitHub

**King-Detective (W探长)** - 让 OCI 管理在任何网络环境下都能轻松运行！
