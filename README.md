# King-Detective

## Docker 部署

```bash
# 创建工作目录
mkdir -p /app/king-detective/keys && cd /app/king-detective

# 下载配置文件
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/docker-compose-ipv6.yml -O docker-compose.yml
wget https://raw.githubusercontent.com/tony-wang1990/King-Detective/main/src/main/resources/application.yml

# 修改配置（可选）
nano application.yml

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

**访问地址**: `http://your-server-ip:9527`

**默认账号**: admin  
**默认密码**: admin123456

---

** 首次登录后请立即修改密码**
